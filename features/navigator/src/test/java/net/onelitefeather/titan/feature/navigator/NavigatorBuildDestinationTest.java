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
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
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
 * End-to-end coverage for the permission-gated {@code BUILD} destination: which of the two shared
 * inventories a player gets on open, what a click on Build does, and that both inventories follow
 * the feature flag and stop reacting once the module has stopped.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorBuildDestinationTest {

    private static final int BUILD_SLOT = 24;
    private static final String BUILD_PERMISSION = "titan.navigator.buildserver";
    private static final String SLENDER_PERMISSION = "titan.navigator.slender";

    private static AbstractInventory openNavigator(NavigatorFixture fixture, Player player) {
        fixture.useFeather(player);
        return player.getOpenInventory();
    }

    private static InventoryPreClickEvent click(Env env, AbstractInventory inventory, Player player, int slot) {
        InventoryPreClickEvent event = new InventoryPreClickEvent(inventory, player, new Click.Left(slot));
        env.process().eventHandler().call(event);
        return event;
    }

    private static void assertPublicMenu(AbstractInventory inventory, boolean slenderVisible) {
        Assertions.assertEquals(Material.ELYTRA, inventory.getItemStack(11).material());
        Assertions.assertEquals(Material.COMPASS, inventory.getItemStack(31).material(), "slot 31 must show Spawn");
        Assertions.assertEquals(Material.GRASS_BLOCK, inventory.getItemStack(20).material());
        Assertions.assertEquals(slenderVisible ? Material.ENDERMAN_SPAWN_EGG : Material.GRAY_STAINED_GLASS_PANE, inventory.getItemStack(15).material(), "slot 15 has the wrong material");
        Assertions.assertEquals(Material.WOODEN_AXE, inventory.getItemStack(22).material());
        for (int slot : List.of(10, 12, 13, 14, BUILD_SLOT)) {
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItemStack(slot).material(), "slot " + slot + " should be a blank glass pane");
        }
    }

    @DisplayName("A player with the build permission sees Build on slot 24 and the other destinations unchanged")
    @Test
    void permittedPlayerSeesBuildOnSlotSeven(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().allowEveryone("titan.navigator.slender").set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);

            AbstractInventory inventory = openNavigator(fixture, player);

            Assertions.assertEquals(Material.SCAFFOLDING, inventory.getItemStack(BUILD_SLOT).material(), "slot 24 must show Build");
            Assertions.assertEquals(Material.ELYTRA, inventory.getItemStack(11).material());
            Assertions.assertEquals(Material.COMPASS, inventory.getItemStack(31).material(), "slot 31 must show Spawn");
            Assertions.assertEquals(Material.GRASS_BLOCK, inventory.getItemStack(20).material());
            Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, inventory.getItemStack(15).material());
            Assertions.assertEquals(Material.WOODEN_AXE, inventory.getItemStack(22).material());
            for (int slot : List.of(10, 12, 13, 14)) {
                Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItemStack(slot).material(), "slot " + slot + " should be a blank glass pane");
            }
        }
    }

    @DisplayName("A player whose build permission is not set gets the public menu, glass on slot 24")
    @Test
    void notSetPlayerSeesThePublicMenu(Env env) {
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone("titan.navigator.slender"))) {
            Player player = env.createPlayer(env.createFlatInstance());
            fixture.equip(player);

            AbstractInventory inventory = openNavigator(fixture, player);

            assertPublicMenu(inventory, true);
        }
    }

    @DisplayName("A player whose build permission is denied gets the public menu, glass on slot 24")
    @Test
    void deniedPlayerSeesThePublicMenu(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().allowEveryone("titan.navigator.slender").set(player.getUuid(), BUILD_PERMISSION, PermissionResult.DENIED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);

            AbstractInventory inventory = openNavigator(fixture, player);

            assertPublicMenu(inventory, true);
        }
    }

    @DisplayName("Clicking Build with the permission forwards to the Team-Build task and closes the inventory")
    @Test
    void clickingBuildForwardsToTheBuildTask(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().allowEveryone("titan.navigator.slender").set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, permissions)) {
            fixture.equip(player);
            AbstractInventory inventory = openNavigator(fixture, player);

            InventoryPreClickEvent event = click(env, inventory, player, BUILD_SLOT);

            Assertions.assertTrue(event.isCancelled(), "the click must be cancelled so the icon stays in place");
            Assertions.assertEquals(1, deliver.deliveries().size(), "exactly one delivery must be recorded");
            Assertions.assertEquals("Team-Build", deliver.deliveries().get(0).taskName());
            Assertions.assertNotSame(inventory, player.getOpenInventory(), "the navigator must close after a click");
        }
    }

    @DisplayName("Clicking Build after the permission was revoked forwards nothing, cancels the click and closes the inventory")
    @Test
    void clickingBuildAfterRevocationIsRefused(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().allowEveryone("titan.navigator.slender").set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, permissions)) {
            fixture.equip(player);
            AbstractInventory inventory = openNavigator(fixture, player);
            permissions.set(player.getUuid(), BUILD_PERMISSION, PermissionResult.NOT_SET);

            InventoryPreClickEvent event = click(env, inventory, player, BUILD_SLOT);

            Assertions.assertTrue(event.isCancelled(), "the click must still be cancelled");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a revoked permission must not forward the player");
            Assertions.assertNotSame(inventory, player.getOpenInventory(), "the navigator must close");
        }
    }

    @DisplayName("Clicking the glass pane on slot 24 of the public menu forwards nothing and keeps it open")
    @Test
    void clickingTheGlassPaneOnSlotSevenDoesNothing(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"))) {
            Player player = env.createPlayer(env.createFlatInstance());
            fixture.equip(player);
            AbstractInventory inventory = openNavigator(fixture, player);

            click(env, inventory, player, BUILD_SLOT);

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "the public menu has no Build to forward to");
            Assertions.assertSame(inventory, player.getOpenInventory(), "the navigator must stay open");
        }
    }

    @DisplayName("A Slender grant between opens shows in the public menu on the next open")
    @Test
    void slenderGrantReachesThePublicMenu(Env env) {
        FakePermissionService permissions = new FakePermissionService();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            Player player = env.createPlayer(env.createFlatInstance());
            fixture.equip(player);
            assertPublicMenu(openNavigator(fixture, player), false);
            player.closeInventory();

            permissions.set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED);

            assertPublicMenu(openNavigator(fixture, player), true);
        }
    }

    @DisplayName("A Slender grant between opens shows in the team menu on the next open")
    @Test
    void slenderGrantReachesTheTeamMenu(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);
            AbstractInventory first = openNavigator(fixture, player);
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, first.getItemStack(15).material(), "slot 15 must be blank without the Slender grant");
            Assertions.assertEquals(Material.SCAFFOLDING, first.getItemStack(BUILD_SLOT).material());
            player.closeInventory();

            permissions.set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED);

            AbstractInventory second = openNavigator(fixture, player);
            Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, second.getItemStack(15).material(), "slot 15 must show Slender after the grant");
            Assertions.assertEquals(Material.SCAFFOLDING, second.getItemStack(BUILD_SLOT).material(), "Build must survive the relayout");
        }
    }

    @DisplayName("Once the module is stopped, the public inventory no longer reacts to clicks")
    @Test
    void publicInventoryIsDeadAfterStop(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"))) {
            Player player = env.createPlayer(env.createFlatInstance());
            fixture.equip(player);
            AbstractInventory inventory = openNavigator(fixture, player);

            fixture.stopModule();

            InventoryPreClickEvent event = click(env, inventory, player, 20);
            Assertions.assertFalse(event.isCancelled(), "no feature code may run once the module has stopped");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "no delivery may happen once the module has stopped");
        }
    }

    @DisplayName("Once the module is stopped, the team inventory no longer reacts to clicks")
    @Test
    void teamInventoryIsDeadAfterStop(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().allowEveryone("titan.navigator.slender").set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, permissions)) {
            fixture.equip(player);
            AbstractInventory inventory = openNavigator(fixture, player);

            fixture.stopModule();

            InventoryPreClickEvent event = click(env, inventory, player, BUILD_SLOT);
            Assertions.assertFalse(event.isCancelled(), "no feature code may run once the module has stopped");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "no delivery may happen once the module has stopped");
        }
    }
}
