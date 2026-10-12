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

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The menu is as big as the lobbies need and pages once six rows are not enough. */
@ExtendWith(MicrotusExtension.class)
class SwitcherPagingTest {

    private static final int PERIOD_TICKS = SwitcherFixture.REFRESH_SECONDS * ViewerCounter.TICKS_PER_SECOND;

    /** Zero-padded so that name order equals creation order; none of them is this lobby. */
    private static ServiceCount[] lobbies(int count) {
        return IntStream.range(0, count).mapToObj(i -> new ServiceCount("Lobby-%03d".formatted(i), 1, 50)).toArray(ServiceCount[]::new);
    }

    private static String name(ItemStack stack) {
        return PlainTextComponentSerializer.plainText().serialize(stack.get(DataComponents.CUSTOM_NAME));
    }

    private static Player openWith(SwitcherFixture fixture, int lobbies) {
        fixture.counts().serve(lobbies(lobbies));
        return fixture.joinAndOpen();
    }

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) {
            env.tick();
        }
    }

    @DisplayName("Two lobbies open a menu of one row")
    @Test
    void fewLobbiesNeedOneRow(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 2);

            Assertions.assertEquals(9, player.getOpenInventory().getSize(), "one row of nine slots");
        }
    }

    @DisplayName("Ten lobbies open a menu of two rows")
    @Test
    void tenLobbiesNeedTwoRows(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 10);

            Assertions.assertEquals(18, player.getOpenInventory().getSize(), "ten entries need two rows");
        }
    }

    @DisplayName("54 lobbies fill six rows without navigation")
    @Test
    void fullSixRowsHaveNoNavigation(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 54);

            Assertions.assertEquals(54, player.getOpenInventory().getSize());
            Assertions.assertEquals("Lobby-053", name(player.getOpenInventory().getItemStack(53)), "the last slot is the last lobby, not an arrow");
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
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItemStack(SwitcherLayout.PREVIOUS_SLOT).material(), "no previous page");
            Assertions.assertEquals(Material.ARROW, inventory.getItemStack(SwitcherLayout.NEXT_SLOT).material(), "next page");
            Assertions.assertTrue(name(inventory.getItemStack(SwitcherLayout.INDICATOR_SLOT)).contains("1/3"), "indicator: " + name(inventory.getItemStack(SwitcherLayout.INDICATOR_SLOT)));
        }
    }

    @DisplayName("Next shows the following lobbies and previous returns")
    @Test
    void navigatesForwardAndBack(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);

            fixture.click(player, SwitcherLayout.NEXT_SLOT);
            Assertions.assertEquals("Lobby-045", name(player.getOpenInventory().getItemStack(0)), "second page starts with the 46th lobby");
            Assertions.assertEquals(Material.ARROW, player.getOpenInventory().getItemStack(SwitcherLayout.PREVIOUS_SLOT).material());
            Assertions.assertEquals(Material.ARROW, player.getOpenInventory().getItemStack(SwitcherLayout.NEXT_SLOT).material());

            fixture.click(player, SwitcherLayout.NEXT_SLOT);
            Assertions.assertEquals("Lobby-090", name(player.getOpenInventory().getItemStack(0)));
            Assertions.assertEquals("Lobby-099", name(player.getOpenInventory().getItemStack(9)), "the last page holds the remaining ten");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, player.getOpenInventory().getItemStack(10).material());
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, player.getOpenInventory().getItemStack(SwitcherLayout.NEXT_SLOT).material(), "no page after the last");

            fixture.click(player, SwitcherLayout.PREVIOUS_SLOT);
            fixture.click(player, SwitcherLayout.PREVIOUS_SLOT);
            Assertions.assertEquals("Lobby-000", name(player.getOpenInventory().getItemStack(0)), "back on the first page");
        }
    }

    @DisplayName("Clicking a missing arrow keeps the page")
    @Test
    void missingArrowDoesNothing(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);

            fixture.click(player, SwitcherLayout.PREVIOUS_SLOT);

            Assertions.assertEquals("Lobby-000", name(player.getOpenInventory().getItemStack(0)));
            Assertions.assertTrue(fixture.deliver().deliveries().isEmpty(), "the filler is no lobby");
        }
    }

    @DisplayName("A lobby on the second page is the one that gets joined")
    @Test
    void secondPageClickJoinsThatLobby(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);
            fixture.click(player, SwitcherLayout.NEXT_SLOT);

            fixture.click(player, 2);
            env.tick();

            Assertions.assertEquals(List.of(new RecordingDeliver.Delivery(player, "Lobby-047")), fixture.deliver().deliveries());
        }
    }

    @DisplayName("Turning the page keeps the refresh running")
    @Test
    void navigationKeepsTheRefreshPeriod(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);
            fixture.click(player, SwitcherLayout.NEXT_SLOT);

            tick(env, PERIOD_TICKS + 10);

            Assertions.assertEquals(2, fixture.counts().reads(), "the open read plus one periodic read");
        }
    }

    @DisplayName("The page controls are rendered per locale")
    @Test
    void controlsAreTranslated(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            fixture.counts().serve(lobbies(100));
            Player player = fixture.join(env.createFlatInstance());
            player.setLocale(Locale.GERMAN);
            fixture.use(player);
            fixture.settle();

            Assertions.assertTrue(name(player.getOpenInventory().getItemStack(SwitcherLayout.NEXT_SLOT)).contains("Nächste"), name(player.getOpenInventory().getItemStack(SwitcherLayout.NEXT_SLOT)));
            Assertions.assertTrue(name(player.getOpenInventory().getItemStack(SwitcherLayout.INDICATOR_SLOT)).contains("Seite"), name(player.getOpenInventory().getItemStack(SwitcherLayout.INDICATOR_SLOT)));
        }
    }

    @DisplayName("A refresh with the same size keeps the menu open")
    @Test
    void sameSizeKeepsTheMenuOpen(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);
            fixture.counts().serve(lobbies(120));

            tick(env, PERIOD_TICKS + 10);

            Assertions.assertNotNull(player.getOpenInventory(), "100 and 120 lobbies are both three pages");
        }
    }

    @DisplayName("When the lobbies outgrow the rows the open menu closes and reopens at the new size")
    @Test
    void growingClosesTheOpenMenu(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 2);
            fixture.counts().serve(lobbies(12));

            tick(env, PERIOD_TICKS + 10);

            Assertions.assertNull(player.getOpenInventory(), "the nine-slot menu cannot show twelve lobbies");
            fixture.use(player);
            fixture.settle();
            Assertions.assertEquals(18, player.getOpenInventory().getSize(), "reopened with two rows");
            Assertions.assertEquals("Lobby-011", name(player.getOpenInventory().getItemStack(11)));
        }
    }

    @DisplayName("When the lobbies shrink to one page the paged menu closes and the next one has no arrows")
    @Test
    void shrinkingDropsThePaging(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);
            fixture.counts().serve(lobbies(3));

            tick(env, PERIOD_TICKS + 10);

            Assertions.assertNull(player.getOpenInventory(), "the paged menu is gone");
            fixture.use(player);
            fixture.settle();
            Assertions.assertEquals(9, player.getOpenInventory().getSize());
        }
    }

    @DisplayName("A menu closed by a resize stops the refresh until somebody opens it again")
    @Test
    void resizeCloseStopsTheRefresh(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            openWith(fixture, 2);
            fixture.counts().serve(lobbies(12));
            tick(env, PERIOD_TICKS + 10);
            int reads = fixture.counts().reads();

            tick(env, PERIOD_TICKS * 3);

            Assertions.assertEquals(reads, fixture.counts().reads(), "no viewer, no read");
        }
    }
}
