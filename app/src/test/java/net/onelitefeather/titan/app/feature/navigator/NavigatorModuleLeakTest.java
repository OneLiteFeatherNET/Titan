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

import java.util.List;
import java.util.UUID;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.app.testutils.EventListenerCounter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Re-points the idea of the old, {@code @Disabled}
 * {@code net.onelitefeather.titan.app.navigator.NavigatorListenerLeakTest} at {@link
 * NavigatorModule}: repeatedly opening and closing the shared navigator, and many players joining,
 * opening it once and leaving, must never change the number of listeners registered on the
 * module's own event node.
 *
 * <p>{@link NavigatorModule} registers exactly one {@code InventoryPreClickEvent} listener, once,
 * in {@link NavigatorModule#enable}. Unlike the old {@code NavigationHelper} - a Caffeine cache of
 * one Aves {@code PersonalInventoryBuilder} per player, refreshed without ever unregistering the
 * replaced builder's click listener - nothing here registers a listener again after {@code
 * enable()} returns, for any player, on any open. This test proves that structurally: the listener
 * count on the module's own {@code titan/navigator} node, read via {@link EventListenerCounter}
 * (see its own Javadoc on why reflection is needed - Minestom has no public API for this), stays
 * exactly the same no matter how many times the navigator is opened or how many players pass
 * through it.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorModuleLeakTest {

    // design.md suggests 100 players for the "player leaves" scenario; a smaller number would
    // already prove the same structural point, but 100 is fast enough here since nothing beyond
    // Env#createPlayer itself is expensive - no per-player registration happens anymore.
    private static final int PLAYER_COUNT = 100;
    private static final int OPEN_CLOSE_COUNT = 50;

    private static EventNode<Event> navigatorNode(EventNode<Event> parent) {
        List<EventNode<Event>> children = parent.findChildren("titan/navigator");
        Assertions.assertEquals(1, children.size(), "expected exactly one 'titan/navigator' child node");
        return children.get(0);
    }

    @DisplayName("Opening and closing the navigator 50 times registers no extra listeners")
    @Test
    void openingAndClosingRepeatedlyDoesNotLeakListeners(Env env) {
        EventNode<Event> parent = EventNode.all("nav-leak-open-close-" + UUID.randomUUID());
        NavigatorEntries entries = new NavigatorEntries();
        NavigatorModule module = new NavigatorModule(new RecordingDeliver(), entries);
        NavigatorModuleTestSupport.Started started = NavigatorModuleTestSupport.start(env, parent, entries, module);
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        started.items().equip(player);
        ItemStack feather = player.getInventory().getItemStack(4);
        EventNode<Event> navigatorNode = navigatorNode(parent);
        int listenersBefore = EventListenerCounter.countListeners(navigatorNode);

        for (int i = 0; i < OPEN_CLOSE_COUNT; i++) {
            parent.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));
            player.closeInventory();
        }

        Assertions.assertEquals(listenersBefore, EventListenerCounter.countListeners(navigatorNode), "opening and closing the navigator must never register another listener");
        started.registry().disableAll();
    }

    @DisplayName("100 players joining, opening the navigator once and leaving leaves the listener count unchanged")
    @Test
    void manyPlayersJoinOpenAndLeaveWithoutLeakingListeners(Env env) {
        EventNode<Event> parent = EventNode.all("nav-leak-players-" + UUID.randomUUID());
        NavigatorEntries entries = new NavigatorEntries();
        NavigatorModule module = new NavigatorModule(new RecordingDeliver(), entries);
        NavigatorModuleTestSupport.Started started = NavigatorModuleTestSupport.start(env, parent, entries, module);
        Instance instance = env.createFlatInstance();
        EventNode<Event> navigatorNode = navigatorNode(parent);
        int listenersBefore = EventListenerCounter.countListeners(navigatorNode);

        for (int i = 0; i < PLAYER_COUNT; i++) {
            Player player = env.createPlayer(instance);
            started.items().equip(player);
            ItemStack feather = player.getInventory().getItemStack(4);

            parent.call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));
            player.closeInventory();
            parent.call(new PlayerDisconnectEvent(player));
        }

        Assertions.assertEquals(listenersBefore, EventListenerCounter.countListeners(navigatorNode), "100 players opening the navigator and leaving must not change the listener count");
        started.registry().disableAll();
    }
}
