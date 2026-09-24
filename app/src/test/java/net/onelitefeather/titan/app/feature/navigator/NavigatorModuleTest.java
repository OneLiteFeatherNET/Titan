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
package net.onelitefeather.titan.app.feature.navigator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntry;
import net.onelitefeather.titan.common.config.ConfigStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end coverage for {@link NavigatorModule} against a real {@code Env}: opening the feather
 * shows the configured entries synchronously (no tick needed), clicking an entry forwards through
 * {@code Deliver} and closes the inventory, an additional entry from configuration appears, and an
 * entry contributed by another module appears and disappears once that module's contribution is
 * withdrawn.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorModuleTest {

    private static EventNode<Event> freshParent(String name) {
        return EventNode.all(name + "-" + UUID.randomUUID());
    }

    @DisplayName("Opening the navigator via the feather shows the four default entries, synchronously")
    @Test
    void openingTheNavigatorShowsTheFourDefaultEntries(Env env) {
        EventNode<Event> parent = freshParent("nav-open");
        NavigatorEntries entries = new NavigatorEntries();
        NavigatorModule module = new NavigatorModule(new RecordingDeliver(), entries);
        NavigatorModuleTestSupport.Started started = NavigatorModuleTestSupport.start(env, parent, entries, module);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        started.items().equip(player);
        ItemStack feather = player.getInventory().getItemStack(4);

        parent.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));

        AbstractInventory openInventory = player.getOpenInventory();
        Assertions.assertNotNull(openInventory, "the navigator must open synchronously, without a tick");
        Assertions.assertInstanceOf(Inventory.class, openInventory);
        Assertions.assertEquals(InventoryType.CHEST_1_ROW, ((Inventory) openInventory).getInventoryType());
        Assertions.assertEquals(Material.ELYTRA, openInventory.getItemStack(0).material());
        Assertions.assertEquals(Material.GRASS_BLOCK, openInventory.getItemStack(4).material());
        Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, openInventory.getItemStack(5).material());
        Assertions.assertEquals(Material.WOODEN_AXE, openInventory.getItemStack(8).material());
        for (int slot : List.of(1, 2, 3, 6, 7)) {
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, openInventory.getItemStack(slot).material(), "slot " + slot + " should be a blank glass pane");
        }
        started.registry().disableAll();
    }

    @DisplayName("Clicking a navigator entry cancels the click, forwards through Deliver and closes the inventory")
    @Test
    void clickingAnEntryForwardsAndCloses(Env env) {
        EventNode<Event> parent = freshParent("nav-click");
        NavigatorEntries entries = new NavigatorEntries();
        RecordingDeliver deliver = new RecordingDeliver();
        NavigatorModule module = new NavigatorModule(deliver, entries);
        NavigatorModuleTestSupport.Started started = NavigatorModuleTestSupport.start(env, parent, entries, module);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        started.items().equip(player);
        ItemStack feather = player.getInventory().getItemStack(4);
        parent.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));
        AbstractInventory openInventory = player.getOpenInventory();
        Assertions.assertNotNull(openInventory);

        InventoryPreClickEvent clickEvent = new InventoryPreClickEvent(openInventory, player, new Click.Left(4));
        parent.call(clickEvent);

        Assertions.assertTrue(clickEvent.isCancelled(), "the click on a navigator entry must be cancelled so the item stays in place");
        Assertions.assertEquals(1, deliver.deliveries().size(), "exactly one delivery must be recorded");
        RecordingDeliver.Delivery delivery = deliver.deliveries().get(0);
        Assertions.assertEquals(player, delivery.player());
        Assertions.assertEquals("Survival", delivery.taskName(), "slot 4 must forward to Survival");
        Assertions.assertNotSame(openInventory, player.getOpenInventory(), "the navigator must be closed after a click");
        started.registry().disableAll();
    }

    @DisplayName("An additional entry from configuration appears in the navigator (Parkour on slot 2)")
    @Test
    void additionalConfiguredEntryAppears(Env env, @TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("app.json");
        String json = """
                {
                  "configVersion": 2,
                  "navigator": {
                    "title": "<yellow>Navigator",
                    "entries": [
                      {"slot": 0, "icon": "minecraft:elytra", "displayName": "<!i><gradient:#fcba03:#03fc8c>ElytraRace</gradient>", "destination": "ElytraRace"},
                      {"slot": 2, "icon": "minecraft:diamond_pickaxe", "displayName": "<green>Parkour", "destination": "Parkour"},
                      {"slot": 4, "icon": "minecraft:grass_block", "displayName": "<!i><green>Survival", "destination": "Survival"},
                      {"slot": 5, "icon": "minecraft:enderman_spawn_egg", "displayName": "<!i><gradient:#616161:#e80000c>Slender</gradient>", "destination": "cygnus"},
                      {"slot": 8, "icon": "minecraft:wooden_axe", "displayName": "<!i><rainbow>Creative</rainbow>", "destination": "MemberBuild"}
                    ]
                  }
                }
                """;
        Files.writeString(configFile, json);
        ConfigStore store = ConfigStore.open(configFile);
        EventNode<Event> parent = freshParent("nav-config-extra");
        NavigatorEntries entries = new NavigatorEntries();
        NavigatorModule module = new NavigatorModule(new RecordingDeliver(), entries);
        NavigatorModuleTestSupport.Started started = NavigatorModuleTestSupport.start(env, parent, entries, store, module);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        started.items().equip(player);
        ItemStack feather = player.getInventory().getItemStack(4);

        parent.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));

        AbstractInventory openInventory = player.getOpenInventory();
        Assertions.assertNotNull(openInventory);
        Assertions.assertEquals(Material.DIAMOND_PICKAXE, openInventory.getItemStack(2).material(), "the configured Parkour entry must appear on slot 2");
        started.registry().disableAll();
    }

    @DisplayName("An entry contributed by another module appears in the navigator and disappears once withdrawn")
    @Test
    void entryFromAnotherModuleAppearsAndDisappears(Env env) {
        EventNode<Event> parent = freshParent("nav-other-module");
        NavigatorEntries entries = new NavigatorEntries();
        NavigatorModule navigatorModule = new NavigatorModule(new RecordingDeliver(), entries);
        LobbyModule teaser = new LobbyModule() {

            @Override
            public String id() {
                return "teaser";
            }

            @Override
            public void enable(ModuleContext context) {
                context.navigator().add(new NavigatorEntry(2, ItemStack.of(Material.ENDER_EYE), Component.text("Voyager"), "Voyager"));
            }
        };
        NavigatorModuleTestSupport.Started started = NavigatorModuleTestSupport.start(env, parent, entries, navigatorModule, teaser);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        started.items().equip(player);
        ItemStack feather = player.getInventory().getItemStack(4);

        parent.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));
        Assertions.assertEquals(Material.ENDER_EYE, player.getOpenInventory().getItemStack(2).material(), "the teaser module's entry must appear on slot 2");

        // Simulates "the teaser module is disabled": ModuleContext#navigator()'s View removes a
        // module's own entries through exactly this call once the owning module is disabled (see
        // NavigatorEntries#forModule). Driven directly here, rather than through a second, partial
        // ModuleRegistry#disableAll(), which would also tear down the navigator module itself.
        entries.removeAll("teaser");
        player.closeInventory();
        parent.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));

        Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, player.getOpenInventory().getItemStack(2).material(), "slot 2 must fall back to blank glass once the teaser's entry is gone");
        started.registry().disableAll();
    }
}
