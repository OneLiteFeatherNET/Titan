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
package net.onelitefeather.titan.app.module.item;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.minestom.server.event.EventNode;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Covers {@link ItemRegistry} at the unit level: registering and unregistering items, the conflict
 * check {@link ItemRegistry#validate()} runs, and items disappearing once a module's cleanup hook
 * fires. None of this needs a {@link net.minestom.server.entity.Player} - see
 * {@link ItemRegistryIntegrationTest} for dispatch and {@code equip()} against a real one.
 */
@ExtendWith(MicrotusExtension.class)
class ItemRegistryUnitTest {

    private static ItemRegistry newRegistry(Env env, String nodeName) {
        return new ItemRegistry(EventNode.all(nodeName));
    }

    private static LobbyItem item(String key, ItemSlot placement) {
        return new LobbyItem(Key.key(key), ItemStack.of(Material.FEATHER), placement, (player, event) -> {
        });
    }

    @DisplayName("validate() passes when every registered item's placement is unique")
    @Test
    void validatePassesWhenEveryPlacementIsUnique(Env env) {
        ItemRegistry registry = newRegistry(env, "test-validate-ok");
        registry.contextView("navigator", cleanup -> {
        }).register(item("titan:navigator", ItemSlot.hotbar(4)));
        registry.contextView("friends", cleanup -> {
        }).register(item("titan:friends", ItemSlot.hotbar(5)));

        Assertions.assertDoesNotThrow(registry::validate);
    }

    @DisplayName("validate() aborts, naming the slot and both modules, when two modules claim hotbar slot 4")
    @Test
    void validateAbortsWhenTwoModulesClaimTheSameSlot(Env env) {
        ItemRegistry registry = newRegistry(env, "test-validate-conflict");
        registry.contextView("navigator", cleanup -> {
        }).register(item("titan:navigator", ItemSlot.hotbar(4)));
        registry.contextView("friends", cleanup -> {
        }).register(item("titan:friends", ItemSlot.hotbar(4)));

        ItemPlacementConflictException thrown = Assertions.assertThrows(ItemPlacementConflictException.class, registry::validate);

        Assertions.assertTrue(thrown.getMessage().contains("navigator"));
        Assertions.assertTrue(thrown.getMessage().contains("friends"));
        Assertions.assertTrue(thrown.getMessage().contains("4"));
    }

    @DisplayName("validate() aborts, naming the key and both modules, when two modules register the same item key")
    @Test
    void validateAbortsWhenTwoModulesRegisterTheSameKey(Env env) {
        ItemRegistry registry = newRegistry(env, "test-validate-duplicate-key");
        registry.contextView("navigator", cleanup -> {
        }).register(item("titan:shared", ItemSlot.hotbar(0)));
        registry.contextView("friends", cleanup -> {
        }).register(item("titan:shared", ItemSlot.hotbar(1)));

        DuplicateItemKeyException thrown = Assertions.assertThrows(DuplicateItemKeyException.class, registry::validate);

        Assertions.assertTrue(thrown.getMessage().contains("navigator"));
        Assertions.assertTrue(thrown.getMessage().contains("friends"));
        Assertions.assertTrue(thrown.getMessage().contains("titan:shared"));
    }

    @DisplayName("An item disappears from the equip plan once its module's cleanup hook runs")
    @Test
    void anItemDisappearsOnceItsModulesCleanupHookRuns(Env env) {
        ItemRegistry registry = newRegistry(env, "test-cleanup-on-disable");
        List<Runnable> moduleACleanup = new ArrayList<>();
        List<Runnable> moduleBCleanup = new ArrayList<>();
        registry.contextView("a", moduleACleanup::add).register(item("titan:a", ItemSlot.hotbar(0)));
        registry.contextView("b", moduleBCleanup::add).register(item("titan:b", ItemSlot.hotbar(1)));
        Assertions.assertEquals(2, registry.currentPlan().hotbar().size(), "both items are registered before either module disables");

        moduleBCleanup.forEach(Runnable::run);

        EquipPlan plan = registry.currentPlan();
        Assertions.assertEquals(1, plan.hotbar().size(), "module b's item must be gone");
        Assertions.assertTrue(plan.hotbar().containsKey(0), "module a's item must remain");
        Assertions.assertFalse(plan.hotbar().containsKey(1));
    }

    @DisplayName("A module's own view only ever queues cleanup for items it registered itself")
    @Test
    void aModulesCleanupNeverTouchesAnotherModulesItem(Env env) {
        ItemRegistry registry = newRegistry(env, "test-cleanup-isolation");
        List<Runnable> moduleACleanup = new ArrayList<>();
        List<Runnable> moduleBCleanup = new ArrayList<>();
        registry.contextView("a", moduleACleanup::add).register(item("titan:a", ItemSlot.hotbar(0)));
        registry.contextView("b", moduleBCleanup::add).register(item("titan:b", ItemSlot.hotbar(1)));

        moduleACleanup.forEach(Runnable::run);

        EquipPlan plan = registry.currentPlan();
        Assertions.assertFalse(plan.hotbar().containsKey(0), "module a's item must be gone");
        Assertions.assertTrue(plan.hotbar().containsKey(1), "module b's item must be untouched");
    }
}
