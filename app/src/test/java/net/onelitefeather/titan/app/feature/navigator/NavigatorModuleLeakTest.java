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
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.InventoryEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.testing.ModuleHarness;
import net.onelitefeather.titan.app.testutils.EventListenerCounter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Re-points the idea of the old, {@code @Disabled}
 * {@code net.onelitefeather.titan.app.navigator.NavigatorListenerLeakTest} at {@link
 * NavigatorModule}: repeatedly opening and closing the shared navigator, and many players joining,
 * opening it once and leaving, must never change the number of listeners registered - neither on
 * the module's own event node, nor on the event node Aves registered its click listener on.
 *
 * <p>{@link NavigatorModule} registers no listener of its own at all: {@link NavigatorInventory}
 * calls Aves' {@code GlobalInventoryBuilder#register()} exactly once, in
 * {@link NavigatorModule#enable}, which registers exactly one click listener on the built
 * inventory's own event node - see {@link NavigatorModule#sharedInventory()}. Nothing here
 * registers a listener again after {@code enable()} returns, for any player, on any open. This
 * test proves that structurally: the listener counts on both the module's own {@code
 * titan/navigator} node and the shared inventory's own node, read via {@link EventListenerCounter}
 * (see its own Javadoc on why reflection is needed - Minestom has no public API for this), stay
 * exactly the same no matter how many times the navigator is opened or how many players pass
 * through it.
 *
 * <p>Started through {@link ModuleHarness}'s {@link ModuleHarness.ModuleFactory} overload, which
 * hands the harness's own navigator entries to {@link NavigatorModule}'s constructor before the
 * registry starts. Teardown always runs through try-with-resources, so a failed assertion can never
 * leak the harness's listeners into a later test.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorModuleLeakTest {

    // design.md suggests 100 players for the "player leaves" scenario; a smaller number would
    // already prove the same structural point, but 100 is fast enough here since nothing beyond
    // Env#createPlayer itself is expensive - no per-player registration happens anymore.
    private static final int PLAYER_COUNT = 100;
    private static final int OPEN_CLOSE_COUNT = 50;

    private static EventNode<Event> navigatorNode(Env env) {
        List<EventNode<Event>> children = env.process().eventHandler().findChildren("titan/navigator");
        Assertions.assertEquals(1, children.size(), "expected exactly one 'titan/navigator' child node");
        return children.get(0);
    }

    @DisplayName("Opening and closing the navigator 50 times registers no extra listeners")
    @Test
    void openingAndClosingRepeatedlyDoesNotLeakListeners(Env env) {
        NavigatorModule[] moduleHolder = new NavigatorModule[1];
        try (ModuleHarness harness = ModuleHarness.start(env, (navigator, items) -> {
            moduleHolder[0] = new NavigatorModule(new RecordingDeliver(), navigator);
            return new LobbyModule[]{moduleHolder[0]};
        })) {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            harness.items().equip(player);
            ItemStack feather = player.getInventory().getItemStack(4);
            EventNode<Event> navigatorNode = navigatorNode(env);
            EventNode<InventoryEvent> avesInventoryNode = moduleHolder[0].sharedInventory().eventNode();
            int moduleListenersBefore = EventListenerCounter.countListeners(navigatorNode);
            int avesListenersBefore = EventListenerCounter.countListeners(avesInventoryNode);

            for (int i = 0; i < OPEN_CLOSE_COUNT; i++) {
                env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));
                player.closeInventory();
            }

            Assertions.assertEquals(moduleListenersBefore, EventListenerCounter.countListeners(navigatorNode), "opening and closing the navigator must never register another listener on the module's own node");
            Assertions.assertEquals(avesListenersBefore, EventListenerCounter.countListeners(avesInventoryNode), "opening and closing the navigator must never register another listener on the shared inventory's node");
        }
    }

    @DisplayName("100 players joining, opening the navigator once and leaving leaves the listener count unchanged")
    @Test
    void manyPlayersJoinOpenAndLeaveWithoutLeakingListeners(Env env) {
        NavigatorModule[] moduleHolder = new NavigatorModule[1];
        try (ModuleHarness harness = ModuleHarness.start(env, (navigator, items) -> {
            moduleHolder[0] = new NavigatorModule(new RecordingDeliver(), navigator);
            return new LobbyModule[]{moduleHolder[0]};
        })) {
            Instance instance = env.createFlatInstance();
            EventNode<Event> navigatorNode = navigatorNode(env);
            EventNode<InventoryEvent> avesInventoryNode = moduleHolder[0].sharedInventory().eventNode();
            int moduleListenersBefore = EventListenerCounter.countListeners(navigatorNode);
            int avesListenersBefore = EventListenerCounter.countListeners(avesInventoryNode);

            for (int i = 0; i < PLAYER_COUNT; i++) {
                Player player = env.createPlayer(instance);
                harness.items().equip(player);
                ItemStack feather = player.getInventory().getItemStack(4);

                env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, feather, 0L));
                player.closeInventory();
                env.process().eventHandler().call(new PlayerDisconnectEvent(player));
            }

            Assertions.assertEquals(moduleListenersBefore, EventListenerCounter.countListeners(navigatorNode), "100 players opening the navigator and leaving must not change the listener count on the module's own node");
            Assertions.assertEquals(avesListenersBefore, EventListenerCounter.countListeners(avesInventoryNode), "100 players opening the navigator and leaving must not change the listener count on the shared inventory's node");
        }
    }
}
