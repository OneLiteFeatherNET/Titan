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
    // The sort item of a list that fits one row: the third slot of its navigation row.
    private static final int ONE_ROW_SORT_SLOT = 11;
    private static final int PREVIOUS_SLOT = 45;
    private static final int SORT_SLOT = 47;
    private static final int INDICATOR_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private static NavigatorFixture start(Env env, FakePlayerCounts counts) {
        return NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), counts);
    }

    @DisplayName("A hundred services open a first page of 45 with a page indicator and a next arrow only")
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
            Assertions.assertEquals("Seite 1/3", nameOf(picker, INDICATOR_SLOT), "the indicator shows the page and the page count");
            Assertions.assertEquals(Material.ARROW, picker.getItemStack(NEXT_SLOT).material(), "a page follows, so the next arrow shows");
        }
    }

    @DisplayName("The second page has an arrow back and an arrow on")
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
            Assertions.assertEquals(Material.ARROW, picker.getItemStack(PREVIOUS_SLOT).material(), "an arrow back shows from the second page on");
            Assertions.assertEquals(Material.ARROW, picker.getItemStack(NEXT_SLOT).material(), "a page still follows");
            Assertions.assertEquals("Seite 2/3", nameOf(picker, INDICATOR_SLOT), "the indicator moves with the page");
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
            Assertions.assertEquals("ElytraRace-099", nameOf(picker, 9), "the last service is on the last page");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, picker.getItemStack(NEXT_SLOT).material(), "no page follows the last one");
            Assertions.assertEquals("Seite 3/3", nameOf(picker, INDICATOR_SLOT), "the indicator shows the last page");
        }
    }

    @DisplayName("A short list has no page navigation, only the sort item under its services")
    @Test
    void shortListShowsOnlyTheSortItem(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", services(6));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        player.setLocale(Locale.GERMANY);
        try (NavigatorFixture fixture = start(env, counts)) {
            AbstractInventory picker = openPicker(env, fixture, player);

            Assertions.assertEquals(18, picker.getSize(), "one row of services and the navigation row under it");
            Assertions.assertEquals("Sortierung: Name (A bis Z)", nameOf(picker, 11), "the sort item is the third slot of the navigation row");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, picker.getItemStack(9).material(), "no previous arrow without pages");
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
            Assertions.assertEquals("Page 1/3", nameOf(secondPicker, INDICATOR_SLOT), "the second player is on the first page");
            Assertions.assertEquals("▸ Status", loreOf(first.getOpenInventory(), SORT_SLOT).get(3), "the first player stays on status after the second player opened the list");
            Assertions.assertEquals("Page 2/3", nameOf(first.getOpenInventory(), INDICATOR_SLOT), "the first player stays on page 2 after the second player opened the list");
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
