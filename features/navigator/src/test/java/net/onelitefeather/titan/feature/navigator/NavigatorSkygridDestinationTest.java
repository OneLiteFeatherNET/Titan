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
 * End-to-end coverage for the permission-gated {@code SKYGRID} destination in the server row: where
 * it shows on the main menu, that it stays hidden without its permission, and what a click on it
 * does.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorSkygridDestinationTest {

    private static final int SKYGRID_SLOT = 23;
    private static final String SKYGRID_PERMISSION = "titan.navigator.skygrid";
    private static final String BUILD_PERMISSION = "titan.navigator.buildserver";

    private static AbstractInventory openNavigator(NavigatorFixture fixture, Player player) {
        fixture.useFeather(player);
        return player.getOpenInventory();
    }

    private static InventoryPreClickEvent click(Env env, AbstractInventory inventory, Player player, int slot) {
        InventoryPreClickEvent event = new InventoryPreClickEvent(inventory, player, new Click.Left(slot));
        env.process().eventHandler().call(event);
        return event;
    }

    @DisplayName("A player with the skygrid permission sees SkyGrid on slot 23 beside Survival and Creative")
    @Test
    void permittedPlayerSeesSkygridOnSlot23(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), SKYGRID_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);

            AbstractInventory inventory = openNavigator(fixture, player);

            Assertions.assertEquals(Material.DIAMOND_ORE, inventory.getItemStack(SKYGRID_SLOT).material(), "slot 23 must show SkyGrid");
            Assertions.assertEquals(Material.GRASS_BLOCK, inventory.getItemStack(21).material(), "Survival moves to slot 21 when it is the only other server");
            Assertions.assertEquals(Material.WOODEN_AXE, inventory.getItemStack(22).material(), "Creative moves to slot 22");
        }
    }

    @DisplayName("With Build as well, SkyGrid follows Build and the server row stays centred")
    @Test
    void skygridFollowsBuildInTheCentredServerRow(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), SKYGRID_PERMISSION, PermissionResult.ALLOWED).set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);

            AbstractInventory inventory = openNavigator(fixture, player);

            Assertions.assertEquals(Material.GRASS_BLOCK, inventory.getItemStack(20).material(), "Survival moves to slot 20 with four servers");
            Assertions.assertEquals(Material.WOODEN_AXE, inventory.getItemStack(21).material(), "Creative moves to slot 21");
            Assertions.assertEquals(Material.SCAFFOLDING, inventory.getItemStack(22).material(), "Build moves to slot 22");
            Assertions.assertEquals(Material.DIAMOND_ORE, inventory.getItemStack(SKYGRID_SLOT).material(), "SkyGrid stays last on slot 23");
        }
    }

    @DisplayName("A player whose skygrid permission is not set sees no SkyGrid, and the server row closes up without a gap")
    @Test
    void notSetPlayerSeesNoSkygrid(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            fixture.equip(player);

            AbstractInventory inventory = openNavigator(fixture, player);

            Assertions.assertEquals(Material.AIR, inventory.getItemStack(SKYGRID_SLOT).material(), "slot 23 must stay empty without SkyGrid");
            Assertions.assertEquals(Material.GRASS_BLOCK, inventory.getItemStack(21).material(), "Survival stays on slot 21 without SkyGrid");
            Assertions.assertEquals(Material.WOODEN_AXE, inventory.getItemStack(22).material(), "Creative stays on slot 22 without SkyGrid");
        }
    }

    @DisplayName("A player whose skygrid permission is denied sees no SkyGrid")
    @Test
    void deniedPlayerSeesNoSkygrid(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), SKYGRID_PERMISSION, PermissionResult.DENIED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            fixture.equip(player);

            AbstractInventory inventory = openNavigator(fixture, player);

            Assertions.assertEquals(Material.AIR, inventory.getItemStack(SKYGRID_SLOT).material(), "a denied SkyGrid must not be shown");
        }
    }

    @DisplayName("Clicking SkyGrid with the permission forwards to the SkyGrid task and closes the inventory")
    @Test
    void clickingSkygridForwardsToTheSkygridTask(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), SKYGRID_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, permissions)) {
            fixture.equip(player);
            AbstractInventory inventory = openNavigator(fixture, player);

            InventoryPreClickEvent event = click(env, inventory, player, SKYGRID_SLOT);

            Assertions.assertTrue(event.isCancelled(), "the click must be cancelled so the icon stays in place");
            Assertions.assertEquals(1, deliver.deliveries().size(), "exactly one delivery must be recorded");
            Assertions.assertEquals("SkyGrid", deliver.deliveries().get(0).taskName());
            Assertions.assertNotSame(inventory, player.getOpenInventory(), "the navigator must close after a click");
        }
    }

    @DisplayName("Clicking SkyGrid after the permission was revoked forwards nothing and closes the inventory")
    @Test
    void clickingSkygridAfterRevocationIsRefused(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), SKYGRID_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, permissions)) {
            fixture.equip(player);
            AbstractInventory inventory = openNavigator(fixture, player);
            permissions.set(player.getUuid(), SKYGRID_PERMISSION, PermissionResult.NOT_SET);

            InventoryPreClickEvent event = click(env, inventory, player, SKYGRID_SLOT);

            Assertions.assertTrue(event.isCancelled(), "the click must still be cancelled");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a revoked permission must not forward the player");
            Assertions.assertNotSame(inventory, player.getOpenInventory(), "the navigator must close");
        }
    }
}
