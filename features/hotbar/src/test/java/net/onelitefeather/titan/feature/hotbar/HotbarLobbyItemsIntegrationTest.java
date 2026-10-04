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
package net.onelitefeather.titan.feature.hotbar;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
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
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.ItemUseHandler;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

/**
 * Cyano/Microtus {@code Env} coverage for {@link HotbarLobbyItems}: equipping a real
 * {@link Player},
 * dispatching a used item to its handler, a look-alike item never registered doing nothing, a
 * conflicting item list aborting construction, error attribution, and an unknown key.
 */
@ExtendWith(MicrotusExtension.class)
class HotbarLobbyItemsIntegrationTest {

    private static LobbyItem item(String featureId, String key, Material material, ItemSlot placement, ItemUseHandler onUse) {
        return new LobbyItem(featureId, Key.key(key), ItemStack.of(material), placement, onUse);
    }

    @DisplayName("equip() places exactly the fixed items and clears everything else")
    @Test
    void equipPlacesExactlyTheFixedItems(Env env) {
        EventNode<Event> titan = EventNode.all("test-lobby-items-equip");
        LobbyItem navigator = item("navigator", "titan:navigator", Material.FEATHER, ItemSlot.hotbar(4), (player, event) -> {
        });
        LobbyItem elytra = item("elytra", "titan:elytra", Material.ELYTRA, ItemSlot.equipment(EquipmentSlot.CHESTPLATE), (player, event) -> {
        });
        LobbyItem firework = item("elytra", "titan:firework", Material.FIREWORK_ROCKET, ItemSlot.unplaced(), (player, event) -> {
        });
        HotbarLobbyItems lobbyItems = new HotbarLobbyItems(List.of(navigator, elytra, firework), titan, Telemetry.noop());
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        lobbyItems.equip(player);

        Assertions.assertEquals(lobbyItems.stack("titan:navigator"), player.getInventory().getItemStack(4));
        Assertions.assertEquals(lobbyItems.stack("titan:elytra"), player.getEquipment(EquipmentSlot.CHESTPLATE));
        for (int slot = 0; slot < 9; slot++) {
            if (slot == 4) {
                continue;
            }
            Assertions.assertEquals(ItemStack.AIR, player.getInventory().getItemStack(slot), "hotbar slot " + slot + " must be empty");
        }
        Assertions.assertEquals(ItemStack.AIR, player.getEquipment(EquipmentSlot.HELMET));
        Assertions.assertEquals(ItemStack.AIR, player.getEquipment(EquipmentSlot.OFF_HAND));
    }

    @DisplayName("Using a registered item's stamped stack reaches its handler")
    @Test
    void usingARegisteredItemReachesItsHandler(Env env) {
        EventNode<Event> titan = EventNode.all("test-lobby-items-dispatch");
        List<Player> handledFor = new ArrayList<>();
        LobbyItem navigator = item("navigator", "titan:navigator", Material.FEATHER, ItemSlot.hotbar(4), (player, event) -> handledFor.add(player));
        HotbarLobbyItems lobbyItems = new HotbarLobbyItems(List.of(navigator), titan, Telemetry.noop());
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        ItemStack stamped = lobbyItems.stack("titan:navigator");

        titan.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, stamped, 0L));

        Assertions.assertEquals(List.of(player), handledFor, "the navigator's own handler must run exactly once");
    }

    @DisplayName("A plain feather that was never registered does not reach any handler")
    @Test
    void aPlainFeatherDoesNothing(Env env) {
        EventNode<Event> titan = EventNode.all("test-lobby-items-dispatch-no-tag");
        List<Player> handledFor = new ArrayList<>();
        LobbyItem navigator = item("navigator", "titan:navigator", Material.FEATHER, ItemSlot.hotbar(4), (player, event) -> handledFor.add(player));
        new HotbarLobbyItems(List.of(navigator), titan, Telemetry.noop());
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        titan.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, ItemStack.of(Material.FEATHER), 0L));

        Assertions.assertTrue(handledFor.isEmpty(), "same material, but not the stamped stack - no handler must run");
    }

    @DisplayName("A conflicting item list aborts construction")
    @Test
    void aConflictingItemListAbortsConstruction() {
        EventNode<Event> titan = EventNode.all("test-lobby-items-conflict");
        LobbyItem navigator = item("navigator", "titan:navigator", Material.FEATHER, ItemSlot.hotbar(4), (player, event) -> {
        });
        LobbyItem friends = item("friends", "titan:friends", Material.COMPASS, ItemSlot.hotbar(4), (player, event) -> {
        });

        Assertions.assertThrows(IllegalStateException.class, () -> new HotbarLobbyItems(List.of(navigator, friends), titan, Telemetry.noop()), "two items claiming the same hotbar slot must abort construction");
    }

    @DisplayName("stack(String) throws for a key no item was registered under")
    @Test
    void stackThrowsForAnUnknownKey() {
        EventNode<Event> titan = EventNode.all("test-lobby-items-unknown-key");
        LobbyItem navigator = item("navigator", "titan:navigator", Material.FEATHER, ItemSlot.hotbar(4), (player, event) -> {
        });
        HotbarLobbyItems lobbyItems = new HotbarLobbyItems(List.of(navigator), titan, Telemetry.noop());

        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> lobbyItems.stack("titan:unknown"));

        Assertions.assertTrue(thrown.getMessage().contains("titan:unknown"), "the message must name the unknown key: " + thrown.getMessage());
    }

    @DisplayName("A throwing onUse is attributed to the owning feature, not the item's key")
    @Test
    void aThrowingOnUseIsAttributedToTheOwningFeature(Env env) {
        MinecraftServer.getExceptionManager().setExceptionHandler(FeatureNode::reportUnhandledException);
        EventNode<Event> titan = EventNode.all("test-lobby-items-attribution");
        LobbyItem navigator = item("navigator", "titan:navigator-feather", Material.FEATHER, ItemSlot.hotbar(4), (player, event) -> {
            throw new IllegalStateException("boom for " + player.getUsername());
        });
        HotbarLobbyItems lobbyItems = new HotbarLobbyItems(List.of(navigator), titan, Telemetry.noop());
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        ItemStack stamped = lobbyItems.stack("titan:navigator-feather");
        // ListenerGuard (core, package-private) is where FeatureNode.guard()'s failure is now
        // logged; referenced by name since the class itself is not visible from this package.
        Logger logger = (Logger) LoggerFactory.getLogger("net.onelitefeather.titan.core.module.ListenerGuard");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            Assertions.assertDoesNotThrow(() -> titan.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, stamped, 0L)), "a failing handler must not propagate out of dispatch - the lobby keeps running");
        } finally {
            logger.detachAppender(appender);
        }

        Assertions.assertEquals(1, appender.list.size(), "exactly one failure must be reported");
        String message = appender.list.get(0).getFormattedMessage();
        Assertions.assertTrue(message.contains("navigator"), "the report must name the feature: " + message);
        Assertions.assertFalse(message.contains("titan:navigator-feather"), "the report must not name the item's key: " + message);
    }
}
