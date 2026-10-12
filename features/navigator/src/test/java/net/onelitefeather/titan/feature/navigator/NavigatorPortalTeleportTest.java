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
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalApproach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * End-to-end coverage for the click types on a destination: a left click connects, a right click
 * walks the player to the matching portal of the lobby map, and falls back to connecting when the
 * map has none.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorPortalTeleportTest {

    private static final int SURVIVAL_SLOT = 4;
    private static final int BUILD_SLOT = 7;
    private static final String BUILD_PERMISSION = "titan.navigator.buildserver";
    private static final Box AREA = new Box(new Vec(10, 40, 20), new Vec(12, 42, 22));
    private static final LobbyPortals SURVIVAL_PORTAL = () -> List.of(new Portal("survival", AREA, "Survival", null));

    private static FakeFeatureFlags slenderActive() {
        return new FakeFeatureFlags().declare(Destination.SLENDER_FLAG, true);
    }

    private static AbstractInventory open(NavigatorFixture fixture, Player player) {
        fixture.equip(player);
        fixture.useFeather(player);
        return player.getOpenInventory();
    }

    private static InventoryPreClickEvent click(Env env, AbstractInventory inventory, Player player, Click click) {
        InventoryPreClickEvent event = new InventoryPreClickEvent(inventory, player, click);
        env.process().eventHandler().call(event);
        return event;
    }

    @DisplayName("A right click teleports the player in front of the portal of that task, facing it, and sends nobody")
    @Test
    void rightClickTeleportsInFrontOfTheMatchingPortal(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(11.5, 40, 60));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), new FakePermissionService(), SURVIVAL_PORTAL)) {
            AbstractInventory inventory = open(fixture, player);

            InventoryPreClickEvent event = click(env, inventory, player, new Click.Right(SURVIVAL_SLOT));
            env.tick();

            Pos expected = PortalApproach.standingBefore(SURVIVAL_PORTAL.portals().getFirst(), new Pos(11.5, 40, 60));
            Assertions.assertTrue(event.isCancelled(), "the click must be cancelled so the icon stays in place");
            Assertions.assertEquals(expected, player.getPosition(), "the player must stand in front of the portal");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a teleport must not connect to the server");
            Assertions.assertNotSame(inventory, player.getOpenInventory(), "the navigator must close");
        }
    }

    @DisplayName("The spot in front of the portal is outside the portal's trigger")
    @Test
    void theSpotIsOutsideTheTrigger(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(11.5, 40, 60));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), new FakePermissionService(), SURVIVAL_PORTAL)) {
            AbstractInventory inventory = open(fixture, player);

            click(env, inventory, player, new Click.Right(SURVIVAL_SLOT));
            env.tick();

            Assertions.assertFalse(AREA.crossedBy(player.getPosition(), player.getPosition()), "landing inside the trigger would send the player away again");
        }
    }

    @DisplayName("A right click without a portal for the task connects to the server like a left click")
    @Test
    void rightClickWithoutPortalConnects(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(11.5, 40, 60));
        LobbyPortals otherTask = () -> List.of(new Portal("race", AREA, "ElytraRace", null));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), new FakePermissionService(), otherTask)) {
            AbstractInventory inventory = open(fixture, player);

            click(env, inventory, player, new Click.Right(SURVIVAL_SLOT));
            env.tick();

            Assertions.assertEquals(1, deliver.deliveries().size(), "exactly one delivery must be recorded");
            Assertions.assertEquals("Survival", deliver.deliveries().getFirst().taskName());
            Assertions.assertEquals(new Pos(11.5, 40, 60), player.getPosition(), "the player must not move");
            Assertions.assertNotSame(inventory, player.getOpenInventory(), "the navigator must close");
        }
    }

    @DisplayName("A left click connects to the server even when a portal for the task exists")
    @Test
    void leftClickStillConnects(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(11.5, 40, 60));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), new FakePermissionService(), SURVIVAL_PORTAL)) {
            AbstractInventory inventory = open(fixture, player);

            click(env, inventory, player, new Click.Left(SURVIVAL_SLOT));
            env.tick();

            Assertions.assertEquals("Survival", deliver.deliveries().getFirst().taskName());
            Assertions.assertEquals(new Pos(11.5, 40, 60), player.getPosition(), "the player must not move");
        }
    }

    @DisplayName("Other click types connect like a left click")
    @Test
    void otherClickTypesConnect(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(11.5, 40, 60));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), new FakePermissionService(), SURVIVAL_PORTAL)) {
            AbstractInventory inventory = open(fixture, player);

            click(env, inventory, player, new Click.LeftShift(SURVIVAL_SLOT));
            env.tick();

            Assertions.assertEquals(1, deliver.deliveries().size(), "a shift click connects");
            Assertions.assertEquals(new Pos(11.5, 40, 60), player.getPosition(), "the player must not move");
        }
    }

    @DisplayName("A right click on a destination the player may not use neither teleports nor connects")
    @Test
    void deniedRightClickDoesNothing(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(11.5, 40, 60));
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        LobbyPortals buildPortal = () -> List.of(new Portal("build", AREA, "Build", BUILD_PERMISSION));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), permissions, buildPortal)) {
            AbstractInventory inventory = open(fixture, player);
            permissions.set(player.getUuid(), BUILD_PERMISSION, PermissionResult.NOT_SET);

            click(env, inventory, player, new Click.Right(BUILD_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a refused destination must not connect");
            Assertions.assertEquals(new Pos(11.5, 40, 60), player.getPosition(), "a refused destination must not teleport");
            Assertions.assertNotSame(inventory, player.getOpenInventory(), "the navigator must close");
        }
    }

    @DisplayName("A denied left click still neither connects nor teleports")
    @Test
    void deniedLeftClickDoesNothing(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(11.5, 40, 60));
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        LobbyPortals buildPortal = () -> List.of(new Portal("build", AREA, "Build", BUILD_PERMISSION));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, slenderActive(), permissions, buildPortal)) {
            AbstractInventory inventory = open(fixture, player);
            permissions.set(player.getUuid(), BUILD_PERMISSION, PermissionResult.DENIED);

            click(env, inventory, player, new Click.Left(BUILD_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a refused destination must not connect");
            Assertions.assertEquals(new Pos(11.5, 40, 60), player.getPosition(), "a refused destination must not teleport");
        }
    }
}
