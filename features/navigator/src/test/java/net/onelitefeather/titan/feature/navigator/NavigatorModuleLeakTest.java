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
import java.util.Locale;
import java.util.Set;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.trait.InventoryEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.testfixtures.EventListenerCounter;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import net.onelitefeather.titan.core.testfixtures.RecordingDeliver;
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
    private static final String BOUNCE_PERMISSION = "titan.navigator.bounce";
    private static final String SLENDER_PERMISSION = "titan.navigator.slender";
    private static final String SKYGRID_PERMISSION = "titan.navigator.skygrid";
    private static final String TAMIAS_PERMISSION = "titan.navigator.tamias";
    private static final String STAGING_PERMISSION = "cygnus.staging.join";

    @DisplayName("Opening and closing the navigator 50 times, alternating with and without permission, registers no extra listeners")
    @Test
    void openingAndClosingRepeatedlyDoesNotLeakListeners(Env env) {
        Instance instance = env.createFlatInstance();
        Player team = env.createPlayer(instance);
        Player other = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().set(team.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED).set(team.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED).set(team.getUuid(), BOUNCE_PERMISSION, PermissionResult.ALLOWED).set(team.getUuid(), STAGING_PERMISSION, PermissionResult.ALLOWED).set(team.getUuid(), SKYGRID_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
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

    @DisplayName("Opening and closing the German navigator 50 times registers no extra listeners")
    @Test
    void germanOpeningAndClosingRepeatedlyDoesNotLeakListeners(Env env) {
        Instance instance = env.createFlatInstance();
        Player team = env.createPlayer(instance);
        Player other = env.createPlayer(instance);
        team.setLocale(Locale.GERMANY);
        other.setLocale(Locale.GERMANY);
        FakePermissionService permissions = new FakePermissionService().set(team.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED).set(team.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED).set(team.getUuid(), BOUNCE_PERMISSION, PermissionResult.ALLOWED).set(team.getUuid(), TAMIAS_PERMISSION, PermissionResult.ALLOWED).set(team.getUuid(), STAGING_PERMISSION, PermissionResult.ALLOWED).set(team.getUuid(), SKYGRID_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(team);
            fixture.equip(other);
            EventNode<Event> navigatorNode = navigatorNode(env);
            EventNode<InventoryEvent> publicNode = fixture.module().publicInventory(Locale.GERMAN).eventNode();
            EventNode<InventoryEvent> teamNode = fixture.module().teamInventory(Locale.GERMAN).eventNode();
            Assertions.assertTrue(EventListenerCounter.countListeners(publicNode) > 0, "the German public inventory must listen for clicks before the loop, or the leak check proves nothing");
            Assertions.assertTrue(EventListenerCounter.countListeners(teamNode) > 0, "the German team inventory must listen for clicks before the loop, or the leak check proves nothing");
            int moduleListenersBefore = EventListenerCounter.countListeners(navigatorNode);
            int publicListenersBefore = EventListenerCounter.countListeners(publicNode);
            int teamListenersBefore = EventListenerCounter.countListeners(teamNode);

            for (int i = 0; i < OPEN_CLOSE_COUNT; i++) {
                boolean withBuild = i % 2 == 0;
                Player player = withBuild ? team : other;
                Inventory expected = withBuild ? fixture.module().teamInventory(Locale.GERMAN) : fixture.module().publicInventory(Locale.GERMAN);
                fixture.useFeather(player);
                Assertions.assertSame(expected, player.getOpenInventory(), "opening the navigator must show the German " + (withBuild ? "team" : "public") + " inventory, or the leak check would pass vacuously");
                player.closeInventory();
            }

            Assertions.assertEquals(moduleListenersBefore, EventListenerCounter.countListeners(navigatorNode), "opening the German navigator must never register another listener on the module's own node");
            Assertions.assertEquals(publicListenersBefore, EventListenerCounter.countListeners(publicNode), "opening the German navigator must never register another listener on the German public inventory's node");
            Assertions.assertEquals(teamListenersBefore, EventListenerCounter.countListeners(teamNode), "opening the German navigator must never register another listener on the German team inventory's node");
        }
    }

    @DisplayName("Every language's inventory listens while the module runs and stops listening once it is stopped")
    @Test
    void everyInventoryIsUnregisteredWhenTheModuleStops(Env env) {
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone("titan.navigator.slender"))) {
            Set<Destination> allStaging = Set.copyOf(Destination.stagingEntries());
            List<Inventory> inventories = List.of(fixture.module().publicInventory(), fixture.module().teamInventory(), fixture.module().publicInventory(Locale.GERMAN), fixture.module().teamInventory(Locale.GERMAN), fixture.module().stagingInventory(allStaging, Locale.ENGLISH), fixture.module().stagingInventory(allStaging, Locale.GERMAN));
            for (Inventory inventory : inventories) {
                Assertions.assertTrue(EventListenerCounter.countListeners(inventory.eventNode()) > 0, "the inventory must listen for clicks while the module runs");
            }

            fixture.stopModule();

            for (Inventory inventory : inventories) {
                Assertions.assertEquals(0, EventListenerCounter.countListeners(inventory.eventNode()), "a stopped module must leave no click listener behind on any inventory");
            }
        }
    }

    @DisplayName("100 players joining, opening the navigator once and leaving leaves the listener count unchanged")
    @Test
    void manyPlayersJoinOpenAndLeaveWithoutLeakingListeners(Env env) {
        FakePermissionService permissions = new FakePermissionService().allowEveryone("titan.navigator.slender");
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
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
