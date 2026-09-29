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
package net.onelitefeather.titan.feature.navigator;

import java.util.List;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.trait.InventoryEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.testfixtures.EventListenerCounter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Leak coverage for {@link NavigatorModule}: repeatedly opening and closing the shared navigator,
 * and many players joining, opening it once and leaving, must never change the listener count on
 * the module's own node or either shared inventory's node.
 *
 * <p>{@link NavigatorModule} registers no listener of its own; {@link NavigatorModule#start()}
 * only attaches an empty {@link net.onelitefeather.titan.core.module.FeatureNode} and registers
 * exactly one click listener via Aves, once. Counts are read via {@link EventListenerCounter}, and
 * teardown always runs through try-with-resources so a failed assertion can never leak into a
 * later test.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorModuleLeakTest {

    // 100 players proves the same structural point a smaller number would, but is fast enough
    // here since nothing beyond Env#createPlayer itself is expensive.
    private static final int PLAYER_COUNT = 100;
    private static final int OPEN_CLOSE_COUNT = 50;

    private static EventNode<Event> navigatorNode(Env env) {
        List<EventNode<Event>> children = env.process().eventHandler().findChildren("titan/navigator");
        Assertions.assertEquals(1, children.size(), "expected exactly one 'titan/navigator' child node");
        return children.get(0);
    }

    private static final String BUILD_PERMISSION = "titan.navigator.buildserver";

    private static FakeFeatureFlags slenderActive() {
        return new FakeFeatureFlags().declare("NAVIGATOR_SLENDER", true);
    }

    @DisplayName("Opening and closing the navigator 50 times, alternating with and without permission, registers no extra listeners")
    @Test
    void openingAndClosingRepeatedlyDoesNotLeakListeners(Env env) {
        Instance instance = env.createFlatInstance();
        Player team = env.createPlayer(instance);
        Player other = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().set(team.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), permissions)) {
            fixture.equip(team);
            fixture.equip(other);
            EventNode<Event> navigatorNode = navigatorNode(env);
            EventNode<InventoryEvent> publicNode = fixture.module().publicInventory().eventNode();
            EventNode<InventoryEvent> teamNode = fixture.module().teamInventory().eventNode();
            int moduleListenersBefore = EventListenerCounter.countListeners(navigatorNode);
            int publicListenersBefore = EventListenerCounter.countListeners(publicNode);
            int teamListenersBefore = EventListenerCounter.countListeners(teamNode);

            for (int i = 0; i < OPEN_CLOSE_COUNT; i++) {
                Player player = i % 2 == 0 ? team : other;
                fixture.useFeather(player);
                player.closeInventory();
            }

            Assertions.assertEquals(moduleListenersBefore, EventListenerCounter.countListeners(navigatorNode), "opening and closing the navigator must never register another listener on the module's own node");
            Assertions.assertEquals(publicListenersBefore, EventListenerCounter.countListeners(publicNode), "opening and closing the navigator must never register another listener on the public inventory's node");
            Assertions.assertEquals(teamListenersBefore, EventListenerCounter.countListeners(teamNode), "opening and closing the navigator must never register another listener on the team inventory's node");
        }
    }

    @DisplayName("100 players joining, opening the navigator once and leaving leaves the listener count unchanged")
    @Test
    void manyPlayersJoinOpenAndLeaveWithoutLeakingListeners(Env env) {
        FakePermissionService permissions = new FakePermissionService();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), permissions)) {
            Instance instance = env.createFlatInstance();
            EventNode<Event> navigatorNode = navigatorNode(env);
            EventNode<InventoryEvent> publicNode = fixture.module().publicInventory().eventNode();
            EventNode<InventoryEvent> teamNode = fixture.module().teamInventory().eventNode();
            int moduleListenersBefore = EventListenerCounter.countListeners(navigatorNode);
            int publicListenersBefore = EventListenerCounter.countListeners(publicNode);
            int teamListenersBefore = EventListenerCounter.countListeners(teamNode);

            for (int i = 0; i < PLAYER_COUNT; i++) {
                Player player = env.createPlayer(instance);
                if (i % 2 == 0) {
                    permissions.set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
                }
                fixture.equip(player);

                fixture.useFeather(player);
                player.closeInventory();
                env.process().eventHandler().call(new PlayerDisconnectEvent(player));
            }

            Assertions.assertEquals(moduleListenersBefore, EventListenerCounter.countListeners(navigatorNode), "100 players opening the navigator and leaving must not change the listener count on the module's own node");
            Assertions.assertEquals(publicListenersBefore, EventListenerCounter.countListeners(publicNode), "100 players opening the navigator and leaving must not change the listener count on the public inventory's node");
            Assertions.assertEquals(teamListenersBefore, EventListenerCounter.countListeners(teamNode), "100 players opening the navigator and leaving must not change the listener count on the team inventory's node");
        }
    }
}
