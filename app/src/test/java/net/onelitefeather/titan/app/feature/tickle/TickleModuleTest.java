/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.app.feature.tickle;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.testing.ModuleHarness;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Env integration coverage for {@link TickleModule}, started through {@link ModuleHarness}: the
 * scenarios a pure {@link TickleCooldownRuleTest} cannot reach because they need a real {@link
 * Player} and {@link Instance} - a feather-holding attack broadcasting the tickle message, an
 * attack without a feather doing nothing, and a second hit within the cooldown doing nothing.
 *
 * <p>Every test uses a fixed {@link Clock} (F.I.R.S.T. - repeatable), so "now" never depends on
 * when the test happens to run.
 */
@ExtendWith(MicrotusExtension.class)
class TickleModuleTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static TickleModule fixedClockModule() {
        return new TickleModule(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @DisplayName("Attacking another player with a feather broadcasts the tickle message and sets the cooldown")
    @Test
    void tickleWithFeatherBroadcastsMessageAndSetsCooldown(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection attackerConnection = env.createConnection();
        Player attacker = attackerConnection.connect(instance);
        TestConnection targetConnection = env.createConnection();
        Player target = targetConnection.connect(instance);
        attacker.setItemInOffHand(ItemStack.of(Material.FEATHER));

        Collector<SystemChatPacket> attackerMessages = attackerConnection.trackIncoming(SystemChatPacket.class);
        Collector<SystemChatPacket> targetMessages = targetConnection.trackIncoming(SystemChatPacket.class);
        Collector<SetCooldownPacket> cooldownPackets = attackerConnection.trackIncoming(SetCooldownPacket.class);

        try (ModuleHarness harness = ModuleHarness.start(env, fixedClockModule())) {
            env.process().eventHandler().call(new EntityAttackEvent(attacker, target));

            attackerMessages.assertSingle();
            targetMessages.assertSingle();
            cooldownPackets.assertSingle();
            Assertions.assertTrue(attacker.hasTag(TickleAttackHandler.COOLDOWN_EXPIRY), "attacker must carry the cooldown tag after tickling");
            Assertions.assertEquals(Long.valueOf(NOW.toEpochMilli() + TickleConfig.DEFAULTS.cooldownMillis()), attacker.getTag(TickleAttackHandler.COOLDOWN_EXPIRY));
        }
    }

    @DisplayName("Attacking another player without a feather does nothing")
    @Test
    void attackWithoutFeatherDoesNothing(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection attackerConnection = env.createConnection();
        Player attacker = attackerConnection.connect(instance);
        TestConnection targetConnection = env.createConnection();
        Player target = targetConnection.connect(instance);

        Collector<SystemChatPacket> attackerMessages = attackerConnection.trackIncoming(SystemChatPacket.class);
        Collector<SystemChatPacket> targetMessages = targetConnection.trackIncoming(SystemChatPacket.class);
        Collector<SetCooldownPacket> cooldownPackets = attackerConnection.trackIncoming(SetCooldownPacket.class);

        try (ModuleHarness harness = ModuleHarness.start(env, fixedClockModule())) {
            env.process().eventHandler().call(new EntityAttackEvent(attacker, target));

            attackerMessages.assertEmpty();
            targetMessages.assertEmpty();
            cooldownPackets.assertEmpty();
            Assertions.assertFalse(attacker.hasTag(TickleAttackHandler.COOLDOWN_EXPIRY), "attacker must not carry a cooldown tag without a feather");
        }
    }

    @DisplayName("A second hit within the cooldown does nothing")
    @Test
    void secondHitWithinCooldownDoesNothing(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection attackerConnection = env.createConnection();
        Player attacker = attackerConnection.connect(instance);
        TestConnection targetConnection = env.createConnection();
        Player target = targetConnection.connect(instance);
        attacker.setItemInOffHand(ItemStack.of(Material.FEATHER));

        Collector<SystemChatPacket> attackerMessages = attackerConnection.trackIncoming(SystemChatPacket.class);
        Collector<SetCooldownPacket> cooldownPackets = attackerConnection.trackIncoming(SetCooldownPacket.class);

        try (ModuleHarness harness = ModuleHarness.start(env, fixedClockModule())) {
            env.process().eventHandler().call(new EntityAttackEvent(attacker, target));
            env.process().eventHandler().call(new EntityAttackEvent(attacker, target));

            Assertions.assertEquals(1, attackerMessages.collect().size(), "a second hit inside the cooldown must not tickle again");
            Assertions.assertEquals(1, cooldownPackets.collect().size(), "a second hit inside the cooldown must not send another cooldown packet");
        }
    }

    @Disabled("Tickle cooldown bug - follow-up change tickle-cooldown")
    @DisplayName("Desired: the first hit after the cooldown expires tickles again, and the cooldown packet carries a tick count")
    @Test
    void firstHitAfterExpiryShouldTickleAgainWithATickBasedCooldownPacket(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection attackerConnection = env.createConnection();
        Player attacker = attackerConnection.connect(instance);
        TestConnection targetConnection = env.createConnection();
        Player target = targetConnection.connect(instance);
        attacker.setItemInOffHand(ItemStack.of(Material.FEATHER));
        AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);

        Collector<SystemChatPacket> attackerMessages = attackerConnection.trackIncoming(SystemChatPacket.class);
        Collector<SetCooldownPacket> cooldownPackets = attackerConnection.trackIncoming(SetCooldownPacket.class);

        try (ModuleHarness harness = ModuleHarness.start(env, new TickleModule(clock))) {
            env.process().eventHandler().call(new EntityAttackEvent(attacker, target));

            // Move past the cooldown and attack again - desired behaviour is a second tickle, not
            // just the cooldown tag silently being cleared.
            clock.advance(Duration.ofMillis(TickleConfig.DEFAULTS.cooldownMillis() + 1));
            env.process().eventHandler().call(new EntityAttackEvent(attacker, target));

            Assertions.assertEquals(2, attackerMessages.collect().size(), "the first hit after expiry must tickle again, not just clear the tag");
            long expectedTicks = TickleConfig.DEFAULTS.cooldownMillis() / 50;
            for (SetCooldownPacket packet : cooldownPackets.collect()) {
                Assertions.assertEquals(expectedTicks, packet.cooldownTicks(), "the cooldown packet must carry a tick count (cooldownMillis / 50), not a millisecond timestamp");
            }
        }
    }
}
