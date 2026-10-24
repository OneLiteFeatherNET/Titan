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
package net.onelitefeather.titan.feature.settings;

import java.util.Optional;
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
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The settings item: it sits in hotbar slot 7 and opens the menu when used, and it does not exist
 * while the feature is off. The hotbar's own equip logic is covered by the hotbar column.
 */
@ExtendWith(MicrotusExtension.class)
class SettingsHotbarIntegrationTest {

    @DisplayName("The settings item is placed in hotbar slot 7 while the feature is on")
    @Test
    void itemIsPlacedInSlotSeven(Env env) {
        LobbyItem item = settingsItem(env, SettingsFixtures.ENABLED).orElseThrow();
        Assertions.assertEquals(ItemSlot.hotbar(7), item.placement(), "the settings item must take hotbar slot 7");
    }

    @DisplayName("Using the settings item opens the settings menu for the player")
    @Test
    void usingTheItemOpensTheMenu(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createConnection().connect(instance);
        LobbyItem item = settingsItem(env, SettingsFixtures.ENABLED).orElseThrow();
        item.onUse().handle(player, new PlayerUseItemEvent(player, PlayerHand.MAIN, ItemStack.of(Material.COMPARATOR), 0L));
        Assertions.assertNotNull(player.getOpenInventory(), "using the item must open the settings menu");
    }

    @DisplayName("With the feature off there is no settings item at all")
    @Test
    void noItemWhileTheFeatureIsOff(Env env) {
        Assertions.assertTrue(settingsItem(env, SettingsFixtures.DISABLED).isEmpty(), "a disabled feature must contribute no hotbar item");
    }

    private static Optional<LobbyItem> settingsItem(Env env, SettingsOptions options) {
        EventNode<Event> node = EventNode.all("settings-item-test");
        SettingsModule module = SettingsFixtures.module(env, node, options, new FakeSettingsStore(), SettingsFixtures.registry(SettingsFixtures.TAG), SettingsFixtures.SAME_THREAD);
        return new SettingsItems().settingsItem(module, options, new SettingsMessages());
    }
}
