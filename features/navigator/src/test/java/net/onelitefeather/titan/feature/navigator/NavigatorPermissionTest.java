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

import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Which destinations the navigator shows and forwards, per permission: Slender needs
 * {@code titan.navigator.slender}, Build needs {@code titan.navigator.buildserver}, and each is
 * independent of the other. {@code NOT_SET} and {@code DENIED} both count as not granted.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorPermissionTest {

    private static final String SLENDER_PERMISSION = "titan.navigator.slender";
    private static final String BUILD_PERMISSION = "titan.navigator.buildserver";
    private static final int SLENDER_SLOT = 15;
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
}
