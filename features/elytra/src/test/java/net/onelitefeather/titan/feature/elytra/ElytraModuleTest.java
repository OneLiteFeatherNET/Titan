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
package net.onelitefeather.titan.feature.elytra;

import io.avaje.config.Config;
import jakarta.inject.Provider;
import java.time.Clock;
import java.util.List;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.projectile.FireworkRocketMeta;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.Unit;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.LobbyReturnToSpawnEvent;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * End-to-end coverage for {@link ElytraModule}: the firework hand-out while flying and after
 * landing, and the boost while flying. {@link ElytraLobbyItems#elytraChestplate()} is checked
 * directly rather than through a full {@code equip()}, since placing an item by its slot is
 * {@code hotbar}'s responsibility, not this feature's - see {@link ElytraFixture}'s Javadoc for
 * why using the firework also calls its handler directly rather than through the platform's
 * tag-based dispatch.
 *
 * <p>Using the firework while flying spawns a real rocket entity the client boosts itself with,
 * instead of the lobby pushing a velocity - ported from Voyager.
 */
@ExtendWith(MicrotusExtension.class)
class ElytraModuleTest {

    /**
     * Read from the facade rather than hardcoded, so a changed shipped default cannot silently
     * desync this test.
     */
    private static final int DEFAULT_BURN_DURATION_TICKS = Config.getAs(ElytraSettings.BURN_DURATION_TICKS_KEY, Integer::parseInt);
    private static final int DEFAULT_COOLDOWN_TICKS = Config.getAs(ElytraSettings.COOLDOWN_TICKS_KEY, Integer::parseInt);

    @DisplayName("elytraChestplate() is an unbreakable elytra placed on the chestplate slot")
    @Test
    void elytraChestplateIsAnUnbreakableElytraPlacedOnTheChestplateSlot() {
        LobbyItem item = new ElytraLobbyItems().elytraChestplate();

        Assertions.assertEquals(Material.ELYTRA, item.itemStack().material());
        Assertions.assertEquals(Unit.INSTANCE, item.itemStack().get(DataComponents.UNBREAKABLE), "the lobby elytra must be unbreakable, as it is today");
        Assertions.assertEquals(ItemSlot.equipment(EquipmentSlot.CHESTPLATE), item.placement(), "the elytra must be placed on the chestplate slot");
    }

    @DisplayName("A missing LobbyItems bean fails start(), not only the first elytra flight")
    @Test
    void aMissingLobbyItemsBeanFailsStart(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            Provider<LobbyItems> missingLobbyItems = () -> {
                throw new IllegalStateException("no LobbyItems bean in this scope (ElytraModuleTest)");
            };
            ElytraModule module = new ElytraModule(titan.node(), missingLobbyItems, new FireworkBoostTracker(), env.process().scheduler(), Clock.systemUTC(), Telemetry.noop(), new ElytraTelemetry(Telemetry.noop()));

            Assertions.assertThrows(IllegalStateException.class, module::start, "a missing LobbyItems bean must abort start(), just like any other missing feature dependency");
        }
    }

    @DisplayName("Starting to fly gives the player the platform-provided firework in the offhand")
    @Test
    void startingToFlyGivesTheStampedFireworkInTheOffHand(Env env) {
        try (ElytraFixture fixture = ElytraFixture.start(env)) {
            Player player = env.createPlayer(env.createFlatInstance());

            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));

            ItemStack offHand = player.getItemInOffHand();
            Assertions.assertEquals(Material.FIREWORK_ROCKET, offHand.material());
            Assertions.assertEquals(fixture.stampedFireworkStack(), offHand, "the module must hand out exactly the stack LobbyItems.stack(key) returned");
        }
    }

    @DisplayName("Stopping flight empties the offhand again")
    @Test
    void stoppingFlightEmptiesTheOffHandAgain(Env env) {
        try (ElytraFixture fixture = ElytraFixture.start(env)) {
            Player player = env.createPlayer(env.createFlatInstance());
            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));

            env.process().eventHandler().call(new PlayerStopFlyingWithElytraEvent(player));

            Assertions.assertEquals(ItemStack.AIR, player.getItemInOffHand());
        }
    }

    @DisplayName("Returning to spawn while gliding takes the rocket away and forgets the boost")
    @Test
    void returningToSpawnTakesTheRocketAndForgetsTheBoost(Env env) {
        try (ElytraFixture fixture = ElytraFixture.start(env)) {
            Player player = env.createPlayer(env.createFlatInstance());
            player.setFlyingWithElytra(true);
            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));
            fixture.useFirework(player);

            env.process().eventHandler().call(new LobbyReturnToSpawnEvent(player));

            Assertions.assertEquals(ItemStack.AIR, player.getItemInOffHand(), "the rocket must be gone, the stop-flying event does not fire for a server-side end of the glide");
            Assertions.assertEquals(0, fixture.boosts().cooldownTicksRemaining(player.getUuid()), "the boost counter must be forgotten");
        }
    }

    @DisplayName("Returning to spawn without gliding changes nothing and does not fail")
    @Test
    void returningToSpawnWithoutGlidingIsHarmless(Env env) {
        try (ElytraFixture fixture = ElytraFixture.start(env)) {
            Player player = env.createPlayer(env.createFlatInstance());

            Assertions.assertDoesNotThrow(() -> env.process().eventHandler().call(new LobbyReturnToSpawnEvent(player)));

            Assertions.assertEquals(ItemStack.AIR, player.getItemInOffHand(), "the off hand stays empty");
        }
    }

    @DisplayName("Using the firework while flying spawns a rocket entity attached to the player")
    @Test
    void usingTheFireworkWhileFlyingSpawnsARocketAttachedToThePlayer(Env env) {
        try (ElytraFixture fixture = ElytraFixture.start(env)) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            player.setFlyingWithElytra(true);

            fixture.useFirework(player);

            Entity rocket = onlyRocketIn(instance);
            FireworkRocketMeta meta = (FireworkRocketMeta) rocket.getEntityMeta();
            Assertions.assertEquals(player.getEntityId(), meta.getShooterEntityId(), "the spawned rocket must be attached to the player who used it, so the client boosts itself");

            // The rocket must be removed again once its burn ends; driving ticks past that point
            // must not throw.
            Assertions.assertDoesNotThrow(() -> {
                for (int i = 0; i < DEFAULT_BURN_DURATION_TICKS; i++) {
                    env.tick();
                }
            });
            Assertions.assertTrue(rocket.isRemoved(), "the rocket must be removed once its configured burn duration has passed");
        }
    }

    @DisplayName("A second use during the burn and its cooldown is refused and spawns no second rocket")
    @Test
    void aSecondUseDuringTheBurnAndItsCooldownIsRefused(Env env) {
        try (ElytraFixture fixture = ElytraFixture.start(env)) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            player.setFlyingWithElytra(true);

            fixture.useFirework(player);
            fixture.useFirework(player);

            Assertions.assertEquals(1, rocketsIn(instance).size(), "a second rocket used during an active burn (or its cooldown) must not be lit");
        }
    }

    @DisplayName("Stopping flight clears any active boost, so flying again allows an immediate new one")
    @Test
    void stoppingFlightClearsAnyActiveBoostSoFlyingAgainAllowsAnImmediateNewOne(Env env) {
        try (ElytraFixture fixture = ElytraFixture.start(env)) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            player.setFlyingWithElytra(true);
            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));
            fixture.useFirework(player);

            player.setFlyingWithElytra(false);
            env.process().eventHandler().call(new PlayerStopFlyingWithElytraEvent(player));

            // If the boost were not forgotten on stop-flying, this second use would still be
            // refused by the running cooldown - a no-op that never spawns a second rocket.
            player.setFlyingWithElytra(true);
            fixture.useFirework(player);

            Assertions.assertEquals(2, rocketsIn(instance).size(), "stopping flight must clear the previous boost so using the firework again lights a brand-new rocket");
        }
    }

    @DisplayName("Ticking past the burn and its cooldown while still flying allows a second rocket")
    @Test
    void tickingPastTheBurnAndItsCooldownWhileStillFlyingAllowsASecondRocket(Env env) {
        try (ElytraFixture fixture = ElytraFixture.start(env)) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            player.setFlyingWithElytra(true);
            fixture.useFirework(player);

            // Neither landing nor disconnecting clears the boost here - only ticking past the
            // burn and cooldown does, proving ElytraModule drives the tracker every tick.
            int ticksToClearTheCooldown = DEFAULT_BURN_DURATION_TICKS + DEFAULT_COOLDOWN_TICKS;
            for (int i = 0; i < ticksToClearTheCooldown; i++) {
                // Reasserted every tick: Minestom's physics tick lands the player and clears
                // gliding once gravity brings them down, which would end the test early.
                player.setFlyingWithElytra(true);
                env.tick();
            }
            player.setFlyingWithElytra(true);

            // The first rocket's burn has already ended by now, so a refused second use would
            // leave no rocket at all - only a freshly lit one proves the cooldown cleared.
            Assertions.assertTrue(rocketsIn(instance).isEmpty(), "the first rocket's own burn must have ended long before its cooldown does");

            fixture.useFirework(player);

            Assertions.assertEquals(1, rocketsIn(instance).size(), "once the burn and its cooldown have fully ticked away, a second use must light a new rocket");
        }
    }

    @DisplayName("A PlayerDisconnectEvent clears the player's boost state, so a reconnecting player may boost immediately")
    @Test
    void playerDisconnectClearsAnyActiveBoostSoAReconnectingPlayerMayBoostImmediately(Env env) {
        try (ElytraFixture fixture = ElytraFixture.start(env)) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            player.setFlyingWithElytra(true);
            fixture.useFirework(player);

            env.process().eventHandler().call(new PlayerDisconnectEvent(player));

            // If the boost were not forgotten on disconnect, this second use (standing in for a
            // reconnect) would still be refused by the running cooldown, a no-op.
            fixture.useFirework(player);

            Assertions.assertEquals(2, rocketsIn(instance).size(), "a PlayerDisconnectEvent must clear the previous boost so a new use lights a brand-new rocket");
        }
    }

    @DisplayName("The per-tick boost task advances the tracker before stop() and no longer afterwards")
    @Test
    void theBoostTaskAdvancesBeforeStopAndNotAfterwards(Env env) {
        ElytraFixture fixture = ElytraFixture.start(env);
        try {
            Player player = env.createPlayer(env.createFlatInstance());
            player.setFlyingWithElytra(true);
            fixture.useFirework(player);

            env.tick();
            int cooldownAfterOneTick = fixture.boosts().cooldownTicksRemaining(player.getUuid());
            Assertions.assertTrue(cooldownAfterOneTick < DEFAULT_COOLDOWN_TICKS, "the scheduled task must advance the tracker at least once before stop() runs");

            fixture.stopModule();
            env.tick();
            env.tick();

            Assertions.assertEquals(cooldownAfterOneTick, fixture.boosts().cooldownTicksRemaining(player.getUuid()), "no further tick may advance the tracker once the module has stopped");
        } finally {
            fixture.close();
        }
    }

    @DisplayName("Once the module is stopped, starting to fly no longer hands out the firework")
    @Test
    void startingToFlyDoesNothingOnceTheModuleIsStopped(Env env) {
        ElytraFixture fixture = ElytraFixture.start(env);
        try {
            Player player = env.createPlayer(env.createFlatInstance());

            fixture.stopModule();
            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));

            Assertions.assertEquals(ItemStack.AIR, player.getItemInOffHand(), "no feature code may run once the module has stopped");
        } finally {
            fixture.close();
        }
    }

    private static Entity onlyRocketIn(Instance instance) {
        List<Entity> rockets = rocketsIn(instance);
        Assertions.assertEquals(1, rockets.size(), "firework rockets in the instance");
        return rockets.getFirst();
    }

    private static List<Entity> rocketsIn(Instance instance) {
        return instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.FIREWORK_ROCKET).toList();
    }
}
