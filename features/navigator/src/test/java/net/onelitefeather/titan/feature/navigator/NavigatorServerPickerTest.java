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
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalApproach;
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * End-to-end coverage of the minigame clicks: a left click quick joins the best ready service, a
 * right click opens the server list, and a shift click walks to the portal. A click on one listed
 * server connects to exactly that service.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorServerPickerTest {

    private static final String SLENDER_PERMISSION = "titan.navigator.slender";
    private static final int ELYTRA_RACE_SLOT = Destination.ELYTRA_RACE.slot();
    private static final int SURVIVAL_SLOT = Destination.SURVIVAL.slot();
    private static final int SLENDER_SLOT = Destination.SLENDER.slot();
    private static final Pos SPAWN = new Pos(11.5, 40, 60);
    private static final Box AREA = new Box(new Vec(10, 40, 20), new Vec(12, 42, 22));
    private static final LobbyPortals RACE_PORTAL = () -> List.of(new Portal("race", AREA, "ElytraRace", null));
    private static final LobbyPortals NO_PORTALS = List::of;
    private static final ServiceCount RACE_ONE = new ServiceCount("ElytraRace-1", 3, 50);
    private static final ServiceCount RACE_TWO = new ServiceCount("ElytraRace-2", 10, 50);

    private static AbstractInventory openNavigator(NavigatorFixture fixture, Player player) {
        fixture.equip(player);
        fixture.useFeather(player);
        return player.getOpenInventory();
    }

    private static InventoryPreClickEvent click(Env env, AbstractInventory inventory, Player player, Click click) {
        InventoryPreClickEvent event = new InventoryPreClickEvent(inventory, player, click);
        env.process().eventHandler().call(event);
        return event;
    }

    private static String nameOf(AbstractInventory inventory, int slot) {
        ItemStack stack = inventory.getItemStack(slot);
        MiniMessage mini = MiniMessage.miniMessage();
        return mini.stripTags(mini.serialize(stack.get(DataComponents.CUSTOM_NAME)));
    }

    private static NavigatorFixture start(Env env, RecordingDeliver deliver, FakePlayerCounts counts, LobbyPortals portals) {
        return NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone(SLENDER_PERMISSION), counts, portals);
    }

    @DisplayName("A right click on a minigame opens the list of its services, sorted by name, and connects nobody")
    @Test
    void rightClickOpensThePickerSortedByName(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_TWO, RACE_ONE);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Right(ELYTRA_RACE_SLOT));
            env.tick();

            AbstractInventory picker = player.getOpenInventory();
            Assertions.assertNotSame(navigator, picker, "the picker replaces the navigator");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "opening the picker must not connect");
            Assertions.assertEquals("ElytraRace-1", nameOf(picker, 0), "the first entry is the service that sorts first");
            Assertions.assertEquals("ElytraRace-2", nameOf(picker, 1), "the second entry follows by name");
        }
    }

    @DisplayName("A right click with one running service still opens the picker, listing that service")
    @Test
    void rightClickWithOneServiceOpensThePicker(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Right(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "the picker opens instead of connecting");
            Assertions.assertEquals("ElytraRace-1", nameOf(player.getOpenInventory(), 0), "the picker lists the one service");
        }
    }

    @DisplayName("A click on a listed service connects the player to exactly that service")
    @Test
    void clickingAServerDeliversToThatService(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE, RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            openNavigator(fixture, player);
            click(env, player.getOpenInventory(), player, new Click.Right(ELYTRA_RACE_SLOT));
            env.tick();
            AbstractInventory picker = player.getOpenInventory();

            click(env, picker, player, new Click.Left(1));
            env.tick();

            Assertions.assertEquals(1, deliver.deliveries().size(), "exactly one delivery");
            Assertions.assertEquals("ElytraRace-2", deliver.deliveries().getFirst().serverName(), "the player goes to the clicked service");
            Assertions.assertNull(deliver.deliveries().getFirst().taskName(), "a service is addressed by its name, not the task");
            Assertions.assertNull(player.getOpenInventory(), "the picker closes once the player is sent");
        }
    }

    @DisplayName("A left click on a minigame quick joins the most populated ready service")
    @Test
    void leftClickQuickJoinsTheMostPopulatedService(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE, RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertEquals(1, deliver.deliveries().size(), "exactly one delivery");
            Assertions.assertEquals("ElytraRace-2", deliver.deliveries().getFirst().serverName(), "the round with the most players is filled first");
            Assertions.assertNull(player.getOpenInventory(), "the navigator closes once the player is sent");
        }
    }

    @DisplayName("A left click without a joinable service opens the picker, which shows why")
    @Test
    void leftClickWithoutJoinableServiceOpensThePicker(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", new ServiceCount("ElytraRace-1", 50, 50), new ServiceCount("ElytraRace-2", 4, 0));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "no ready service means nobody is sent");
            Assertions.assertEquals("ElytraRace-1", nameOf(player.getOpenInventory(), 0), "the picker lists the full and the not ready services");
        }
    }

    @DisplayName("A left click with no running service opens the picker, which is empty")
    @Test
    void leftClickWithoutServiceOpensThePicker(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, new FakePlayerCounts(), NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "nothing running means nobody is sent");
            Assertions.assertNotSame(navigator, player.getOpenInventory(), "the picker replaces the navigator");
        }
    }

    @DisplayName("A failing service read opens the picker instead of sending the player anywhere")
    @Test
    void failedReadOpensThePicker(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.failWith(new IllegalStateException("bridge gone"));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "an unknown service list is not a joinable one");
            Assertions.assertNotSame(navigator, player.getOpenInventory(), "the picker opens instead");
        }
    }

    @DisplayName("A shift click on a minigame walks to its portal and connects nobody")
    @Test
    void shiftClickStillTeleports(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE, RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, RACE_PORTAL)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.LeftShift(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a teleport connects nobody");
            Assertions.assertEquals(PortalApproach.standingBefore(RACE_PORTAL.portals().getFirst(), SPAWN), player.getPosition(), "the spot in front of the portal");
            Assertions.assertNull(player.getOpenInventory(), "neither the navigator nor a picker stays open");
        }
    }

    @DisplayName("A left click on a server connects directly to the task, even when it runs on several services")
    @Test
    void survivalConnectsDirectly(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("Survival", RACE_ONE, RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(SURVIVAL_SLOT));
            env.tick();

            Assertions.assertEquals("Survival", deliver.deliveries().getFirst().taskName(), "a server never opens the picker");
        }
    }

    @DisplayName("A right click on a server connects too, with no picker")
    @Test
    void rightClickOnServerConnects(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, new FakePlayerCounts(), NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Right(SURVIVAL_SLOT));
            env.tick();

            Assertions.assertEquals("Survival", deliver.deliveries().getFirst().taskName(), "a plain right click on a server has no meaning, so it connects");
        }
    }

    @DisplayName("A left click on Slender quick joins the best ready Slender service")
    @Test
    void slenderQuickJoins(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("Slender", new ServiceCount("Slender-1", 2, 10), new ServiceCount("Slender-2", 6, 10));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(SLENDER_SLOT));
            env.tick();

            Assertions.assertEquals("Slender-2", deliver.deliveries().getFirst().serverName(), "Slender uses the same quick join as ElytraRace");
        }
    }

    @DisplayName("A click on a full server does not connect and keeps the picker open")
    @Test
    void fullServerIsNotJoined(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", new ServiceCount("ElytraRace-1", 50, 50), RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            openNavigator(fixture, player);
            click(env, player.getOpenInventory(), player, new Click.Right(ELYTRA_RACE_SLOT));
            env.tick();
            AbstractInventory picker = player.getOpenInventory();

            click(env, picker, player, new Click.Left(0));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a full server must not be joined");
            Assertions.assertSame(picker, player.getOpenInventory(), "the picker stays open to pick another server");
        }
    }

    @DisplayName("A click on a server that has not announced a limit does not connect")
    @Test
    void notReadyServerIsNotJoined(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", new ServiceCount("ElytraRace-1", 0, 0), RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            openNavigator(fixture, player);
            click(env, player.getOpenInventory(), player, new Click.Right(ELYTRA_RACE_SLOT));
            env.tick();

            click(env, player.getOpenInventory(), player, new Click.Left(0));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a server that is not ready must not be joined");
        }
    }

    @DisplayName("A second left click before the first one is decided is ignored")
    @Test
    void secondClickWhilePendingIsIgnored(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertEquals(1, deliver.deliveries().size(), "the second click must not send the player again");
        }
    }
}
