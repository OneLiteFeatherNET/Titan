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
package net.onelitefeather.titan.feature.lobbyswitcher;

import static net.onelitefeather.titan.feature.lobbyswitcher.SwitcherFixture.lobbies;
import static net.onelitefeather.titan.feature.lobbyswitcher.SwitcherFixture.name;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryOpenEvent;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.RecordingDeliver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The menu is as big as the lobbies need and pages once six rows are not enough. */
@ExtendWith(MicrotusExtension.class)
class SwitcherPagingTest {

    private static final int PERIOD_TICKS = SwitcherFixture.REFRESH_SECONDS * ViewerCounter.TICKS_PER_SECOND;
    /** The layout of 100 lobbies: three pages, so every arrow and the indicator are shown. */
    private static final SwitcherLayout PAGED = SwitcherLayout.of(100);

    private static Player openWith(SwitcherFixture fixture, int lobbies) {
        fixture.counts().serveServices(SwitcherFixture.OWN.task(), lobbies(lobbies));
        return fixture.joinAndOpen();
    }

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) {
            env.tick();
        }
    }

    @DisplayName("Two lobbies open a menu of one content row and the navigation row")
    @Test
    void fewLobbiesNeedOneContentRow(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 2);

            Assertions.assertEquals(18, player.getOpenInventory().getSize(), "one content row of nine slots and the navigation row");
        }
    }

    @DisplayName("Ten lobbies open a menu of two content rows and the navigation row")
    @Test
    void tenLobbiesNeedTwoContentRows(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 10);

            Assertions.assertEquals(27, player.getOpenInventory().getSize(), "ten entries need two content rows and the navigation row");
        }
    }

    @DisplayName("45 lobbies fill five content rows with the navigation row below them")
    @Test
    void fullContentRowsAreAboveTheNavigation(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 45);

            Assertions.assertEquals(54, player.getOpenInventory().getSize(), "five content rows and the navigation row");
            Assertions.assertEquals("Lobby-044", name(player.getOpenInventory().getItemStack(44)), "slot 44 is the 45th lobby, the last content slot");
            Assertions.assertEquals(Material.BARRIER, player.getOpenInventory().getItemStack(45).material(), "the first slot of the navigation row is the close button");
        }
    }

    @DisplayName("More lobbies than fit show 45 per page and a next arrow on the first page only")
    @Test
    void firstPageOffersNextOnly(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);

            var inventory = player.getOpenInventory();
            Assertions.assertEquals(54, inventory.getSize());
            Assertions.assertEquals("Lobby-044", name(inventory.getItemStack(44)), "slot 44 is the 45th lobby");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItemStack(PAGED.previousSlot()).material(), "no previous page");
            Assertions.assertEquals(Material.ARROW, inventory.getItemStack(PAGED.nextSlot()).material(), "next page");
            Assertions.assertTrue(name(inventory.getItemStack(PAGED.indicatorSlot())).contains("1/3"), "indicator: " + name(inventory.getItemStack(PAGED.indicatorSlot())));
        }
    }

    @DisplayName("Next shows the following lobbies and previous returns")
    @Test
    void navigatesForwardAndBack(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);

            fixture.click(player, PAGED.nextSlot());
            Assertions.assertEquals("Lobby-045", name(player.getOpenInventory().getItemStack(0)), "second page starts with the 46th lobby");
            Assertions.assertEquals(Material.ARROW, player.getOpenInventory().getItemStack(PAGED.previousSlot()).material());
            Assertions.assertEquals(Material.ARROW, player.getOpenInventory().getItemStack(PAGED.nextSlot()).material());

            fixture.click(player, PAGED.nextSlot());
            Assertions.assertEquals("Lobby-090", name(player.getOpenInventory().getItemStack(0)));
            Assertions.assertEquals("Lobby-099", name(player.getOpenInventory().getItemStack(9)), "the last page holds the remaining ten");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, player.getOpenInventory().getItemStack(10).material());
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, player.getOpenInventory().getItemStack(PAGED.nextSlot()).material(), "no page after the last");

            fixture.click(player, PAGED.previousSlot());
            fixture.click(player, PAGED.previousSlot());
            Assertions.assertEquals("Lobby-000", name(player.getOpenInventory().getItemStack(0)), "back on the first page");
        }
    }

    @DisplayName("Clicking a missing arrow keeps the page")
    @Test
    void missingArrowDoesNothing(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);

            fixture.click(player, PAGED.previousSlot());

            Assertions.assertEquals("Lobby-000", name(player.getOpenInventory().getItemStack(0)));
            Assertions.assertTrue(fixture.deliver().deliveries().isEmpty(), "the filler is no lobby");
        }
    }

    @DisplayName("A lobby on the second page is the one that gets joined")
    @Test
    void secondPageClickJoinsThatLobby(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);
            fixture.click(player, PAGED.nextSlot());

            fixture.click(player, 2);
            env.tick();

            Assertions.assertEquals(List.of(RecordingDeliver.Delivery.toServer(player, "Lobby-047")), fixture.deliver().deliveries());
        }
    }

    @DisplayName("Turning the page keeps the refresh running")
    @Test
    void navigationKeepsTheRefreshPeriod(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);
            fixture.click(player, PAGED.nextSlot());

            tick(env, PERIOD_TICKS + 10);

            Assertions.assertEquals(2, fixture.counts().reads(), "the open read plus one periodic read");
        }
    }

    @DisplayName("The page controls are rendered per locale")
    @Test
    void controlsAreTranslated(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            fixture.counts().serveServices(SwitcherFixture.OWN.task(), lobbies(100));
            Player player = fixture.join(env.createFlatInstance());
            player.setLocale(Locale.GERMAN);
            fixture.use(player);
            fixture.settle();

            Assertions.assertTrue(name(player.getOpenInventory().getItemStack(PAGED.nextSlot())).contains("Nächste"), name(player.getOpenInventory().getItemStack(PAGED.nextSlot())));
            Assertions.assertTrue(name(player.getOpenInventory().getItemStack(PAGED.indicatorSlot())).contains("Seite"), name(player.getOpenInventory().getItemStack(PAGED.indicatorSlot())));
        }
    }

    @DisplayName("A refresh with the same size keeps the menu open and shows the new lobby count")
    @Test
    void sameSizeKeepsTheMenuOpen(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);
            fixture.counts().serveServices(SwitcherFixture.OWN.task(), lobbies(120));

            tick(env, PERIOD_TICKS + 10);

            Assertions.assertNotNull(player.getOpenInventory(), "100 and 120 lobbies are both three pages");
            fixture.click(player, PAGED.nextSlot());
            fixture.settle();
            fixture.click(player, PAGED.nextSlot());
            fixture.settle();
            Assertions.assertEquals("Lobby-100", name(player.getOpenInventory().getItemStack(10)), "the third page of 120 lobbies holds Lobby-100 in slot 10");
        }
    }

    @DisplayName("When the lobbies outgrow the rows the open menu closes and reopens at the new size")
    @Test
    void growingClosesTheOpenMenu(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 2);
            fixture.counts().serveServices(SwitcherFixture.OWN.task(), lobbies(12));

            tick(env, PERIOD_TICKS + 10);

            Assertions.assertNull(player.getOpenInventory(), "the nine-slot menu cannot show twelve lobbies");
            fixture.use(player);
            fixture.settle();
            Assertions.assertEquals(27, player.getOpenInventory().getSize(), "reopened with two content rows and the navigation row");
            Assertions.assertEquals("Lobby-011", name(player.getOpenInventory().getItemStack(11)));
        }
    }

    @DisplayName("When the lobbies shrink to one page the paged menu closes and the next one has no arrows")
    @Test
    void shrinkingDropsThePaging(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);
            fixture.counts().serveServices(SwitcherFixture.OWN.task(), lobbies(3));

            tick(env, PERIOD_TICKS + 10);

            Assertions.assertNull(player.getOpenInventory(), "the paged menu is gone");
            fixture.use(player);
            fixture.settle();
            Assertions.assertEquals(18, player.getOpenInventory().getSize(), "one content row and the navigation row");
        }
    }

    @DisplayName("A menu closed by a resize stops the refresh until somebody opens it again")
    @Test
    void resizeCloseStopsTheRefresh(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            openWith(fixture, 2);
            fixture.counts().serveServices(SwitcherFixture.OWN.task(), lobbies(12));
            tick(env, PERIOD_TICKS + 10);
            int reads = fixture.counts().reads();

            tick(env, PERIOD_TICKS * 3);

            Assertions.assertEquals(reads, fixture.counts().reads(), "no viewer, no read");
        }
    }

    @DisplayName("Closing the list after a page turn that failed stops the refresh")
    @Test
    void closeAfterFailedTurnStopsTheRefresh(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);
            AtomicBoolean failing = new AtomicBoolean();
            env.process().eventHandler().addListener(InventoryOpenEvent.class, event -> {
                if (failing.get()) {
                    throw new IllegalStateException("the next page cannot be opened");
                }
            });

            failing.set(true);
            // The Env reports a server-side exception as a thrown error; that is how the failed turn reaches the test.
            Assertions.assertThrows(Throwable.class, () -> fixture.click(player, PAGED.nextSlot()));
            failing.set(false);
            player.closeInventory();
            int reads = fixture.counts().reads();

            tick(env, PERIOD_TICKS * 3);

            Assertions.assertEquals(reads, fixture.counts().reads(), "the only viewer closed the list, so nothing is read any more");
        }
    }
}
