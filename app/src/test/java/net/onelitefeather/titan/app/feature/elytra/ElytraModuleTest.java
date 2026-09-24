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
package net.onelitefeather.titan.app.feature.elytra;

import java.util.List;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.projectile.FireworkRocketMeta;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.Unit;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.testing.ModuleHarness;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * End-to-end coverage for {@link ElytraModule} through a real {@link ModuleHarness}: the
 * {@code lobby-hotbar} spec scenarios "Standardausstattung" (the elytra half), "Feuerwerk beim
 * Fliegen", "Feuerwerk nach dem Landen" and "Boost beim Fliegen" - the last one now ported from
 * Voyager (see {@link FireworkBoostTracker} and {@link FireworkRockets}): using the firework while
 * flying spawns a real rocket entity the client boosts itself with, instead of the lobby pushing a
 * velocity. Drives ticks with {@link Env#tick()}; no sleeps.
 */
@ExtendWith(MicrotusExtension.class)
class ElytraModuleTest {

    @DisplayName("equip() puts an unbreakable elytra on the chestplate")
    @Test
    void equipPutsAnUnbreakableElytraOnTheChestplate(Env env) {
        try (ModuleHarness harness = ModuleHarness.start(env, new ElytraModule())) {
            Player player = env.createPlayer(env.createFlatInstance());

            harness.items().equip(player);

            ItemStack chestplate = player.getEquipment(EquipmentSlot.CHESTPLATE);
            Assertions.assertEquals(Material.ELYTRA, chestplate.material());
            Assertions.assertEquals(Unit.INSTANCE, chestplate.get(DataComponents.UNBREAKABLE), "the lobby elytra must be unbreakable, as it is today");
        }
    }

    @DisplayName("Starting to fly gives the player the registry-stamped firework in the offhand")
    @Test
    void startingToFlyGivesTheStampedFireworkInTheOffHand(Env env) {
        try (ModuleHarness harness = ModuleHarness.start(env, new ElytraModule())) {
            Player player = env.createPlayer(env.createFlatInstance());

            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));

            ItemStack offHand = player.getItemInOffHand();
            Assertions.assertEquals(Material.FIREWORK_ROCKET, offHand.material());
            Assertions.assertEquals("titan:firework", offHand.getTag(ItemRegistry.IDENTITY_TAG), "the handed-out stack must carry the registry's identity tag so its use reaches this module");
        }
    }

    @DisplayName("Stopping flight empties the offhand again")
    @Test
    void stoppingFlightEmptiesTheOffHandAgain(Env env) {
        try (ModuleHarness harness = ModuleHarness.start(env, new ElytraModule())) {
            Player player = env.createPlayer(env.createFlatInstance());
            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));

            env.process().eventHandler().call(new PlayerStopFlyingWithElytraEvent(player));

            Assertions.assertEquals(ItemStack.AIR, player.getItemInOffHand());
        }
    }

    @DisplayName("Using the stamped firework while flying spawns a rocket entity attached to the player")
    @Test
    void usingTheStampedFireworkWhileFlyingSpawnsARocketAttachedToThePlayer(Env env) {
        try (ModuleHarness harness = ModuleHarness.start(env, new ElytraModule())) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            player.setFlyingWithElytra(true);
            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));
            ItemStack stampedFirework = player.getItemInOffHand();

            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.OFF, stampedFirework, 1));

            Entity rocket = onlyRocketIn(instance);
            FireworkRocketMeta meta = (FireworkRocketMeta) rocket.getEntityMeta();
            Assertions.assertEquals(player.getEntityId(), meta.getShooterEntityId(), "the spawned rocket must be attached to the player who used it, so the client boosts itself");

            // The rocket must be removed again once its burn ends; driving ticks past that point
            // must not throw.
            Assertions.assertDoesNotThrow(() -> {
                for (int i = 0; i < ElytraConfig.DEFAULTS.burnDurationTicks(); i++) {
                    env.tick();
                }
            });
            Assertions.assertTrue(rocket.isRemoved(), "the rocket must be removed once its configured burn duration has passed");
        }
    }

    @DisplayName("Using a look-alike firework that was never registered does not spawn a rocket")
    @Test
    void usingAnUnregisteredLookAlikeFireworkDoesNotSpawnARocket(Env env) {
        try (ModuleHarness harness = ModuleHarness.start(env, new ElytraModule())) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            player.setFlyingWithElytra(true);

            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, ItemStack.of(Material.FIREWORK_ROCKET), 1));

            Assertions.assertTrue(rocketsIn(instance).isEmpty(), "a look-alike stack without the registry's identity tag must never reach the elytra module's handler");
        }
    }

    @DisplayName("A second use during the burn and its cooldown is refused and spawns no second rocket")
    @Test
    void aSecondUseDuringTheBurnAndItsCooldownIsRefused(Env env) {
        try (ModuleHarness harness = ModuleHarness.start(env, new ElytraModule())) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            player.setFlyingWithElytra(true);
            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));
            ItemStack stampedFirework = player.getItemInOffHand();

            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.OFF, stampedFirework, 1));
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.OFF, stampedFirework, 1));

            Assertions.assertEquals(1, rocketsIn(instance).size(), "a second rocket used during an active burn (or its cooldown) must not be lit");
        }
    }

    @DisplayName("Stopping flight clears any active boost, so flying again allows an immediate new one")
    @Test
    void stoppingFlightClearsAnyActiveBoostSoFlyingAgainAllowsAnImmediateNewOne(Env env) {
        try (ModuleHarness harness = ModuleHarness.start(env, new ElytraModule())) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            player.setFlyingWithElytra(true);
            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));
            ItemStack stampedFirework = player.getItemInOffHand();
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.OFF, stampedFirework, 1));

            player.setFlyingWithElytra(false);
            env.process().eventHandler().call(new PlayerStopFlyingWithElytraEvent(player));

            // If the module had not forgotten the boost on stop-flying, this second use would still
            // be refused by the still-running cooldown - a no-op that never spawns a second rocket.
            player.setFlyingWithElytra(true);
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.OFF, stampedFirework, 1));

            Assertions.assertEquals(2, rocketsIn(instance).size(), "stopping flight must clear the previous boost so using the firework again lights a brand-new rocket");
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
