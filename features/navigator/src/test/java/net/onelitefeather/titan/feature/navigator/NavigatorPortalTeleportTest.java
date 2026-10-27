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
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import net.onelitefeather.titan.core.testfixtures.RecordingDeliver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * End-to-end coverage for the click types on a server entry: a left or right click connects, a
 * shift click walks the player to the matching portal of the lobby map and falls back to connecting
 * when the map has none, and every other click type does nothing.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorPortalTeleportTest {

    private static final int SURVIVAL_SLOT = 21;
    private static final int BUILD_SLOT = 23;
    private static final String BUILD_PERMISSION = "titan.navigator.buildserver";
    private static final Box AREA = new Box(new Vec(10, 40, 20), new Vec(12, 42, 22));
    private static final LobbyPortals SURVIVAL_PORTAL = () -> List.of(new Portal("survival", AREA, "Survival", null));

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

    @DisplayName("A shift click teleports the player in front of the portal of that task, facing it, and sends nobody")
    @Test
    void shiftClickTeleportsInFrontOfTheMatchingPortal(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(11.5, 40, 60));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), SURVIVAL_PORTAL)) {
            AbstractInventory inventory = open(fixture, player);

            InventoryPreClickEvent event = click(env, inventory, player, new Click.LeftShift(SURVIVAL_SLOT));
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
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone("titan.navigator.slender"), SURVIVAL_PORTAL)) {
            AbstractInventory inventory = open(fixture, player);

            click(env, inventory, player, new Click.LeftShift(SURVIVAL_SLOT));
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
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), otherTask)) {
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
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), SURVIVAL_PORTAL)) {
            AbstractInventory inventory = open(fixture, player);

            click(env, inventory, player, new Click.Left(SURVIVAL_SLOT));
            env.tick();

            Assertions.assertEquals("Survival", deliver.deliveries().getFirst().taskName());
            Assertions.assertEquals(new Pos(11.5, 40, 60), player.getPosition(), "the player must not move");
        }
    }

    @DisplayName("A middle click on a server does nothing: it connects nobody, moves nobody and keeps the navigator open")
    @Test
    void middleClickOnServerDoesNothing(Env env) {
        assertUnnamedClickDoesNothing(env, new Click.Middle(SURVIVAL_SLOT));
    }

    @DisplayName("A number-key swap on a server does nothing")
    @Test
    void hotbarSwapOnServerDoesNothing(Env env) {
        assertUnnamedClickDoesNothing(env, new Click.HotbarSwap(0, SURVIVAL_SLOT));
    }

    @DisplayName("A Q drop on a server does nothing")
    @Test
    void dropSlotOnServerDoesNothing(Env env) {
        assertUnnamedClickDoesNothing(env, new Click.DropSlot(SURVIVAL_SLOT, false));
    }

    @DisplayName("A double click on a server does nothing")
    @Test
    void doubleClickOnServerDoesNothing(Env env) {
        assertUnnamedClickDoesNothing(env, new Click.Double(SURVIVAL_SLOT));
    }

    @DisplayName("An F swap on a server does nothing")
    @Test
    void offhandSwapOnServerDoesNothing(Env env) {
        assertUnnamedClickDoesNothing(env, new Click.OffhandSwap(SURVIVAL_SLOT));
    }

    // The click is cancelled, and no unnamed click type connects, teleports or closes the navigator.
    private static void assertUnnamedClickDoesNothing(Env env, Click unnamed) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(11.5, 40, 60));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), SURVIVAL_PORTAL)) {
            AbstractInventory inventory = open(fixture, player);

            InventoryPreClickEvent event = click(env, inventory, player, unnamed);
            env.tick();

            Assertions.assertTrue(event.isCancelled(), "the click must be cancelled so no icon moves");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "an unnamed click must not connect");
            Assertions.assertEquals(new Pos(11.5, 40, 60), player.getPosition(), "an unnamed click must not move the player");
            Assertions.assertSame(inventory, player.getOpenInventory(), "an unnamed click must leave the navigator open");
        }
    }

    @DisplayName("A right click on a destination the player may not use neither teleports nor connects")
    @Test
    void deniedRightClickDoesNothing(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, new Pos(11.5, 40, 60));
        FakePermissionService permissions = new FakePermissionService().allowEveryone("titan.navigator.slender").set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        LobbyPortals buildPortal = () -> List.of(new Portal("build", AREA, "Team-Build", BUILD_PERMISSION));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, permissions, buildPortal)) {
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
        FakePermissionService permissions = new FakePermissionService().allowEveryone("titan.navigator.slender").set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        LobbyPortals buildPortal = () -> List.of(new Portal("build", AREA, "Team-Build", BUILD_PERMISSION));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, permissions, buildPortal)) {
            AbstractInventory inventory = open(fixture, player);
            permissions.set(player.getUuid(), BUILD_PERMISSION, PermissionResult.DENIED);

            click(env, inventory, player, new Click.Left(BUILD_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a refused destination must not connect");
            Assertions.assertEquals(new Pos(11.5, 40, 60), player.getPosition(), "a refused destination must not teleport");
        }
    }
}
