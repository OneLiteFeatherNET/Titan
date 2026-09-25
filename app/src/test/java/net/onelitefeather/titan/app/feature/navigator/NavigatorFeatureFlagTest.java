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

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntry;
import net.onelitefeather.titan.app.module.testing.ModuleHarness;
import net.onelitefeather.titan.common.config.ConfigException;
import net.onelitefeather.titan.common.config.ConfigSections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * End-to-end coverage for the {@code lobby-navigator} spec requirement "Navigator-Ziele können
 * hinter einer Feature-Flag liegen" (design.md decision 13): Slender hidden while
 * {@code NAVIGATOR_SLENDER} is off, shown and forwarding while it is on, becoming visible on the
 * very next open once the flag flips at runtime with no restart, and an unknown feature name in a
 * navigator-sourced entry aborting startup by naming {@code navigator.entries}.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorFeatureFlagTest {

    @DisplayName("Slender's flag off: slot 5 is a blank glass pane, the other entries are unchanged")
    @Test
    void slenderHiddenWhenFlagIsOff(Env env) {
        FakeFeatureFlags flags = new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", false);
        try (ModuleHarness harness = ModuleHarness.start(env, (navigator, items) -> new LobbyModule[]{new NavigatorModule(new RecordingDeliver(), navigator, flags)})) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            harness.items().equip(player);
            ItemStack feather = player.getInventory().getItemStack(4);

            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));

            AbstractInventory openInventory = player.getOpenInventory();
            Assertions.assertNotNull(openInventory);
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, openInventory.getItemStack(5).material(), "slot 5 must be blank while NAVIGATOR_SLENDER is off");
            Assertions.assertEquals(Material.ELYTRA, openInventory.getItemStack(0).material(), "unrelated entries must be unaffected");
            Assertions.assertEquals(Material.GRASS_BLOCK, openInventory.getItemStack(4).material(), "unrelated entries must be unaffected");
            Assertions.assertEquals(Material.WOODEN_AXE, openInventory.getItemStack(8).material(), "unrelated entries must be unaffected");
        }
    }

    @DisplayName("Slender's flag on: slot 5 shows Slender, and a click on it forwards to cygnus")
    @Test
    void slenderShownAndForwardsWhenFlagIsOn(Env env) {
        FakeFeatureFlags flags = new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", true);
        RecordingDeliver deliver = new RecordingDeliver();
        try (ModuleHarness harness = ModuleHarness.start(env, (navigator, items) -> new LobbyModule[]{new NavigatorModule(deliver, navigator, flags)})) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            harness.items().equip(player);
            ItemStack feather = player.getInventory().getItemStack(4);
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));
            AbstractInventory openInventory = player.getOpenInventory();
            Assertions.assertNotNull(openInventory);
            Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, openInventory.getItemStack(5).material(), "slot 5 must show Slender while NAVIGATOR_SLENDER is on");

            InventoryPreClickEvent clickEvent = new InventoryPreClickEvent(openInventory, player, new Click.Left(5));
            env.process().eventHandler().call(clickEvent);

            Assertions.assertTrue(clickEvent.isCancelled());
            Assertions.assertEquals(1, deliver.deliveries().size());
            Assertions.assertEquals("cygnus", deliver.deliveries().get(0).taskName(), "clicking Slender must forward to cygnus");
        }
    }

    @DisplayName("Toggling the flag on between two opens shows Slender on the second open, without a restart")
    @Test
    void togglingTheFlagBetweenTwoOpensShowsSlenderOnTheSecondOpen(Env env) {
        FakeFeatureFlags flags = new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", false);
        try (ModuleHarness harness = ModuleHarness.start(env, (navigator, items) -> new LobbyModule[]{new NavigatorModule(new RecordingDeliver(), navigator, flags)})) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            harness.items().equip(player);
            ItemStack feather = player.getInventory().getItemStack(4);

            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, player.getOpenInventory().getItemStack(5).material(), "slot 5 must be blank on the first open, flag off");
            player.closeInventory();

            flags.set("NAVIGATOR_SLENDER", true);
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));

            Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, player.getOpenInventory().getItemStack(5).material(), "slot 5 must show Slender on the second open, after the flag flipped, with no restart");
        }
    }

    @DisplayName("An unknown feature name in a navigator-sourced entry aborts enableAll(), naming navigator.entries and the unknown flag")
    @Test
    void unknownFeatureInConfigurationAbortsEnableAll(Env env) {
        // A stand-in for an unknown feature name in navigator.entries, in place of a ConfigSections
        // override no longer read by NavigatorModule (design.md, decision 5). Contributed under the
        // "navigator" module id, the same id a config-sourced entry is always attributed to.
        NavigatorEntry slender = new NavigatorEntry(5, ItemStack.of(Material.ENDERMAN_SPAWN_EGG), Component.text("Slender"), "cygnus", "GIBT_ES_NICHT");
        LobbyModule navigatorEntrySource = new LobbyModule() {

            @Override
            public String id() {
                return "navigator";
            }

            @Override
            public void enable(ModuleContext context) {
                context.navigator().add(slender);
            }
        };
        FakeFeatureFlags flags = new FakeFeatureFlags();

        // FeatureFlags wired into the harness/registry itself, for
        // ModuleRegistry#enableAll()'s NavigatorEntries#validate(FeatureFlags) check - exactly like
        // Titan wires the real TogglzFeatureFlags in.
        ConfigException thrown = Assertions.assertThrows(ConfigException.class, () -> ModuleHarness.start(env, (ConfigSections) null, flags, (navigator, items) -> new LobbyModule[]{navigatorEntrySource}));

        Assertions.assertEquals("navigator", thrown.section(), "a config-sourced entry's origin module is 'navigator'");
        Assertions.assertEquals("entries", thrown.field());
        Assertions.assertTrue(thrown.reason().contains("GIBT_ES_NICHT"), "the message must name the unknown flag");
    }
}
