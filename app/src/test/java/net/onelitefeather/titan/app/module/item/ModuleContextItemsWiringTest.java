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
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.app.module.ModuleRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * End-to-end coverage for the wiring in {@code ModuleContext} and {@code ModuleRegistry}:
 * {@code context.items()} reaches the one platform-wide {@link ItemRegistry} every module shares,
 * and {@code ModuleRegistry.enableAll()} runs {@link ItemRegistry#validate()} once every module has
 * enabled. Everything {@link ItemRegistry} does on its own - dispatch, conflict detection, cleanup,
 * the equip plan - is already covered in {@link ItemRegistryUnitTest} and
 * {@link ItemRegistryIntegrationTest}; this class only checks that the platform actually connects a
 * module to it.
 */
@ExtendWith(MicrotusExtension.class)
class ModuleContextItemsWiringTest {

    private static ModuleRegistry.Builder builder(Env env, EventNode<Event> parent) {
        return ModuleRegistry.builder().parent(parent).scheduler(env.process().scheduler()).commandManager(env.process().command());
    }

    private static LobbyModule moduleRegisteringItem(String id, String key, ItemSlot placement) {
        return new LobbyModule() {

            @Override
            public String id() {
                return id;
            }

            @Override
            public void enable(ModuleContext context) {
                context.items().register(new LobbyItem(Key.key(key), ItemStack.of(Material.FEATHER), placement, (player, event) -> {
                }));
            }
        };
    }

    @DisplayName("enableAll() aborts when two modules both claim hotbar slot 4 through context.items()")
    @Test
    void enableAllAbortsOnAConflictRegisteredThroughContext(Env env) {
        EventNode<Event> parent = EventNode.all("test-context-items-conflict");
        ModuleRegistry registry = builder(env, parent).modules(moduleRegisteringItem("navigator", "titan:navigator", ItemSlot.hotbar(4)), moduleRegisteringItem("friends", "titan:friends", ItemSlot.hotbar(4))).build();

        ItemPlacementConflictException thrown = Assertions.assertThrows(ItemPlacementConflictException.class, registry::enableAll);

        Assertions.assertTrue(thrown.getMessage().contains("navigator"));
        Assertions.assertTrue(thrown.getMessage().contains("friends"));
    }

    @DisplayName("Items registered by different modules through context.items() share one registry and all equip together")
    @Test
    void itemsFromDifferentModulesShareOneRegistryAndEquipTogether(Env env) {
        EventNode<Event> parent = EventNode.all("test-context-items-equip");
        AtomicReference<ModuleContext> capturedContext = new AtomicReference<>();
        LobbyModule navigatorModule = moduleRegisteringItem("navigator", "titan:navigator", ItemSlot.hotbar(4));
        LobbyModule elytraModule = new LobbyModule() {

            @Override
            public String id() {
                return "elytra";
            }

            @Override
            public void enable(ModuleContext context) {
                capturedContext.set(context);
                context.items().register(new LobbyItem(Key.key("titan:elytra"), ItemStack.of(Material.ELYTRA), ItemSlot.equipment(EquipmentSlot.CHESTPLATE), (player, event) -> {
                }));
            }
        };
        ModuleRegistry registry = builder(env, parent).modules(navigatorModule, elytraModule).build();
        registry.enableAll();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        // Any module's own view equips the whole platform loadout, not just what it registered
        // itself - the same instance the spawn and respawn modules call in production.
        capturedContext.get().items().equip(player);

        Assertions.assertEquals(Material.FEATHER, player.getInventory().getItemStack(4).material());
        Assertions.assertEquals(Material.ELYTRA, player.getEquipment(EquipmentSlot.CHESTPLATE).material());
    }

    @DisplayName("A module's items are gone once the module is disabled: no longer equipped, and using the old stack no longer dispatches")
    @Test
    void itemsAreGoneAfterModuleDisabled(Env env) {
        EventNode<Event> parent = EventNode.all("test-context-items-disable");
        List<Player> handledFor = new ArrayList<>();
        AtomicReference<ModuleItems> itemsView = new AtomicReference<>();
        AtomicReference<ItemStack> stampedStack = new AtomicReference<>();
        LobbyModule module = new LobbyModule() {

            @Override
            public String id() {
                return "navigator";
            }

            @Override
            public void enable(ModuleContext context) {
                itemsView.set(context.items());
                stampedStack.set(context.items().register(new LobbyItem(Key.key("titan:navigator"), ItemStack.of(Material.FEATHER), ItemSlot.hotbar(4), (usedBy, event) -> handledFor.add(usedBy))));
            }
        };
        ModuleRegistry registry = builder(env, parent).modules(module).build();
        registry.enableAll();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        itemsView.get().equip(player);
        Assertions.assertEquals(Material.FEATHER, player.getInventory().getItemStack(4).material(), "the item must be equipped while the module is enabled");
        parent.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, stampedStack.get(), 0L));
        Assertions.assertEquals(1, handledFor.size(), "using the item must reach its handler while the module is enabled");

        registry.disableAll();

        itemsView.get().equip(player);
        Assertions.assertEquals(ItemStack.AIR, player.getInventory().getItemStack(4), "the item must no longer be equipped once the module is disabled");
        parent.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, stampedStack.get(), 0L));
        Assertions.assertEquals(1, handledFor.size(), "using the old, still-stamped stack must not reach the handler once the module is disabled");
    }
}
