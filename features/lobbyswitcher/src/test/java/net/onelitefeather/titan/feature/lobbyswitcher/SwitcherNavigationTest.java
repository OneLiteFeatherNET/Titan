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
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The navigation row: the close button at its start, the indicator and the arrows beside the
 * centre.
 */
@ExtendWith(MicrotusExtension.class)
class SwitcherNavigationTest {

    private static final int PERIOD_TICKS = SwitcherFixture.REFRESH_SECONDS * ViewerCounter.TICKS_PER_SECOND;
    /** Three lobbies: one content row, so the navigation row starts at slot 9. */
    private static final SwitcherLayout SINGLE = SwitcherLayout.of(3);
    /** One hundred lobbies: three pages, so the navigation row starts at slot 45. */
    private static final SwitcherLayout PAGED = SwitcherLayout.of(100);

    private static Player openWith(SwitcherFixture fixture, int lobbies) {
        fixture.counts().serve(lobbies(lobbies));
        return fixture.joinAndOpen();
    }

    @DisplayName("One page shows the close button first in the navigation row and leaves the arrows and indicator empty")
    @Test
    void singlePageShowsOnlyTheCloseButton(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            AbstractInventory inventory = openWith(fixture, 3).getOpenInventory();

            Assertions.assertEquals(Material.BARRIER, inventory.getItemStack(9).material(), "the close button is the first slot of the navigation row");
            Assertions.assertEquals("Close", name(inventory.getItemStack(9)), "the close button is labelled");
            for (int slot : List.of(SINGLE.previousSlot(), SINGLE.indicatorSlot(), SINGLE.nextSlot())) {
                Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItemStack(slot).material(), "slot " + slot + " stays empty on one page");
            }
        }
    }

    @DisplayName("Clicking the close button closes the list")
    @Test
    void closeButtonClosesTheList(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 3);

            fixture.click(player, 9);

            Assertions.assertNull(player.getOpenInventory(), "the list is closed after the close button");
        }
    }

    @DisplayName("A list closed with the close button is not read again")
    @Test
    void closeButtonStopsTheRefresh(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 3);
            fixture.click(player, 9);
            int reads = fixture.counts().reads();

            for (int i = 0; i < PERIOD_TICKS * 3; i++) {
                env.tick();
            }

            Assertions.assertNull(player.getOpenInventory(), "still closed");
            Assertions.assertEquals(reads, fixture.counts().reads(), "no viewer left, so no read");
        }
    }

    @DisplayName("The close button is at the same place on the second page, and it closes from there")
    @Test
    void closeButtonWorksOnASecondPage(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);
            fixture.click(player, PAGED.nextSlot());

            Assertions.assertEquals("Close", name(player.getOpenInventory().getItemStack(PAGED.closeSlot())), "page 2 keeps the close button at slot 45");
            fixture.click(player, PAGED.closeSlot());
            Assertions.assertNull(player.getOpenInventory(), "closed from the second page");
        }
    }

    @DisplayName("The arrows sit three and five slots after the close button, the indicator in between")
    @Test
    void arrowsSitBesideTheIndicator(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);
            fixture.click(player, PAGED.nextSlot());
            AbstractInventory inventory = player.getOpenInventory();

            Assertions.assertEquals(Material.ARROW, inventory.getItemStack(45 + 3).material(), "previous arrow at close + 3");
            Assertions.assertEquals("Page 2/3", name(inventory.getItemStack(45 + 4)), "indicator at close + 4");
            Assertions.assertEquals(Material.ARROW, inventory.getItemStack(45 + 5).material(), "next arrow at close + 5");
        }
    }

    @DisplayName("The arrow at close + 3 goes back a page and the one at close + 5 goes on")
    @Test
    void arrowsTurnThePages(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = openWith(fixture, 100);

            fixture.click(player, 45 + 5);
            Assertions.assertEquals("Lobby-045", name(player.getOpenInventory().getItemStack(0)), "the arrow at close + 5 shows page 2");

            fixture.click(player, 45 + 3);
            Assertions.assertEquals("Lobby-000", name(player.getOpenInventory().getItemStack(0)), "the arrow at close + 3 shows page 1 again");
        }
    }

    @DisplayName("The close button label follows the client language")
    @Test
    void closeButtonIsGerman(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = fixture.join(env.createFlatInstance());
            player.setLocale(Locale.GERMAN);

            fixture.use(player);
            fixture.settle();

            Assertions.assertEquals("Schließen", name(player.getOpenInventory().getItemStack(9)), "German close label");
        }
    }
}
