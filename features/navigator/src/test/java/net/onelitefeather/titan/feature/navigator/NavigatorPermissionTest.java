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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import net.onelitefeather.titan.core.testfixtures.FakePlayerCounts;
import net.onelitefeather.titan.core.testfixtures.RecordingDeliver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Which destinations the navigator shows and forwards, per permission: Slender needs
 * {@code titan.navigator.slender}, Bounce needs {@code titan.navigator.bounce}, Build needs
 * {@code titan.navigator.buildserver}, and each is independent of the others. {@code NOT_SET} and
 * {@code DENIED} both count as not granted.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorPermissionTest {

    private static final String SLENDER_PERMISSION = "titan.navigator.slender";
    private static final String BOUNCE_PERMISSION = "titan.navigator.bounce";
    private static final String BUILD_PERMISSION = "titan.navigator.buildserver";
    private static final List<Locale> LANGUAGES = List.of(Locale.ENGLISH, Locale.GERMAN);
    private static final int SLENDER_SLOT = 15;
    private static final int BOUNCE_SLOT = 13;
    private static final int BUILD_SLOT = 24;

    private static NavigatorFixture start(Env env, Deliver deliver, FakePermissionService permissions) {
        return NavigatorFixture.start(env, deliver, permissions);
    }

    private static AbstractInventory openNavigator(NavigatorFixture fixture, Player player) {
        fixture.useFeather(player);
        return player.getOpenInventory();
    }

    private static InventoryPreClickEvent click(Env env, AbstractInventory inventory, Player player, int slot) {
        InventoryPreClickEvent event = new InventoryPreClickEvent(inventory, player, new Click.Left(slot));
        env.process().eventHandler().call(event);
        return event;
    }

    @DisplayName("A player without any permission sees neither Slender nor Build")
    @Test
    void playerWithoutPermissionsSeesNeitherSlenderNorBuild(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), new FakePermissionService())) {
            fixture.equip(player);

            AbstractInventory menu = openNavigator(fixture, player);

            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getItemStack(SLENDER_SLOT).material(), "slot 15 must be blank without the Slender permission");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getItemStack(BUILD_SLOT).material(), "slot 24 must be blank without the Build permission");
        }
    }

    @DisplayName("A player with only the Slender permission sees Slender and not Build")
    @Test
    void playerWithOnlySlenderSeesSlenderNotBuild(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);

            AbstractInventory menu = openNavigator(fixture, player);

            Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, menu.getItemStack(SLENDER_SLOT).material(), "slot 15 must show Slender");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getItemStack(BUILD_SLOT).material(), "slot 24 must be blank without the Build permission");
        }
    }

    @DisplayName("A player with only the Build permission sees Build and not Slender")
    @Test
    void playerWithOnlyBuildSeesBuildNotSlender(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);

            AbstractInventory menu = openNavigator(fixture, player);

            Assertions.assertEquals(Material.SCAFFOLDING, menu.getItemStack(BUILD_SLOT).material(), "slot 24 must show Build");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getItemStack(SLENDER_SLOT).material(), "slot 15 must be blank without the Slender permission");
        }
    }

    @DisplayName("A player with both permissions sees Slender and Build")
    @Test
    void playerWithBothPermissionsSeesSlenderAndBuild(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED).set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);

            AbstractInventory menu = openNavigator(fixture, player);

            Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, menu.getItemStack(SLENDER_SLOT).material(), "slot 15 must show Slender");
            Assertions.assertEquals(Material.SCAFFOLDING, menu.getItemStack(BUILD_SLOT).material(), "slot 24 must show Build");
        }
    }

    @DisplayName("A denied Slender permission counts as not granted, like NOT_SET")
    @Test
    void deniedSlenderPermissionHidesSlender(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.DENIED);
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);

            AbstractInventory menu = openNavigator(fixture, player);

            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getItemStack(SLENDER_SLOT).material(), "slot 15 must be blank when the permission is denied");
        }
    }

    @DisplayName("A permission granted between two opens shows on the second open")
    @Test
    void permissionGrantedBetweenOpensShowsOnTheNextOpen(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService();
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, openNavigator(fixture, player).getItemStack(SLENDER_SLOT).material());
            player.closeInventory();

            permissions.set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED);

            Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, openNavigator(fixture, player).getItemStack(SLENDER_SLOT).material(), "slot 15 must show Slender on the second open");
        }
    }

    @DisplayName("Clicking Slender with its permission forwards to the Slender task")
    @Test
    void clickingSlenderWithPermissionForwards(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED);
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("Slender", new ServiceCount("Slender-1", 2, 10));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, permissions, counts)) {
            fixture.equip(player);
            AbstractInventory menu = openNavigator(fixture, player);

            click(env, menu, player, SLENDER_SLOT);
            NavigatorFixture.tick(env, 1);

            Assertions.assertEquals(1, deliver.deliveries().size(), "a click on Slender with its permission must forward once");
            Assertions.assertEquals("Slender-1", deliver.deliveries().get(0).serverName(), "the quick join goes to the ready Slender service");
        }
    }

    @DisplayName("Clicking Slender after its permission was revoked is denied and closes the menu")
    @Test
    void clickingSlenderAfterRevokeIsDenied(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = start(env, deliver, permissions)) {
            fixture.equip(player);
            AbstractInventory menu = openNavigator(fixture, player);
            permissions.set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.NOT_SET);

            InventoryPreClickEvent event = click(env, menu, player, SLENDER_SLOT);
            NavigatorFixture.tick(env, 1);

            Assertions.assertTrue(event.isCancelled(), "the click must be cancelled so the icon stays in place");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a revoked player must not be sent to Slender");
            Assertions.assertNull(player.getOpenInventory(), "the menu must close on a denied click");
        }
    }

    @DisplayName("Clicking Slender after its permission was denied is rejected and closes the menu")
    @Test
    void clickingSlenderAfterDenyIsRejected(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = start(env, deliver, permissions)) {
            fixture.equip(player);
            AbstractInventory menu = openNavigator(fixture, player);
            permissions.set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.DENIED);

            click(env, menu, player, SLENDER_SLOT);
            NavigatorFixture.tick(env, 1);

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a denied player must not be sent to Slender");
            Assertions.assertNull(player.getOpenInventory(), "the menu must close on a rejected click");
        }
    }

    @DisplayName("Players with the same permissions share one menu inventory")
    @Test
    void playersWithTheSamePermissionsShareOneMenu(Env env) {
        Player first = env.createPlayer(env.createFlatInstance());
        Player second = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(first.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED).set(second.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(first);
            fixture.equip(second);

            AbstractInventory firstMenu = openNavigator(fixture, first);
            AbstractInventory secondMenu = openNavigator(fixture, second);

            Assertions.assertSame(firstMenu, secondMenu, "players with the same grants must see the same shared menu");
        }
    }

    @DisplayName("A player with Slender does not share a menu with a player without it")
    @Test
    void slenderAndPlainPlayersGetDifferentMenus(Env env) {
        Player granted = env.createPlayer(env.createFlatInstance());
        Player plain = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(granted.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(granted);
            fixture.equip(plain);

            AbstractInventory grantedMenu = openNavigator(fixture, granted);
            AbstractInventory plainMenu = openNavigator(fixture, plain);

            Assertions.assertNotSame(grantedMenu, plainMenu, "a menu that shows Slender must not be the one a player without it sees");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, plainMenu.getItemStack(SLENDER_SLOT).material(), "the plain player's menu must keep slot 15 blank");
        }
    }

    @DisplayName("A player with the Bounce permission sees Bounce on slot 13")
    @Test
    void playerWithBouncePermissionSeesBounceOnSlot13(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BOUNCE_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);

            AbstractInventory menu = openNavigator(fixture, player);

            Assertions.assertEquals(Material.SLIME_BALL, menu.getItemStack(BOUNCE_SLOT).material(), "slot 13 must show Bounce");
        }
    }

    @DisplayName("A player with only the Bounce permission sees Bounce and not Slender")
    @Test
    void playerWithOnlyBounceSeesBounceNotSlender(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BOUNCE_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);

            AbstractInventory menu = openNavigator(fixture, player);

            Assertions.assertEquals(Material.SLIME_BALL, menu.getItemStack(BOUNCE_SLOT).material(), "slot 13 must show Bounce");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getItemStack(SLENDER_SLOT).material(), "slot 15 must be blank without the Slender permission");
        }
    }

    @DisplayName("Without the Bounce permission slot 13 is a grey pane")
    @Test
    void playerWithoutBouncePermissionSeesGreyPaneOnSlot13(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), new FakePermissionService())) {
            fixture.equip(player);

            AbstractInventory menu = openNavigator(fixture, player);

            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getItemStack(BOUNCE_SLOT).material(), "slot 13 must be blank without the Bounce permission");
        }
    }

    @DisplayName("A denied Bounce permission counts as not granted, like NOT_SET")
    @Test
    void deniedBouncePermissionHidesBounce(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BOUNCE_PERMISSION, PermissionResult.DENIED);
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);

            AbstractInventory menu = openNavigator(fixture, player);

            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getItemStack(BOUNCE_SLOT).material(), "slot 13 must be blank when the permission is denied");
        }
    }

    @DisplayName("A Bounce permission granted between two opens shows on the second open")
    @Test
    void bouncePermissionGrantedBetweenOpensShowsOnTheNextOpen(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService();
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, openNavigator(fixture, player).getItemStack(BOUNCE_SLOT).material());
            player.closeInventory();

            permissions.set(player.getUuid(), BOUNCE_PERMISSION, PermissionResult.ALLOWED);

            Assertions.assertEquals(Material.SLIME_BALL, openNavigator(fixture, player).getItemStack(BOUNCE_SLOT).material(), "slot 13 must show Bounce on the second open");
        }
    }

    @DisplayName("Clicking Bounce with its permission quick-joins the ready Bounce service")
    @Test
    void clickingBounceWithPermissionQuickJoins(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BOUNCE_PERMISSION, PermissionResult.ALLOWED);
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("Bounce", new ServiceCount("Bounce-1", 2, 10));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, permissions, counts)) {
            fixture.equip(player);
            AbstractInventory menu = openNavigator(fixture, player);

            click(env, menu, player, BOUNCE_SLOT);
            NavigatorFixture.tick(env, 1);

            Assertions.assertEquals(1, deliver.deliveries().size(), "a click on Bounce with its permission must forward once");
            Assertions.assertEquals("Bounce-1", deliver.deliveries().get(0).serverName(), "the quick join goes to the ready Bounce service");
        }
    }

    @DisplayName("Clicking Bounce after its permission was revoked is denied, closes the menu and sends no chat message")
    @Test
    void clickingBounceAfterRevokeIsDeniedWithoutChat(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BOUNCE_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = start(env, deliver, permissions)) {
            fixture.equip(player);
            AbstractInventory menu = openNavigator(fixture, player);
            permissions.set(player.getUuid(), BOUNCE_PERMISSION, PermissionResult.NOT_SET);

            InventoryPreClickEvent event = click(env, menu, player, BOUNCE_SLOT);
            NavigatorFixture.tick(env, 1);

            Assertions.assertTrue(event.isCancelled(), "the click must be cancelled so the icon stays in place");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a revoked player must not be sent to Bounce");
            Assertions.assertNull(player.getOpenInventory(), "the menu must close on a denied click");
            Assertions.assertEquals(0, chat.collect().size(), "a denied click must not send a chat message");
        }
    }

    @DisplayName("Starting the navigator creates ninety-six shared inventories: sixty-four for the main menu and thirty-two for the submenu")
    @Test
    void startCreatesNinetySixSharedInventories(Env env) {
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), new FakePermissionService())) {
            Set<Inventory> main = new HashSet<>();
            for (Set<Destination> granted : subsets(Destination.livePermissioned())) {
                for (boolean stagingHub : List.of(false, true)) {
                    for (Locale language : LANGUAGES) {
                        main.add(sharedOrFail(fixture.module().inventory(granted, stagingHub, language), "the main menu for " + granted + ", hub " + stagingHub + ", " + language));
                    }
                }
            }
            Set<Inventory> submenu = new HashSet<>();
            for (Set<Destination> granted : subsets(Destination.stagingEntries())) {
                for (Locale language : LANGUAGES) {
                    submenu.add(sharedOrFail(fixture.module().stagingInventory(granted, language), "the submenu for " + granted + ", " + language));
                }
            }

            Assertions.assertEquals(64, main.size(), "four live permissions plus the staging hub bit, in two languages, make sixty-four distinct main menus");
            Assertions.assertEquals(32, submenu.size(), "four staging permissions in two languages make thirty-two distinct submenus");
            Set<Inventory> all = new HashSet<>(main);
            all.addAll(submenu);
            Assertions.assertEquals(96, all.size(), "ninety-six distinct shared inventories in total");
        }
    }

    @DisplayName("Every main menu shows the entries of its key, and the hub only when a staging permission is held")
    @Test
    void everyMainMenuShowsTheEntriesOfItsKey(Env env) {
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), new FakePermissionService())) {
            for (Set<Destination> granted : subsets(Destination.livePermissioned())) {
                for (boolean stagingHub : List.of(false, true)) {
                    for (Locale language : LANGUAGES) {
                        AbstractInventory menu = fixture.module().inventory(granted, stagingHub, language);
                        String key = granted + ", hub " + stagingHub + ", " + language;
                        for (Destination destination : Destination.livePermissioned()) {
                            Material expected = granted.contains(destination) ? destination.item(List.of()).material() : Material.GRAY_STAINED_GLASS_PANE;
                            Assertions.assertEquals(expected, menu.getItemStack(destination.slot()).material(), destination + " on slot " + destination.slot() + " for " + key);
                        }
                        Material hub = stagingHub ? Material.STRUCTURE_BLOCK : Material.GRAY_STAINED_GLASS_PANE;
                        Assertions.assertEquals(hub, menu.getItemStack(NavigatorLayout.STAGING_HUB_SLOT).material(), "the staging hub on slot 16 for " + key);
                    }
                }
            }
        }
    }

    @DisplayName("Every submenu shows exactly the staging entries its key grants, and the back button on slot 8")
    @Test
    void everySubmenuShowsTheEntriesOfItsKey(Env env) {
        try (NavigatorFixture fixture = start(env, new RecordingDeliver(), new FakePermissionService())) {
            for (Set<Destination> granted : subsets(Destination.stagingEntries())) {
                for (Locale language : LANGUAGES) {
                    AbstractInventory submenu = fixture.module().stagingInventory(granted, language);
                    String key = granted + ", " + language;
                    Assertions.assertEquals(StagingMenu.SIZE, submenu.getSize(), "the submenu has nine slots for " + key);
                    for (Destination destination : Destination.stagingEntries()) {
                        Material expected = granted.contains(destination) ? destination.item(List.of()).material() : Material.GRAY_STAINED_GLASS_PANE;
                        Assertions.assertEquals(expected, submenu.getItemStack(destination.slot()).material(), destination + " on slot " + destination.slot() + " for " + key);
                    }
                    Assertions.assertEquals(Material.DARK_OAK_DOOR, submenu.getItemStack(StagingMenu.BACK_SLOT).material(), "the back button on slot 8 for " + key);
                }
            }
        }
    }

    private static Inventory sharedOrFail(Inventory inventory, String what) {
        Assertions.assertNotNull(inventory, "there must be a shared inventory for " + what);
        return inventory;
    }

    // Every subset of the given destinations, each one a grant set a menu key can name.
    private static List<Set<Destination>> subsets(List<Destination> destinations) {
        List<Set<Destination>> subsets = new ArrayList<>();
        for (int mask = 0; mask < 1 << destinations.size(); mask++) {
            Set<Destination> granted = new HashSet<>();
            for (int bit = 0; bit < destinations.size(); bit++) {
                if ((mask & 1 << bit) != 0) {
                    granted.add(destinations.get(bit));
                }
            }
            subsets.add(granted);
        }
        return subsets;
    }
}
