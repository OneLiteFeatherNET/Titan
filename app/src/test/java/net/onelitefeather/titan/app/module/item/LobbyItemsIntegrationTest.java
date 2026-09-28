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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.ArrayList;
import java.util.List;
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
import net.onelitefeather.titan.common.observability.TitanObservability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

/**
 * Cyano/Microtus {@code Env} coverage for {@link LobbyItems}: equipping a real {@link Player},
 * dispatching a used item to its handler, a look-alike item never registered doing nothing, a
 * conflicting item list aborting construction, error attribution, and an unknown key.
 */
@ExtendWith(MicrotusExtension.class)
class LobbyItemsIntegrationTest {

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
        LobbyItems lobbyItems = new LobbyItems(List.of(navigator, elytra, firework), titan);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        lobbyItems.equip(player);

        Assertions.assertEquals(lobbyItems.stack(Key.key("titan:navigator")), player.getInventory().getItemStack(4));
        Assertions.assertEquals(lobbyItems.stack(Key.key("titan:elytra")), player.getEquipment(EquipmentSlot.CHESTPLATE));
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
        LobbyItems lobbyItems = new LobbyItems(List.of(navigator), titan);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        ItemStack stamped = lobbyItems.stack(Key.key("titan:navigator"));

        titan.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, stamped, 0L));

        Assertions.assertEquals(List.of(player), handledFor, "the navigator's own handler must run exactly once");
    }

    @DisplayName("A plain feather that was never registered does not reach any handler")
    @Test
    void aPlainFeatherDoesNothing(Env env) {
        EventNode<Event> titan = EventNode.all("test-lobby-items-dispatch-no-tag");
        List<Player> handledFor = new ArrayList<>();
        LobbyItem navigator = item("navigator", "titan:navigator", Material.FEATHER, ItemSlot.hotbar(4), (player, event) -> handledFor.add(player));
        new LobbyItems(List.of(navigator), titan);
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

        Assertions.assertThrows(IllegalStateException.class, () -> new LobbyItems(List.of(navigator, friends), titan), "two items claiming the same hotbar slot must abort construction");
    }

    @DisplayName("stack(Key) throws for a key no item was registered under")
    @Test
    void stackThrowsForAnUnknownKey() {
        EventNode<Event> titan = EventNode.all("test-lobby-items-unknown-key");
        LobbyItem navigator = item("navigator", "titan:navigator", Material.FEATHER, ItemSlot.hotbar(4), (player, event) -> {
        });
        LobbyItems lobbyItems = new LobbyItems(List.of(navigator), titan);

        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> lobbyItems.stack(Key.key("titan:unknown")));

        Assertions.assertTrue(thrown.getMessage().contains("titan:unknown"), "the message must name the unknown key: " + thrown.getMessage());
    }

    @DisplayName("A throwing onUse is attributed to the owning feature, not the item's key")
    @Test
    void aThrowingOnUseIsAttributedToTheOwningFeature(Env env) {
        TitanObservability.installExceptionHandler();
        EventNode<Event> titan = EventNode.all("test-lobby-items-attribution");
        LobbyItem navigator = item("navigator", "titan:navigator-feather", Material.FEATHER, ItemSlot.hotbar(4), (player, event) -> {
            throw new IllegalStateException("boom for " + player.getUsername());
        });
        LobbyItems lobbyItems = new LobbyItems(List.of(navigator), titan);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        ItemStack stamped = lobbyItems.stack(Key.key("titan:navigator-feather"));
        Logger logger = (Logger) LoggerFactory.getLogger(TitanObservability.class);
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
