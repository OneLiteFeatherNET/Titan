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

import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
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
 * Fliegen", "Feuerwerk nach dem Landen" and "Boost beim Fliegen". Drives ticks with
 * {@link Env#tick()}; no sleeps.
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

    @DisplayName("Using the stamped firework while flying changes the player's velocity")
    @Test
    void usingTheStampedFireworkWhileFlyingChangesVelocity(Env env) {
        try (ModuleHarness harness = ModuleHarness.start(env, new ElytraModule())) {
            Player player = env.createPlayer(env.createFlatInstance());
            player.setFlyingWithElytra(true);
            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));
            ItemStack stampedFirework = player.getItemInOffHand();
            Vec before = player.getVelocity();

            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.OFF, stampedFirework, 1));

            Assertions.assertNotEquals(before, player.getVelocity(), "using the stamped firework while flying must accelerate the player");

            // The boost keeps nudging velocity for its whole lifetime; driving further ticks must
            // not throw once the player is airborne and boosted.
            Assertions.assertDoesNotThrow(() -> {
                for (int i = 0; i < 5; i++) {
                    env.tick();
                }
            });
        }
    }

    @DisplayName("Using a look-alike firework that was never registered does not boost the player")
    @Test
    void usingAnUnregisteredLookAlikeFireworkDoesNotBoost(Env env) {
        try (ModuleHarness harness = ModuleHarness.start(env, new ElytraModule())) {
            Player player = env.createPlayer(env.createFlatInstance());
            player.setFlyingWithElytra(true);
            Vec before = player.getVelocity();

            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, ItemStack.of(Material.FIREWORK_ROCKET), 1));

            Assertions.assertEquals(before, player.getVelocity(), "a look-alike stack without the registry's identity tag must never reach the elytra module's handler");
        }
    }

    @DisplayName("Stopping flight clears any active boost, so flying again starts a fresh one")
    @Test
    void stoppingFlightClearsAnyActiveBoostSoFlyingAgainStartsAFreshOne(Env env) {
        try (ModuleHarness harness = ModuleHarness.start(env, new ElytraModule())) {
            Player player = env.createPlayer(env.createFlatInstance());
            player.setFlyingWithElytra(true);
            env.process().eventHandler().call(new PlayerStartFlyingWithElytraEvent(player));
            ItemStack stampedFirework = player.getItemInOffHand();
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.OFF, stampedFirework, 1));

            player.setFlyingWithElytra(false);
            env.process().eventHandler().call(new PlayerStopFlyingWithElytraEvent(player));

            // If the module had not cleared the boost on stop-flying, this second use would just
            // extend the still-tracked boost - a no-op that never touches velocity. A properly
            // cleared boost instead starts fresh, whose first tick applies synchronously.
            player.setFlyingWithElytra(true);
            Vec beforeSecondUse = player.getVelocity();
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.OFF, stampedFirework, 1));

            Assertions.assertNotEquals(beforeSecondUse, player.getVelocity(), "stopping flight must clear the previous boost so using the firework again starts a brand-new one");
        }
    }
}
