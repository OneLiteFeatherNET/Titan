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

import static net.onelitefeather.titan.feature.navigator.PickerTestSupport.clickAndTick;
import static net.onelitefeather.titan.feature.navigator.PickerTestSupport.nameOf;
import static net.onelitefeather.titan.feature.navigator.PickerTestSupport.openPicker;
import static net.onelitefeather.titan.feature.navigator.PickerTestSupport.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Paging and sorting in the server list: the clicks that do nothing, the re-read after each change,
 * and that no service is lost across the pages.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorPickerPagingTest {

    private static final String SLENDER_PERMISSION = "titan.navigator.slender";
    private static final Pos SPAWN = new Pos(11.5, 40, 60);
    private static final LobbyPortals NO_PORTALS = List::of;
    private static final int PREVIOUS_SLOT = 45;
    private static final int SORT_SLOT = 49;
    private static final int NEXT_SLOT = 53;
    // The sort item of a list that fits one row: the middle slot of its navigation row.
    private static final int ONE_ROW_SORT_SLOT = 13;
    private static final int SERVICE_SLOTS = 45;
    private static final String SERVICE_PREFIX = "ElytraRace-";

    private static NavigatorFixture start(Env env, FakePlayerCounts counts) {
        return NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), counts, NO_PORTALS);
    }

    /** The names of the services on the open page, in slot order. */
    private static List<String> servicesOnPage(AbstractInventory picker) {
        List<String> names = new ArrayList<>();
        for (int slot = 0; slot < SERVICE_SLOTS; slot++) {
            String name = nameOf(picker, slot);
            if (name.startsWith(SERVICE_PREFIX)) {
                names.add(name);
            }
        }
        return names;
    }

    @DisplayName("A missing arrow does nothing: a click on the previous arrow of the first page keeps the page")
    @Test
    void missingPreviousArrowKeepsThePage(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(100));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, counts)) {
            AbstractInventory picker = openPicker(env, fixture, player);

            clickAndTick(env, picker, player, new Click.Left(PREVIOUS_SLOT));

            Assertions.assertEquals("ElytraRace-000", nameOf(player.getOpenInventory(), 0), "the first page stays the first page");
        }
    }

    @DisplayName("The previous arrow goes back one page")
    @Test
    void previousArrowGoesBackOnePage(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(100));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, counts)) {
            openPicker(env, fixture, player);
            clickAndTick(env, player.getOpenInventory(), player, new Click.Left(NEXT_SLOT));

            clickAndTick(env, player.getOpenInventory(), player, new Click.Left(PREVIOUS_SLOT));

            AbstractInventory picker = player.getOpenInventory();
            Assertions.assertEquals("ElytraRace-000", nameOf(picker, 0), "the previous arrow returns to the first page");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, picker.getItemStack(PREVIOUS_SLOT).material(), "the first page has no arrow back");
        }
    }

    @DisplayName("Sorting from the second page lists the new order on the first page")
    @Test
    void sortingFromTheSecondPageReturnsToTheFirst(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(100));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, counts)) {
            openPicker(env, fixture, player);
            clickAndTick(env, player.getOpenInventory(), player, new Click.Left(NEXT_SLOT));

            clickAndTick(env, player.getOpenInventory(), player, new Click.Left(SORT_SLOT));

            AbstractInventory sorted = player.getOpenInventory();
            Assertions.assertEquals("ElytraRace-000", nameOf(sorted, 0), "the new order starts on the first page");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, sorted.getItemStack(PREVIOUS_SLOT).material(), "the first page has no arrow back");
        }
    }

    @DisplayName("A next arrow that is stale after the list shrank shows the last page that exists")
    @Test
    void nextArrowAfterTheListShrankShowsTheLastPage(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(100));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, counts)) {
            AbstractInventory picker = openPicker(env, fixture, player);
            clickAndTick(env, picker, player, new Click.Left(NEXT_SLOT));
            counts.serveServices("ElytraRace", services(40));

            clickAndTick(env, player.getOpenInventory(), player, new Click.Left(NEXT_SLOT));

            AbstractInventory shown = player.getOpenInventory();
            Assertions.assertEquals(40, servicesOnPage(shown).size(), "the one page that exists holds all 40 services");
            Assertions.assertEquals("ElytraRace-000", nameOf(shown, 0), "the page shown is the first, the last one that exists");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, shown.getItemStack(NEXT_SLOT).material(), "no page follows the one that exists");
        }
    }

    @DisplayName("A service stopped after the list opened does not show on the page turned to afterwards")
    @Test
    void stoppedServiceIsGoneFromTheTurnedPage(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(60));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, counts)) {
            openPicker(env, fixture, player);
            counts.serveServices("ElytraRace", Arrays.stream(services(60)).filter(service -> !service.name().equals("ElytraRace-050")).toArray(ServiceCount[]::new));

            clickAndTick(env, player.getOpenInventory(), player, new Click.Left(NEXT_SLOT));

            List<String> page = servicesOnPage(player.getOpenInventory());
            Assertions.assertFalse(page.contains("ElytraRace-050"), "the stopped service must not be listed again");
            Assertions.assertEquals(14, page.size(), "the second page holds the 14 services that remain");
        }
    }

    @DisplayName("A second click on the sort item before the first one is read is ignored")
    @Test
    void doubleClickOnTheSortItemStepsOnce(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(3));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, counts)) {
            AbstractInventory picker = openPicker(env, fixture, player);
            int readsBefore = counts.reads();

            env.process().eventHandler().call(new InventoryPreClickEvent(picker, player, new Click.Left(ONE_ROW_SORT_SLOT)));
            env.process().eventHandler().call(new InventoryPreClickEvent(picker, player, new Click.Left(ONE_ROW_SORT_SLOT)));
            env.tick();

            Assertions.assertEquals(readsBefore + 1, counts.reads(), "only the first click reads the services");
            Assertions.assertEquals("▸ Most players", PickerTestSupport.loreOf(player.getOpenInventory(), ONE_ROW_SORT_SLOT).get(1), "the order moved one step, not two");
        }
    }

    @DisplayName("The list keeps the services it opened with until the next click")
    @Test
    void listKeepsItsReadingUntilTheNextClick(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(2));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, counts)) {
            AbstractInventory picker = openPicker(env, fixture, player);
            counts.serveServices("ElytraRace", services(1));

            NavigatorFixture.tick(env, NavigatorFixture.REFRESH_SECONDS * ServerFlag.SERVER_TICKS_PER_SECOND * 2);

            Assertions.assertEquals("ElytraRace-001", nameOf(picker, 4), "a service that stopped stays listed until a click reads again: two entries are centred in columns three and four");
        }
    }

    @DisplayName("Middle and number-key clicks on the sort item and the arrows change neither order nor page")
    @Test
    void unnamedClicksOnTheNavigationChangeNothing(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(100));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, counts)) {
            AbstractInventory picker = openPicker(env, fixture, player);
            clickAndTick(env, picker, player, new Click.Left(NEXT_SLOT));
            AbstractInventory page = player.getOpenInventory();

            clickAndTick(env, page, player, new Click.Middle(SORT_SLOT));
            clickAndTick(env, page, player, new Click.HotbarSwap(0, SORT_SLOT));
            clickAndTick(env, page, player, new Click.Middle(PREVIOUS_SLOT));
            clickAndTick(env, page, player, new Click.HotbarSwap(0, NEXT_SLOT));

            Assertions.assertSame(page, player.getOpenInventory(), "the server list stays open");
            Assertions.assertEquals("ElytraRace-045", nameOf(page, 0), "the page is unchanged");
            Assertions.assertEquals("▸ Name (A–Z)", PickerTestSupport.loreOf(page, SORT_SLOT).get(0), "the order is unchanged");
        }
    }

    @DisplayName("Every service of two hundred appears on exactly one of the five pages")
    @Test
    void everyServiceAppearsExactlyOnce(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(200));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, counts)) {
            AbstractInventory picker = openPicker(env, fixture, player);
            List<String> listed = new ArrayList<>(servicesOnPage(picker));
            for (int page = 1; page < 5; page++) {
                clickAndTick(env, player.getOpenInventory(), player, new Click.Left(NEXT_SLOT));
                listed.addAll(servicesOnPage(player.getOpenInventory()));
            }

            List<String> expected = IntStream.range(0, 200).mapToObj(i -> "ElytraRace-%03d".formatted(i)).toList();
            Assertions.assertEquals(expected, listed, "all 200 services, once each, in name order across the pages");
        }
    }
}
