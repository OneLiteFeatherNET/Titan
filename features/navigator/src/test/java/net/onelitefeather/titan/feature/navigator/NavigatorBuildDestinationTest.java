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

    private static final int BUILD_SLOT = 7;
    private static final String BUILD_PERMISSION = "titan.navigator.buildserver";

    private static FakeFeatureFlags slenderActive() {
        return new FakeFeatureFlags().declare(Destination.SLENDER_FLAG, true);
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

    private static void assertPublicMenu(AbstractInventory inventory, boolean slenderVisible) {
        Assertions.assertEquals(Material.ELYTRA, inventory.getItemStack(0).material());
        Assertions.assertEquals(Material.COMPASS, inventory.getItemStack(2).material(), "slot 2 must show Spawn");
        Assertions.assertEquals(Material.GRASS_BLOCK, inventory.getItemStack(4).material());
        Assertions.assertEquals(slenderVisible ? Material.ENDERMAN_SPAWN_EGG : Material.GRAY_STAINED_GLASS_PANE, inventory.getItemStack(5).material(), "slot 5 has the wrong material");
        Assertions.assertEquals(Material.WOODEN_AXE, inventory.getItemStack(8).material());
        for (int slot : List.of(1, 3, 6, BUILD_SLOT)) {
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItemStack(slot).material(), "slot " + slot + " should be a blank glass pane");
        }
    }

    @DisplayName("A player with the build permission sees Build on slot 7 and the other destinations unchanged")
    @Test
    void permittedPlayerSeesBuildOnSlotSeven(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), permissions)) {
            fixture.equip(player);

            AbstractInventory inventory = openNavigator(fixture, player);

            Assertions.assertEquals(Material.SCAFFOLDING, inventory.getItemStack(BUILD_SLOT).material(), "slot 7 must show Build");
            Assertions.assertEquals(Material.ELYTRA, inventory.getItemStack(0).material());
            Assertions.assertEquals(Material.COMPASS, inventory.getItemStack(2).material(), "slot 2 must show Spawn");
            Assertions.assertEquals(Material.GRASS_BLOCK, inventory.getItemStack(4).material());
            Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, inventory.getItemStack(5).material());
            Assertions.assertEquals(Material.WOODEN_AXE, inventory.getItemStack(8).material());
            for (int slot : List.of(1, 3, 6)) {
                Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItemStack(slot).material(), "slot " + slot + " should be a blank glass pane");
            }
        }
    }

    @DisplayName("A player whose build permission is not set gets the public menu, glass on slot 7")
    @Test
    void notSetPlayerSeesThePublicMenu(Env env) {
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), new FakePermissionService())) {
            Player player = env.createPlayer(env.createFlatInstance());
            fixture.equip(player);

            AbstractInventory inventory = openNavigator(fixture, player);

            assertPublicMenu(inventory, true);
        }
    }

    @DisplayName("A player whose build permission is denied gets the public menu, glass on slot 7")
    @Test
    void deniedPlayerSeesThePublicMenu(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BUILD_PERMISSION, PermissionResult.DENIED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), permissions)) {
            fixture.equip(player);

            AbstractInventory inventory = openNavigator(fixture, player);

            assertPublicMenu(inventory, true);
        }
    }

    @DisplayName("Clicking Build with the permission forwards to the Build task and closes the inventory")
    @Test
    void clickingBuildForwardsToTheBuildTask(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), permissions)) {
            fixture.equip(player);
            AbstractInventory inventory = openNavigator(fixture, player);

            InventoryPreClickEvent event = click(env, inventory, player, BUILD_SLOT);

            Assertions.assertTrue(event.isCancelled(), "the click must be cancelled so the icon stays in place");
            Assertions.assertEquals(1, deliver.deliveries().size(), "exactly one delivery must be recorded");
            Assertions.assertEquals("Build", deliver.deliveries().get(0).taskName());
            Assertions.assertNotSame(inventory, player.getOpenInventory(), "the navigator must close after a click");
        }
    }

    @DisplayName("Clicking Build after the permission was revoked forwards nothing, cancels the click and closes the inventory")
    @Test
    void clickingBuildAfterRevocationIsRefused(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), permissions)) {
            fixture.equip(player);
            AbstractInventory inventory = openNavigator(fixture, player);
            permissions.set(player.getUuid(), BUILD_PERMISSION, PermissionResult.NOT_SET);

            InventoryPreClickEvent event = click(env, inventory, player, BUILD_SLOT);

            Assertions.assertTrue(event.isCancelled(), "the click must still be cancelled");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a revoked permission must not forward the player");
            Assertions.assertNotSame(inventory, player.getOpenInventory(), "the navigator must close");
        }
    }

    @DisplayName("Clicking the glass pane on slot 7 of the public menu forwards nothing and keeps it open")
    @Test
    void clickingTheGlassPaneOnSlotSevenDoesNothing(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), new FakePermissionService())) {
            Player player = env.createPlayer(env.createFlatInstance());
            fixture.equip(player);
            AbstractInventory inventory = openNavigator(fixture, player);

            click(env, inventory, player, BUILD_SLOT);

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "the public menu has no Build to forward to");
            Assertions.assertSame(inventory, player.getOpenInventory(), "the navigator must stay open");
        }
    }

    @DisplayName("A flag change shows in the public menu on the next open")
    @Test
    void flagChangeReachesThePublicMenu(Env env) {
        FakeFeatureFlags flags = new FakeFeatureFlags().declare(Destination.SLENDER_FLAG, false);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), flags, new FakePermissionService())) {
            Player player = env.createPlayer(env.createFlatInstance());
            fixture.equip(player);
            assertPublicMenu(openNavigator(fixture, player), false);
            player.closeInventory();

            flags.set(Destination.SLENDER_FLAG, true);

            assertPublicMenu(openNavigator(fixture, player), true);
        }
    }

    @DisplayName("A flag change shows in the team menu on the next open")
    @Test
    void flagChangeReachesTheTeamMenu(Env env) {
        FakeFeatureFlags flags = new FakeFeatureFlags().declare(Destination.SLENDER_FLAG, false);
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), flags, permissions)) {
            fixture.equip(player);
            AbstractInventory first = openNavigator(fixture, player);
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, first.getItemStack(5).material(), "slot 5 must be blank while the flag is off");
            Assertions.assertEquals(Material.SCAFFOLDING, first.getItemStack(BUILD_SLOT).material());
            player.closeInventory();

            flags.set(Destination.SLENDER_FLAG, true);

            AbstractInventory second = openNavigator(fixture, player);
            Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, second.getItemStack(5).material(), "slot 5 must show Slender after the flag flipped");
            Assertions.assertEquals(Material.SCAFFOLDING, second.getItemStack(BUILD_SLOT).material(), "Build must survive the relayout");
        }
    }

    @DisplayName("Once the module is stopped, the public inventory no longer reacts to clicks")
    @Test
    void publicInventoryIsDeadAfterStop(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), new FakePermissionService())) {
            Player player = env.createPlayer(env.createFlatInstance());
            fixture.equip(player);
            AbstractInventory inventory = openNavigator(fixture, player);

            fixture.stopModule();

            InventoryPreClickEvent event = click(env, inventory, player, 4);
            Assertions.assertFalse(event.isCancelled(), "no feature code may run once the module has stopped");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "no delivery may happen once the module has stopped");
        }
    }

    @DisplayName("Once the module is stopped, the team inventory no longer reacts to clicks")
    @Test
    void teamInventoryIsDeadAfterStop(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), permissions)) {
            fixture.equip(player);
            AbstractInventory inventory = openNavigator(fixture, player);

            fixture.stopModule();

            InventoryPreClickEvent event = click(env, inventory, player, BUILD_SLOT);
            Assertions.assertFalse(event.isCancelled(), "no feature code may run once the module has stopped");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "no delivery may happen once the module has stopped");
        }
    }
}
