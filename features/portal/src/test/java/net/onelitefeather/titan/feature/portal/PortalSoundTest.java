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
package net.onelitefeather.titan.feature.portal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntitySoundEffectPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.testfixtures.AdjustableClock;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import net.onelitefeather.titan.core.testfixtures.RecordingDeliver;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The portal sounds as heard on the wire. Every test builds its own {@code Env}, module, deliver
 * and clock (F.I.R.S.T.); moves are driven explicitly, with no waiting.
 */
@ExtendWith(MicrotusExtension.class)
class PortalSoundTest {

    private static final Pos OUTSIDE = new Pos(10.5, 64, 0.5);
    private static final Pos INSIDE = new Pos(0.5, 64, 0.5);
    private static final Pos BYSTANDER = new Pos(12.5, 64, 4.5);
    private static final Pos OPEN_BOX = new Pos(20.5, 64, 0.5);

    private final AdjustableClock clock = new AdjustableClock(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
    private final RecordingDeliver deliver = new RecordingDeliver();
    private final FakePermissionService permissions = new FakePermissionService();
    private final TestTelemetry testTelemetry = TestTelemetry.create();

    private static Portal survivalBox() {
        return new Portal("survival", new Box(new Vec(0, 64, 0), new Vec(1, 65, 1)), "Survival", null);
    }

    private static Portal gate() {
        return new Portal("gate", new Disc(new Vec(0, 64, 0), 3, new Vec(1, 0, 0)), "Gate", null);
    }

    private PortalModule start(Env env, TestTitanNode titan, Portal... portals) {
        PortalModule module = new PortalModule(titan.node(), () -> List.of(portals), this.deliver, this.permissions, this.clock, env.createFlatInstance(), env.process().scheduler(), Runnable::run, portal -> new LabelReading.Local(0), new PortalSettings(1), Optional.empty(), this.testTelemetry.telemetry());
        module.start();
        return module;
    }

    /** Fires the move event before the position changes, exactly as Minestom does. */
    private static void move(Env env, Player player, Pos to) {
        env.process().eventHandler().call(new PlayerMoveEvent(player, to, true));
        player.teleport(to).join();
        env.tick();
    }

    private static List<SoundEvent> soundsHeard(Collector<EntitySoundEffectPacket> collector) {
        return collector.collect().stream().map(EntitySoundEffectPacket::soundEvent).toList();
    }

    @DisplayName("Passing a box plays exactly one teleport sound for the passing player")
    @Test
    void boxPassPlaysOneTeleportSoundForPasser(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = start(env, titan, survivalBox());
            Instance instance = env.createFlatInstance();
            TestConnection passerConnection = env.createConnection();
            Player passer = passerConnection.connect(instance, OUTSIDE);
            Collector<EntitySoundEffectPacket> heard = passerConnection.trackIncoming(EntitySoundEffectPacket.class);

            move(env, passer, INSIDE);

            Assertions.assertEquals(List.of(PortalSounds.BOX_PASS), soundsHeard(heard), "one box pass must play exactly one teleport sound");
            module.stop();
        }
    }

    @DisplayName("A bystander hears no portal sound when someone passes a box")
    @Test
    void boxPassSoundIsNotHeardByBystander(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = start(env, titan, survivalBox());
            Instance instance = env.createFlatInstance();
            Player passer = env.createConnection().connect(instance, OUTSIDE);
            TestConnection bystanderConnection = env.createConnection();
            bystanderConnection.connect(instance, BYSTANDER);
            Collector<EntitySoundEffectPacket> heard = bystanderConnection.trackIncoming(EntitySoundEffectPacket.class);

            move(env, passer, INSIDE);

            Assertions.assertTrue(soundsHeard(heard).isEmpty(), "the pass sound belongs to the passing player alone");
            module.stop();
        }
    }

    @DisplayName("Passing a ring plays only the ring sound, not the teleport sound")
    @Test
    void ringPassPlaysOnlyTheRingSound(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = start(env, titan, gate());
            Instance instance = env.createFlatInstance();
            TestConnection passerConnection = env.createConnection();
            Player passer = passerConnection.connect(instance, new Pos(-4, 64, 0));
            Collector<EntitySoundEffectPacket> heard = passerConnection.trackIncoming(EntitySoundEffectPacket.class);

            move(env, passer, new Pos(4, 64, 0));

            Assertions.assertEquals(List.of(PortalSounds.RING_PASS), soundsHeard(heard), "one ring pass must play exactly one ring sound");
            module.stop();
        }
    }

    @DisplayName("Entering a portal without the permission plays the refusal sound once")
    @Test
    void refusedEntryPlaysRefusalSoundOnce(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Portal vip = new Portal("vip", new Box(new Vec(0, 64, 0), new Vec(1, 65, 1)), "Vip", "titan.portal.vip");
            PortalModule module = start(env, titan, vip);
            Instance instance = env.createFlatInstance();
            TestConnection passerConnection = env.createConnection();
            Player passer = passerConnection.connect(instance, OUTSIDE);
            this.permissions.set(passer.getUuid(), "titan.portal.vip", PermissionResult.DENIED);
            Collector<EntitySoundEffectPacket> heard = passerConnection.trackIncoming(EntitySoundEffectPacket.class);

            move(env, passer, INSIDE);
            move(env, passer, new Pos(1.2, 64, 0.5));

            Assertions.assertEquals(List.of(PortalSounds.REFUSED), soundsHeard(heard), "a refused player hears the refusal once, not again while standing in the box");
            module.stop();
        }
    }

    @DisplayName("A refused entry within the cooldown plays no refusal sound")
    @Test
    void refusedEntryDuringCooldownPlaysNothing(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Portal vip = new Portal("vip", new Box(new Vec(0, 64, 0), new Vec(1, 65, 1)), "Vip", "titan.portal.vip");
            Portal open = new Portal("survival", new Box(new Vec(20, 64, 0), new Vec(21, 65, 1)), "Survival", null);
            PortalModule module = start(env, titan, open, vip);
            Instance instance = env.createFlatInstance();
            TestConnection passerConnection = env.createConnection();
            Player passer = passerConnection.connect(instance, OUTSIDE);
            this.permissions.set(passer.getUuid(), "titan.portal.vip", PermissionResult.DENIED);
            move(env, passer, OPEN_BOX);
            move(env, passer, OUTSIDE);
            Collector<EntitySoundEffectPacket> heard = passerConnection.trackIncoming(EntitySoundEffectPacket.class);

            move(env, passer, INSIDE);

            Assertions.assertTrue(soundsHeard(heard).stream().noneMatch(PortalSounds.REFUSED::equals), "a refusal inside the cooldown is not reported, so it must not sound");
            module.stop();
        }
    }
}
