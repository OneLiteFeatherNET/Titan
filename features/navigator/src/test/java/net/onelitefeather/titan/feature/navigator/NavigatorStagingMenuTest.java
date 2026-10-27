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
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import net.onelitefeather.titan.core.testfixtures.RecordingDeliver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The staging hub on slot 16 of the main grid and the submenu it opens: the four staging entries on
 * slots 0 to 3, each shown only with its own permission, and a back button on slot 8.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorStagingMenuTest {

    private static final int HUB_SLOT = NavigatorLayout.STAGING_HUB_SLOT;
    private static final int BACK_SLOT = StagingMenu.BACK_SLOT;
    private static final List<Material> STAGING_ICONS = List.of(Material.STRUCTURE_BLOCK, Material.SLIME_BLOCK, Material.FIREWORK_ROCKET, Material.AMETHYST_BLOCK);
    private static final Pos SPAWN = new Pos(11.5, 40, 60);

    private static AbstractInventory openNavigator(NavigatorFixture fixture, Player player) {
        fixture.equip(player);
        fixture.useFeather(player);
        return player.getOpenInventory();
    }

    // The hub is a click on the main grid, so the submenu opens as it does for a player.
    private static AbstractInventory openStagingMenu(Env env, NavigatorFixture fixture, Player player) {
        AbstractInventory navigator = openNavigator(fixture, player);
        click(env, navigator, player, new Click.Left(HUB_SLOT));
        return player.getOpenInventory();
    }

    private static void click(Env env, AbstractInventory inventory, Player player, Click click) {
        env.process().eventHandler().call(new InventoryPreClickEvent(inventory, player, click));
    }

    // The plain text of a stack's custom name; a stack without one has an empty name.
    private static String nameOf(AbstractInventory inventory, int slot) {
        ItemStack stack = inventory.getItemStack(slot);
        if (stack.get(DataComponents.CUSTOM_NAME) == null) {
            return "";
        }
        MiniMessage mini = MiniMessage.miniMessage();
        return mini.stripTags(mini.serialize(stack.get(DataComponents.CUSTOM_NAME)));
    }

    private static FakePermissionService allStagingFor(Player player) {
        FakePermissionService permissions = new FakePermissionService();
        for (Destination staging : Destination.stagingEntries()) {
            permissions.set(player.getUuid(), staging.permission(), PermissionResult.ALLOWED);
        }
        return permissions;
    }

    @DisplayName("Without any permission Tamias is not shown and the hub corner keeps the frame pane")
    @Test
    void noPermissionLeavesTamiasAndHubGrey(Env env) {
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            AbstractInventory menu = openNavigator(fixture, player);

            for (int slot = 0; slot < menu.getSize(); slot++) {
                Assertions.assertNotEquals(Material.AMETHYST_SHARD, menu.getItemStack(slot).material(), "Tamias is hidden without its permission, but slot " + slot + " shows it");
            }
            Assertions.assertEquals(Material.BLACK_STAINED_GLASS_PANE, menu.getItemStack(HUB_SLOT).material(), "the hub is hidden without a staging permission, so the frame pane shows");
        }
    }

    @DisplayName("The staging hub shows on slot 35 once any one staging permission is held")
    @Test
    void anyStagingPermissionShowsTheHub(Env env) {
        FakePermissionService permissions = new FakePermissionService();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            for (Destination staging : Destination.stagingEntries()) {
                Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
                permissions.set(player.getUuid(), staging.permission(), PermissionResult.ALLOWED);

                AbstractInventory menu = openNavigator(fixture, player);

                Assertions.assertEquals(Material.STRUCTURE_BLOCK, menu.getItemStack(HUB_SLOT).material(), "the hub shows for " + staging.permission());
            }
        }
    }

    @DisplayName("Staging entries are never shown on the main grid")
    @Test
    void stagingEntriesStayOffTheMainGrid(Env env) {
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), allStagingFor(player))) {
            AbstractInventory menu = openNavigator(fixture, player);

            for (int slot = 0; slot < menu.getSize(); slot++) {
                if (slot != HUB_SLOT) {
                    Assertions.assertFalse(STAGING_ICONS.contains(menu.getItemStack(slot).material()), "slot " + slot + " must not show a staging entry");
                }
            }
        }
    }

    @DisplayName("A click on the staging hub opens the submenu, which has nine slots")
    @Test
    void hubOpensTheNineSlotSubmenu(Env env) {
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), allStagingFor(player))) {
            AbstractInventory submenu = openStagingMenu(env, fixture, player);

            Assertions.assertEquals(9, submenu.getSize(), "the submenu is one row of nine slots");
            Assertions.assertSame(fixture.module().stagingInventory(Set.copyOf(Destination.stagingEntries()), Locale.ENGLISH), submenu, "the hub opens the shared submenu for the player's staging grants");
        }
    }

    @DisplayName("The submenu shows each staging entry on its own slot with its icon and name")
    @Test
    void submenuShowsEachEntryOnItsSlot(Env env) {
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), allStagingFor(player))) {
            AbstractInventory submenu = openStagingMenu(env, fixture, player);

            Assertions.assertEquals(Material.STRUCTURE_BLOCK, submenu.getItemStack(0).material(), "slot 0 is Slender (Staging)");
            Assertions.assertEquals("Slender (Staging)", nameOf(submenu, 0), "slot 0 carries the Slender (Staging) name");
            Assertions.assertEquals(Material.SLIME_BLOCK, submenu.getItemStack(1).material(), "slot 1 is Bounce (Staging)");
            Assertions.assertEquals("Bounce (Staging)", nameOf(submenu, 1), "slot 1 carries the Bounce (Staging) name");
            Assertions.assertEquals(Material.FIREWORK_ROCKET, submenu.getItemStack(2).material(), "slot 2 is ElytraRace (Staging)");
            Assertions.assertEquals("ElytraRace (Staging)", nameOf(submenu, 2), "slot 2 carries the ElytraRace (Staging) name");
            Assertions.assertEquals(Material.AMETHYST_BLOCK, submenu.getItemStack(3).material(), "slot 3 is Tamias (Staging)");
            Assertions.assertEquals("Tamias (Staging)", nameOf(submenu, 3), "slot 3 carries the Tamias (Staging) name");
        }
    }

    @DisplayName("A staging entry without its own permission is a grey pane, while the granted one shows")
    @Test
    void entryWithoutItsPermissionIsGrey(Env env) {
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), Destination.SLENDER_STAGING.permission(), PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            AbstractInventory submenu = openStagingMenu(env, fixture, player);

            Assertions.assertEquals(Material.STRUCTURE_BLOCK, submenu.getItemStack(0).material(), "the granted Slender (Staging) shows on slot 0");
            for (int slot : new int[]{1, 2, 3}) {
                Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, submenu.getItemStack(slot).material(), "slot " + slot + " has no granted entry and is grey");
            }
        }
    }

    @DisplayName("The submenu leaves the slots that are neither an entry nor the back button grey")
    @Test
    void unusedSubmenuSlotsAreGrey(Env env) {
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), allStagingFor(player))) {
            AbstractInventory submenu = openStagingMenu(env, fixture, player);

            for (int slot = 4; slot < BACK_SLOT; slot++) {
                Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, submenu.getItemStack(slot).material(), "slot " + slot + " is unused and grey");
            }
        }
    }

    @DisplayName("The back button on slot 8 returns to the navigator")
    @Test
    void backButtonReturnsToTheNavigator(Env env) {
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), allStagingFor(player))) {
            AbstractInventory submenu = openStagingMenu(env, fixture, player);

            click(env, submenu, player, new Click.Left(BACK_SLOT));

            Assertions.assertEquals(Material.DARK_OAK_DOOR, submenu.getItemStack(BACK_SLOT).material(), "slot 8 holds the back button");
            Assertions.assertSame(fixture.module().inventory(Set.of(), true, Locale.ENGLISH), player.getOpenInventory(), "the back button opens the navigator with the staging hub");
        }
    }
}
