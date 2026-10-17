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
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.component.DataComponents;
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
 * End-to-end coverage of the server picker: a left click on a destination with several running
 * services opens a list of them, and a click on one entry connects to exactly that service.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorServerPickerTest {

    private static final int ELYTRA_RACE_SLOT = 0;
    private static final int SURVIVAL_SLOT = 4;
    private static final int SLENDER_SLOT = 5;
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

    @DisplayName("A left click on a mode with several services opens the list of those services, sorted by name")
    @Test
    void leftClickWithTwoServicesOpensThePicker(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_TWO, RACE_ONE);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            env.tick();

            AbstractInventory picker = player.getOpenInventory();
            Assertions.assertNotSame(navigator, picker, "the picker replaces the navigator");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "opening the picker must not connect");
            Assertions.assertEquals("ElytraRace-1", nameOf(picker, 0), "the first entry is the service that sorts first");
            Assertions.assertEquals("ElytraRace-2", nameOf(picker, 1), "the second entry follows by name");
        }
    }

    @DisplayName("A click on a listed service connects the player to exactly that service")
    @Test
    void clickingAServerDeliversToThatService(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE, RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), counts, NO_PORTALS)) {
            openNavigator(fixture, player);
            click(env, player.getOpenInventory(), player, new Click.Left(ELYTRA_RACE_SLOT));
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

    @DisplayName("A mode with exactly one running service connects directly to the task")
    @Test
    void oneServiceConnectsDirectly(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertEquals(1, deliver.deliveries().size(), "exactly one delivery");
            Assertions.assertEquals("ElytraRace", deliver.deliveries().getFirst().taskName(), "one service connects to the task as today");
        }
    }

    @DisplayName("A mode with no running service connects directly to the task")
    @Test
    void noServiceConnectsDirectly(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), new FakePlayerCounts(), NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertEquals("ElytraRace", deliver.deliveries().getFirst().taskName(), "no service known connects to the task");
        }
    }

    @DisplayName("A failing service read connects directly to the task instead of opening an empty picker")
    @Test
    void failedReadConnectsDirectly(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.failWith(new IllegalStateException("bridge gone"));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertEquals("ElytraRace", deliver.deliveries().getFirst().taskName(), "an unknown count connects directly");
        }
    }

    @DisplayName("A right click still teleports to the portal even when the mode runs on several services")
    @Test
    void rightClickStillTeleports(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE, RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), counts, RACE_PORTAL)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Right(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a teleport connects nobody");
            Assertions.assertNotEquals(SPAWN, player.getPosition(), "the player walks to the portal");
            Assertions.assertEquals(PortalApproach.standingBefore(RACE_PORTAL.portals().getFirst(), SPAWN), player.getPosition(), "the spot in front of the portal");
            Assertions.assertNull(player.getOpenInventory(), "neither the navigator nor a picker stays open");
        }
    }

    @DisplayName("A mode without a picker connects directly even when it runs on several services")
    @Test
    void survivalConnectsDirectly(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("Survival", RACE_ONE, RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(SURVIVAL_SLOT));
            env.tick();

            Assertions.assertEquals("Survival", deliver.deliveries().getFirst().taskName(), "Survival stays direct");
        }
    }

    @DisplayName("Slender offers the picker as well")
    @Test
    void slenderOpensThePicker(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("Slender", RACE_ONE, RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(SLENDER_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "Slender opens the picker first");
            Assertions.assertEquals("ElytraRace-1", nameOf(player.getOpenInventory(), 0), "the picker lists the Slender services");
        }
    }

    @DisplayName("A click on a full server does not connect and keeps the picker open")
    @Test
    void fullServerIsNotJoined(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", new ServiceCount("ElytraRace-1", 50, 50), RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), counts, NO_PORTALS)) {
            openNavigator(fixture, player);
            click(env, player.getOpenInventory(), player, new Click.Left(ELYTRA_RACE_SLOT));
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
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), counts, NO_PORTALS)) {
            openNavigator(fixture, player);
            click(env, player.getOpenInventory(), player, new Click.Left(ELYTRA_RACE_SLOT));
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
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone("titan.navigator.slender"), counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertEquals(1, deliver.deliveries().size(), "the second click must not send the player again");
        }
    }
}
