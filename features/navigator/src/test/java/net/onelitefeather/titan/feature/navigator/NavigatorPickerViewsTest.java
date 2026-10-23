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
import static net.onelitefeather.titan.feature.navigator.PickerTestSupport.loreOf;
import static net.onelitefeather.titan.feature.navigator.PickerTestSupport.nameOf;
import static net.onelitefeather.titan.feature.navigator.PickerTestSupport.openPicker;
import static net.onelitefeather.titan.feature.navigator.PickerTestSupport.services;

import java.util.Locale;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The server list as a player sees it: its pages, the navigation row under them, and the order a
 * player chose staying with that player's own open list.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorPickerViewsTest {

    private static final String SLENDER_PERMISSION = "titan.navigator.slender";
    private static final Pos SPAWN = new Pos(11.5, 40, 60);
    // The sort item of a list that fits one row: the middle slot of its navigation row.
    private static final int ONE_ROW_BACK_SLOT = 9;
    private static final int ONE_ROW_SORT_SLOT = 13;
    private static final int ONE_ROW_PREVIOUS_SLOT = 12;
    private static final int ONE_ROW_NEXT_SLOT = 14;
    private static final int BACK_SLOT = 45;
    private static final int PREVIOUS_SLOT = 48;
    private static final int SORT_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final String BACK_NAME_DE = "Zurück zum Navigator";
    // The sort item's lore: four orders, a blank line, the page line when paged, then the click hint.
    private static final int PAGE_LINE = 5;

    private static NavigatorFixture start(Env env, FakePlayerCounts counts) {
        return NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), counts);
    }

    @DisplayName("A hundred services open a first page of 45 with the page in the sort item and a next arrow only")
    @Test
    void firstPageOfAHundredServices(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(100));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        player.setLocale(Locale.GERMANY);
        try (NavigatorFixture fixture = start(env, counts)) {
            AbstractInventory picker = openPicker(env, fixture, player);

            Assertions.assertEquals("ElytraRace-000", nameOf(picker, 0), "the first page starts with the first service by name");
            Assertions.assertEquals("ElytraRace-044", nameOf(picker, 44), "the 45th service is the last one on the first page");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, picker.getItemStack(PREVIOUS_SLOT).material(), "there is no page before the first");
            Assertions.assertEquals("Nächste Seite · Seite 2/3", nameOf(picker, NEXT_SLOT), "the next arrow names the page it leads to");
            Assertions.assertEquals(BACK_NAME_DE, nameOf(picker, BACK_SLOT), "the back button sits at the start of the navigation row on the first page");
            Assertions.assertEquals("Seite 1/3", loreOf(picker, SORT_SLOT).get(PAGE_LINE), "the sort item's lore shows the page and the page count");
        }
    }

    @DisplayName("The second page has an arrow back and an arrow on, each naming its page")
    @Test
    void middlePageHasArrowsBothWays(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(100));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        player.setLocale(Locale.GERMANY);
        try (NavigatorFixture fixture = start(env, counts)) {
            openPicker(env, fixture, player);

            clickAndTick(env, player.getOpenInventory(), player, new Click.Left(NEXT_SLOT));

            AbstractInventory picker = player.getOpenInventory();
            Assertions.assertEquals("ElytraRace-045", nameOf(picker, 0), "the second page starts with the 46th service");
            Assertions.assertEquals("Vorige Seite · Seite 1/3", nameOf(picker, PREVIOUS_SLOT), "an arrow back shows from the second page on and names the page before");
            Assertions.assertEquals("Nächste Seite · Seite 3/3", nameOf(picker, NEXT_SLOT), "a page still follows, so the next arrow names the page after");
            Assertions.assertEquals(BACK_NAME_DE, nameOf(picker, BACK_SLOT), "the back button stays at the start of the navigation row on the second page");
            Assertions.assertEquals("Seite 2/3", loreOf(picker, SORT_SLOT).get(PAGE_LINE), "the sort item's lore moves with the page");
        }
    }

    @DisplayName("The last page holds the services that remain and no next arrow")
    @Test
    void lastPageHoldsTheRemainder(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(100));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        player.setLocale(Locale.GERMANY);
        try (NavigatorFixture fixture = start(env, counts)) {
            openPicker(env, fixture, player);
            clickAndTick(env, player.getOpenInventory(), player, new Click.Left(NEXT_SLOT));
            clickAndTick(env, player.getOpenInventory(), player, new Click.Left(NEXT_SLOT));

            AbstractInventory picker = player.getOpenInventory();
            Assertions.assertEquals("ElytraRace-090", nameOf(picker, 0), "the third page starts with the 91st service");
            Assertions.assertEquals("ElytraRace-099", nameOf(picker, 13), "the last service is on the last page, in the second row's centre column");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, picker.getItemStack(NEXT_SLOT).material(), "no page follows the last one");
            Assertions.assertEquals("Vorige Seite · Seite 2/3", nameOf(picker, PREVIOUS_SLOT), "the arrow back names the page before the last one");
            Assertions.assertEquals("Seite 3/3", loreOf(picker, SORT_SLOT).get(PAGE_LINE), "the sort item's lore shows the last page");
            Assertions.assertEquals(BACK_NAME_DE, nameOf(picker, BACK_SLOT), "the back button stays at the start of the navigation row on the last page");
        }
    }

    @DisplayName("One running service is centred in the first row, with the sort item in the middle of the navigation row and no arrows")
    @Test
    void oneServiceIsCentredWithoutArrows(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(1));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        player.setLocale(Locale.GERMANY);
        try (NavigatorFixture fixture = start(env, counts)) {
            AbstractInventory picker = openPicker(env, fixture, player);

            Assertions.assertEquals(18, picker.getSize(), "one row of services and the navigation row under it");
            Assertions.assertEquals("ElytraRace-000", nameOf(picker, 4), "the one entry is in column four, the middle of the first row");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, picker.getItemStack(3).material(), "the columns beside the one entry stay empty");
            Assertions.assertEquals("Sortierung: Name (A bis Z)", nameOf(picker, ONE_ROW_SORT_SLOT), "the sort item is the middle slot of the navigation row, navigation start + 4");
            Assertions.assertEquals(BACK_NAME_DE, nameOf(picker, ONE_ROW_BACK_SLOT), "the back button is the first slot of the navigation row, even without pages");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, picker.getItemStack(ONE_ROW_PREVIOUS_SLOT).material(), "no previous arrow without pages");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, picker.getItemStack(ONE_ROW_NEXT_SLOT).material(), "no next arrow without pages");
            Assertions.assertEquals(6, loreOf(picker, ONE_ROW_SORT_SLOT).size(), "no page line in the sort item's lore without pages");
        }
    }

    @DisplayName("A short list is centred in its row: six entries start in column one")
    @Test
    void shortListIsCentredAndShowsOnlyTheSortItem(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(6));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        player.setLocale(Locale.GERMANY);
        try (NavigatorFixture fixture = start(env, counts)) {
            AbstractInventory picker = openPicker(env, fixture, player);

            Assertions.assertEquals(18, picker.getSize(), "one row of services and the navigation row under it");
            Assertions.assertEquals("ElytraRace-000", nameOf(picker, 1), "(9 - 6) / 2 = 1: the first of six entries is in column one");
            Assertions.assertEquals("ElytraRace-005", nameOf(picker, 6), "the sixth entry is in column six");
            Assertions.assertEquals("Sortierung: Name (A bis Z)", nameOf(picker, ONE_ROW_SORT_SLOT), "the sort item is the middle slot of the navigation row");
            Assertions.assertEquals(BACK_NAME_DE, nameOf(picker, ONE_ROW_BACK_SLOT), "the back button is in the navigation row of a short list too");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, picker.getItemStack(ONE_ROW_PREVIOUS_SLOT).material(), "no previous arrow without pages");
        }
    }

    @DisplayName("An empty list explains itself in the first row and still shows the back button at the start of the navigation row")
    @Test
    void emptyListShowsTheBackButton(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        player.setLocale(Locale.GERMANY);
        try (NavigatorFixture fixture = start(env, counts)) {
            AbstractInventory picker = openPicker(env, fixture, player);

            Assertions.assertEquals(18, picker.getSize(), "one explanation row and the navigation row under it");
            Assertions.assertEquals("Gerade läuft kein Server.", nameOf(picker, ServerPicker.EMPTY_SLOT), "the explanation is in the middle of the first row");
            Assertions.assertEquals(BACK_NAME_DE, nameOf(picker, ONE_ROW_BACK_SLOT), "the back button is at the start of the navigation row on an empty list");
            Assertions.assertEquals("Sortierung: Name (A bis Z)", nameOf(picker, ONE_ROW_SORT_SLOT), "the sort item stays in the middle of the navigation row");
        }
    }

    @DisplayName("A player's order and page stay with that player's open list, and a new opening starts at name and page one")
    @Test
    void openingAfterAnotherPlayerChangedTheOrderStartsAtName(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(100));
        Player first = env.createPlayer(env.createFlatInstance(), SPAWN);
        Player second = env.createPlayer(env.createFlatInstance(), SPAWN);
        second.setLocale(Locale.ENGLISH);
        try (NavigatorFixture fixture = start(env, counts)) {
            openPicker(env, fixture, first);
            clickAndTick(env, first.getOpenInventory(), first, new Click.Left(SORT_SLOT));
            clickAndTick(env, first.getOpenInventory(), first, new Click.Left(SORT_SLOT));
            clickAndTick(env, first.getOpenInventory(), first, new Click.Left(SORT_SLOT));
            clickAndTick(env, first.getOpenInventory(), first, new Click.Left(NEXT_SLOT));
            Assertions.assertEquals("▸ Status", loreOf(first.getOpenInventory(), SORT_SLOT).get(3), "the first player is on status");

            AbstractInventory secondPicker = openPicker(env, fixture, second);

            Assertions.assertEquals("ElytraRace-000", nameOf(secondPicker, 0), "the second player starts with the first service by name");
            Assertions.assertEquals("▸ Name (A–Z)", loreOf(secondPicker, SORT_SLOT).get(0), "the second player's order is name");
            Assertions.assertEquals("Page 1/3", loreOf(secondPicker, SORT_SLOT).get(PAGE_LINE), "the second player is on the first page");
            Assertions.assertEquals("▸ Status", loreOf(first.getOpenInventory(), SORT_SLOT).get(3), "the first player stays on status after the second player opened the list");
            Assertions.assertEquals("Page 2/3", loreOf(first.getOpenInventory(), SORT_SLOT).get(PAGE_LINE), "the first player stays on page 2 after the second player opened the list");
        }
    }

    @DisplayName("The sort item names its orders in the player's language, and English for a language without a bundle")
    @Test
    void sortItemSpeaksThePlayersLanguage(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(6));
        Player german = env.createPlayer(env.createFlatInstance(), SPAWN);
        german.setLocale(Locale.GERMANY);
        Player french = env.createPlayer(env.createFlatInstance(), SPAWN);
        french.setLocale(Locale.FRANCE);
        try (NavigatorFixture fixture = start(env, counts)) {
            AbstractInventory germanPicker = openPicker(env, fixture, german);
            AbstractInventory frenchPicker = openPicker(env, fixture, french);

            Assertions.assertEquals("  Freie Plätze", loreOf(germanPicker, ONE_ROW_SORT_SLOT).get(2), "a German player reads the free slots order in German");
            Assertions.assertEquals("  Free slots", loreOf(frenchPicker, ONE_ROW_SORT_SLOT).get(2), "a player without a bundle reads English");
        }
    }

    @DisplayName("Reopening the list after closing it starts again at name and page one")
    @Test
    void reopeningStartsAtNameAndPageOne(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(100));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        player.setLocale(Locale.ENGLISH);
        try (NavigatorFixture fixture = start(env, counts)) {
            openPicker(env, fixture, player);
            clickAndTick(env, player.getOpenInventory(), player, new Click.Left(SORT_SLOT));
            clickAndTick(env, player.getOpenInventory(), player, new Click.Left(NEXT_SLOT));
            player.closeInventory();

            AbstractInventory reopened = openPicker(env, fixture, player);

            Assertions.assertEquals("ElytraRace-000", nameOf(reopened, 0), "the list opens on the first page");
            Assertions.assertEquals("▸ Name (A–Z)", loreOf(reopened, SORT_SLOT).get(0), "the list opens in name order");
        }
    }
}
