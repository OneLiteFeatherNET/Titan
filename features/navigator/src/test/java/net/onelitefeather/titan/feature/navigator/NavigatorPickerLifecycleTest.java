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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InventoryEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.testfixtures.EventListenerCounter;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import net.onelitefeather.titan.core.testfixtures.FakePlayerCounts;
import net.onelitefeather.titan.core.testfixtures.RecordingDeliver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Lifecycle coverage for the server lists that a minigame's right click opens: they are built
 * lazily
 * on the first open, so repeated opening must reuse them, and stopping the module must remove every
 * one of them again.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorPickerLifecycleTest {

    private static final String SLENDER_PERMISSION = "titan.navigator.slender";
    private static final int ELYTRA_RACE_SLOT = Destination.ELYTRA_RACE.slot();
    private static final Pos SPAWN = new Pos(11.5, 40, 60);
    private static final ServiceCount RACE_ONE = new ServiceCount("ElytraRace-1", 3, 50);
    private static final int OPEN_CLOSE_COUNT = 20;
    // The back button of a list that fits one content row: the first slot of its two-row navigation row.
    private static final int ONE_ROW_BACK_SLOT = 9;
    private static final int PREVIOUS_SLOT = 48;
    private static final int SORT_SLOT = 49;
    private static final int NEXT_SLOT = 50;

    private static EventNode<Event> navigatorNode(Env env) {
        List<EventNode<Event>> children = env.process().eventHandler().findChildren("titan/navigator");
        Assertions.assertEquals(1, children.size(), "expected exactly one 'titan/navigator' child node");
        return children.get(0);
    }

    private static Inventory openPicker(Env env, NavigatorFixture fixture, Player player) {
        return (Inventory) PickerTestSupport.openPicker(env, fixture, player);
    }

    @DisplayName("Opening and closing the server list 20 times reuses one list and registers no extra listeners")
    @Test
    void repeatedPickerOpeningRegistersNoExtraListeners(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        FakePermissionService permissions = new FakePermissionService().allowEveryone(SLENDER_PERMISSION);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions, counts)) {
            fixture.equip(player);
            Inventory picker = openPicker(env, fixture, player);
            EventNode<Event> navigatorNode = navigatorNode(env);
            EventNode<InventoryEvent> pickerNode = picker.eventNode();
            Assertions.assertTrue(EventListenerCounter.countListeners(pickerNode) > 0, "the server list must listen for clicks before the loop, or the leak check proves nothing");
            int moduleListenersBefore = EventListenerCounter.countListeners(navigatorNode);
            int pickerListenersBefore = EventListenerCounter.countListeners(pickerNode);

            for (int i = 0; i < OPEN_CLOSE_COUNT; i++) {
                player.closeInventory();
                Inventory reopened = openPicker(env, fixture, player);
                Assertions.assertSame(picker, reopened, "the same shared server list must open every time, or the leak check proves nothing");
            }

            Assertions.assertEquals(moduleListenersBefore, EventListenerCounter.countListeners(navigatorNode), "opening the server list must never register another listener on the module's own node");
            Assertions.assertEquals(pickerListenersBefore, EventListenerCounter.countListeners(pickerNode), "opening the server list must never register another listener on the server list's node");
        }
    }

    @DisplayName("Going back to the navigator from the server list 20 times registers no extra listeners")
    @Test
    void repeatedBackRegistersNoExtraListeners(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        FakePermissionService permissions = new FakePermissionService().allowEveryone(SLENDER_PERMISSION);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions, counts)) {
            fixture.equip(player);
            Inventory picker = openPicker(env, fixture, player);
            Inventory navigator = fixture.module().inventory(Set.of(Destination.SLENDER), Locale.ENGLISH);
            EventNode<Event> navigatorNode = navigatorNode(env);
            int moduleListenersBefore = EventListenerCounter.countListeners(navigatorNode);
            int pickerListenersBefore = EventListenerCounter.countListeners(picker.eventNode());
            int navigatorListenersBefore = EventListenerCounter.countListeners(navigator.eventNode());

            for (int i = 0; i < OPEN_CLOSE_COUNT; i++) {
                PickerTestSupport.clickAndTick(env, player.getOpenInventory(), player, new Click.Left(ONE_ROW_BACK_SLOT));
                Assertions.assertSame(navigator, player.getOpenInventory(), "the back button must open the navigator on pass " + i + ", or the leak check proves nothing");
                openPicker(env, fixture, player);
            }

            Assertions.assertEquals(moduleListenersBefore, EventListenerCounter.countListeners(navigatorNode), "going back must never register another listener on the module's own node");
            Assertions.assertEquals(pickerListenersBefore, EventListenerCounter.countListeners(picker.eventNode()), "going back must never register another listener on the server list's node");
            Assertions.assertEquals(navigatorListenersBefore, EventListenerCounter.countListeners(navigator.eventNode()), "opening the navigator again must never register another listener on the navigator's node");
        }
    }

    @DisplayName("Stopping the module unregisters every server list that was opened, in every language")
    @Test
    void stoppingTheModuleUnregistersEveryOpenedPicker(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE);
        Player english = env.createPlayer(env.createFlatInstance(), SPAWN);
        Player german = env.createPlayer(env.createFlatInstance(), SPAWN);
        german.setLocale(Locale.GERMANY);
        FakePermissionService permissions = new FakePermissionService().allowEveryone(SLENDER_PERMISSION);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions, counts)) {
            fixture.equip(english);
            fixture.equip(german);
            Inventory englishPicker = openPicker(env, fixture, english);
            Inventory germanPicker = openPicker(env, fixture, german);
            Assertions.assertTrue(EventListenerCounter.countListeners(englishPicker.eventNode()) > 0, "the English server list must listen for clicks while the module runs");
            Assertions.assertTrue(EventListenerCounter.countListeners(germanPicker.eventNode()) > 0, "the German server list must listen for clicks while the module runs");

            fixture.stopModule();

            Assertions.assertEquals(0, EventListenerCounter.countListeners(englishPicker.eventNode()), "a stopped module must leave no click listener on the English server list");
            Assertions.assertEquals(0, EventListenerCounter.countListeners(germanPicker.eventNode()), "a stopped module must leave no click listener on the German server list");
        }
    }

    @DisplayName("Sorting and paging through every order twice adds no listener after the first round")
    @Test
    void sortingAndPagingRegisterNoListenersAfterTheFirstRound(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", PickerTestSupport.services(60));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        FakePermissionService permissions = new FakePermissionService().allowEveryone(SLENDER_PERMISSION);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions, counts)) {
            Set<Inventory> views = new LinkedHashSet<>();
            views.add(openPicker(env, fixture, player));
            sweepEveryOrderAndPage(env, player, views);
            int afterFirstRound = listenersOf(env, views);

            sweepEveryOrderAndPage(env, player, views);

            Assertions.assertTrue(afterFirstRound > 0, "the views must listen for clicks before the comparison, or the check proves nothing");
            Assertions.assertEquals(afterFirstRound, listenersOf(env, views), "a second round over the same views must register no listener");
        }
    }

    @DisplayName("Stopping the module unregisters every server list view that sorting and paging created")
    @Test
    void stoppingTheModuleUnregistersEverySortedAndPagedView(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", PickerTestSupport.services(60));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        FakePermissionService permissions = new FakePermissionService().allowEveryone(SLENDER_PERMISSION);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions, counts)) {
            Set<Inventory> views = new LinkedHashSet<>();
            views.add(openPicker(env, fixture, player));
            sweepEveryOrderAndPage(env, player, views);
            Assertions.assertTrue(views.size() > 1, "the sweep must open more than one view, or the check proves nothing");

            fixture.stopModule();

            for (Inventory view : views) {
                Assertions.assertEquals(0, EventListenerCounter.countListeners(view.eventNode()), "a stopped module must leave no click listener on any server list view");
            }
        }
    }

    // Every order once with its second page and back, so that each view of the sweep is built.
    private static void sweepEveryOrderAndPage(Env env, Player player, Set<Inventory> views) {
        for (int step = 0; step < PickerSort.values().length; step++) {
            visit(env, player, views, NEXT_SLOT);
            visit(env, player, views, PREVIOUS_SLOT);
            visit(env, player, views, SORT_SLOT);
        }
    }

    private static void visit(Env env, Player player, Set<Inventory> views, int slot) {
        AbstractInventory open = player.getOpenInventory();
        PickerTestSupport.clickAndTick(env, open, player, new Click.Left(slot));
        views.add((Inventory) player.getOpenInventory());
    }

    private static int listenersOf(Env env, Set<Inventory> views) {
        int total = EventListenerCounter.countListeners(navigatorNode(env));
        for (Inventory view : views) {
            total += EventListenerCounter.countListeners(view.eventNode());
        }
        return total;
    }
}
