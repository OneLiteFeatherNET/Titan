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
import java.util.List;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
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
    // The sort item of a list that fits one row: the middle slot of its navigation row.
    private static final int SORT_SLOT = 13;
    private static final int NEXT_SLOT = 53;
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
            Assertions.assertEquals("ElytraRace-1", nameOf(picker, 3), "the first entry is the service that sorts first");
            Assertions.assertEquals("ElytraRace-2", nameOf(picker, 4), "the second entry follows by name");
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
            Assertions.assertEquals("ElytraRace-1", nameOf(player.getOpenInventory(), 4), "the picker lists the one service");
        }
    }

    @DisplayName("A click on the centred entry of a single running service connects to that service")
    @Test
    void clickingTheCentredSingleEntryDeliversToIt(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            openNavigator(fixture, player);
            click(env, player.getOpenInventory(), player, new Click.Right(ELYTRA_RACE_SLOT));
            env.tick();
            AbstractInventory picker = player.getOpenInventory();

            click(env, picker, player, new Click.Left(4));
            env.tick();

            Assertions.assertEquals(1, deliver.deliveries().size(), "exactly one delivery");
            Assertions.assertEquals("ElytraRace-1", deliver.deliveries().getFirst().serverName(), "the single entry sits in column four and connects to its service");
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

            click(env, picker, player, new Click.Left(4));
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
            Assertions.assertEquals("ElytraRace-1", nameOf(player.getOpenInventory(), 3), "the picker lists the full and the not ready services");
        }
    }

    @DisplayName("Without a player-count provider a left click refuses in chat instead of listing nothing")
    @Test
    void withoutProviderQuickJoinRefusesInChat(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone(SLENDER_PERMISSION))) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "nothing can be joined without a provider");
            Assertions.assertSame(navigator, player.getOpenInventory(), "no picker opens when the services are unknowable");
            Assertions.assertEquals(List.of("Servers » The server list is unavailable right now."), chatTexts(chat), "the player is told why nothing happened");
        }
    }

    @DisplayName("A left click with no running service opens the picker, which says that nothing runs")
    @Test
    void leftClickWithoutServiceOpensAPickerThatExplainsItself(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, new FakePlayerCounts(), NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            env.tick();

            AbstractInventory picker = player.getOpenInventory();
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "nothing running means nobody is sent");
            Assertions.assertNotSame(navigator, picker, "the picker replaces the navigator");
            Assertions.assertEquals("No server is running right now.", nameOf(picker, 4), "the empty picker names the reason in its middle slot");
        }
    }

    @DisplayName("A failing service read refuses in chat and opens no picker")
    @Test
    void failedReadRefusesInChatAndOpensNoPicker(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.failWith(new IllegalStateException("bridge gone"));
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "an unknown service list is not a joinable one");
            Assertions.assertSame(navigator, player.getOpenInventory(), "no picker opens when the services could not be read");
            Assertions.assertEquals(List.of("Servers » The server list is unavailable right now."), chatTexts(chat), "the player is told why nothing happened");
        }
    }

    @DisplayName("A right click with a failing service read refuses in chat and opens no picker")
    @Test
    void failedReadOnRightClickRefusesInChat(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.failWith(new IllegalStateException("bridge gone"));
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Right(ELYTRA_RACE_SLOT));
            env.tick();

            Assertions.assertSame(navigator, player.getOpenInventory(), "no picker opens when the services could not be read");
            Assertions.assertEquals(1, chat.collect().size(), "the refusal is sent once");
        }
    }

    @DisplayName("A click on a picked server re-reads the services and refuses one that has filled up since the picker opened")
    @Test
    void pickRefusesAServerThatFilledUpAfterOpening(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE, RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);
            click(env, navigator, player, new Click.Right(ELYTRA_RACE_SLOT));
            env.tick();
            AbstractInventory picker = player.getOpenInventory();
            counts.serveServices("ElytraRace", new ServiceCount("ElytraRace-1", 50, 50), RACE_TWO);

            click(env, picker, player, new Click.Left(3));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a server that filled up since the picker opened must not be joined");
            Assertions.assertSame(picker, player.getOpenInventory(), "the picker stays open after the refusal");
        }
    }

    @DisplayName("A click on a picked server that has stopped running refuses instead of joining it")
    @Test
    void pickRefusesAServerThatStoppedSinceOpening(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE, RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);
            click(env, navigator, player, new Click.Right(ELYTRA_RACE_SLOT));
            env.tick();
            AbstractInventory picker = player.getOpenInventory();
            counts.serveServices("ElytraRace", RACE_TWO);

            click(env, picker, player, new Click.Left(3));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a server that is gone must not be joined");
        }
    }

    @DisplayName("A click on a picked server still joins it when it is still ready and has room")
    @Test
    void pickJoinsAServerThatIsStillReady(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE, RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);
            click(env, navigator, player, new Click.Right(ELYTRA_RACE_SLOT));
            env.tick();
            AbstractInventory picker = player.getOpenInventory();

            click(env, picker, player, new Click.Left(4));
            env.tick();

            Assertions.assertEquals("ElytraRace-2", deliver.deliveries().getFirst().serverName(), "the picked server is joined when it still qualifies");
        }
    }

    @DisplayName("A middle click on a picked server does nothing: the picker stays open and nobody is sent")
    @Test
    void middleClickOnPickedServerDoesNothing(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);
            click(env, navigator, player, new Click.Right(ELYTRA_RACE_SLOT));
            env.tick();
            AbstractInventory picker = player.getOpenInventory();

            InventoryPreClickEvent event = click(env, picker, player, new Click.Middle(4));
            env.tick();

            Assertions.assertTrue(event.isCancelled(), "the click must be cancelled so no icon moves");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "an unnamed click must not join a server");
            Assertions.assertSame(picker, player.getOpenInventory(), "an unnamed click must leave the picker open");
        }
    }

    @DisplayName("A middle click on a minigame does nothing: it neither joins nor opens the picker")
    @Test
    void middleClickOnMinigameDoesNothing(Env env) {
        assertUnnamedClickOnMinigameDoesNothing(env, new Click.Middle(ELYTRA_RACE_SLOT));
    }

    @DisplayName("A number-key swap on a minigame does nothing: it neither joins nor opens the picker")
    @Test
    void hotbarSwapOnMinigameDoesNothing(Env env) {
        assertUnnamedClickOnMinigameDoesNothing(env, new Click.HotbarSwap(0, ELYTRA_RACE_SLOT));
    }

    @DisplayName("A Q drop on a minigame does nothing: it neither joins nor opens the picker")
    @Test
    void dropSlotOnMinigameDoesNothing(Env env) {
        assertUnnamedClickOnMinigameDoesNothing(env, new Click.DropSlot(ELYTRA_RACE_SLOT, false));
    }

    @DisplayName("A double click on a minigame does nothing: it neither joins nor opens the picker")
    @Test
    void doubleClickOnMinigameDoesNothing(Env env) {
        assertUnnamedClickOnMinigameDoesNothing(env, new Click.Double(ELYTRA_RACE_SLOT));
    }

    // Only the left and right clicks, with or without shift, are named: any other click is cancelled and
    // does nothing, so the navigator stays open and nobody is sent.
    private static void assertUnnamedClickOnMinigameDoesNothing(Env env, Click unnamed) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            InventoryPreClickEvent event = click(env, navigator, player, unnamed);
            env.tick();

            Assertions.assertTrue(event.isCancelled(), "the click must be cancelled so no icon moves");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "an unnamed click must not join a server");
            Assertions.assertSame(navigator, player.getOpenInventory(), "an unnamed click must not open the picker");
        }
    }

    private static List<String> chatTexts(Collector<SystemChatPacket> chat) {
        MiniMessage mini = MiniMessage.miniMessage();
        return chat.collect().stream().map(packet -> mini.stripTags(mini.serialize(packet.message()))).toList();
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

            click(env, picker, player, new Click.Left(3));
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

            click(env, player.getOpenInventory(), player, new Click.Left(3));
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

    @DisplayName("A click on the sort item reads the services once and lists the next order from the top")
    @Test
    void sortClickReadsOnceAndListsTheNextOrder(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE, RACE_TWO);
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory picker = PickerTestSupport.openPicker(env, fixture, player);
            int readsBefore = counts.reads();

            PickerTestSupport.clickAndTick(env, picker, player, new Click.Left(SORT_SLOT));

            AbstractInventory sorted = player.getOpenInventory();
            Assertions.assertEquals(readsBefore + 1, counts.reads(), "the sort click reads the services exactly once");
            Assertions.assertEquals("ElytraRace-2", nameOf(sorted, 3), "most players comes first: ElytraRace-2 has 10 against 3");
            Assertions.assertEquals("▸ Most players", PickerTestSupport.loreOf(sorted, SORT_SLOT).get(1), "the second order is the active one");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "sorting connects nobody");
        }
    }

    @DisplayName("A click on an entry of the second page connects to exactly that service")
    @Test
    void clickingAnEntryOnTheSecondPageConnectsToIt(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", PickerTestSupport.services(100));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = start(env, deliver, counts, NO_PORTALS)) {
            AbstractInventory picker = PickerTestSupport.openPicker(env, fixture, player);
            PickerTestSupport.clickAndTick(env, picker, player, new Click.Left(NEXT_SLOT));

            PickerTestSupport.clickAndTick(env, player.getOpenInventory(), player, new Click.Left(1));

            Assertions.assertEquals(1, deliver.deliveries().size(), "exactly one delivery");
            Assertions.assertEquals("ElytraRace-046", deliver.deliveries().getFirst().serverName(), "the second entry of the second page is the 47th service");
        }
    }

    @DisplayName("A sort click whose read completes after the player closed the list does not reopen it")
    @Test
    void sortReadCompletingAfterCloseLeavesThePlayerWithNoInventory(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE, RACE_TWO);
        HeldReads reads = new HeldReads();
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.startReadingOn(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), counts, reads)) {
            AbstractInventory navigator = openNavigator(fixture, player);
            click(env, navigator, player, new Click.Right(ELYTRA_RACE_SLOT));
            reads.completeAll();
            env.tick();
            AbstractInventory picker = player.getOpenInventory();
            Assertions.assertNotNull(picker, "the right click must open the list before the sort click, or the check proves nothing");

            click(env, picker, player, new Click.Left(SORT_SLOT));
            player.closeInventory();
            reads.completeAll();
            env.tick();

            Assertions.assertNull(player.getOpenInventory(), "a re-read that completes after the player closed the list must not open it again");
        }
    }

    @DisplayName("A right click whose read completes after the player closed the navigator opens no list")
    @Test
    void rightClickReadCompletingAfterCloseOpensNoList(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE, RACE_TWO);
        HeldReads reads = new HeldReads();
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.startReadingOn(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), counts, reads)) {
            AbstractInventory navigator = openNavigator(fixture, player);
            click(env, navigator, player, new Click.Right(ELYTRA_RACE_SLOT));
            player.closeInventory();
            reads.completeAll();
            env.tick();

            Assertions.assertNull(player.getOpenInventory(), "a list whose read completes after the player closed the navigator must not open");
        }
    }

    /** Holds every read until the test completes it, so a test decides when a read finishes. */
    private static final class HeldReads implements Executor {

        private final List<Runnable> held = new ArrayList<>();

        @Override
        public void execute(Runnable read) {
            this.held.add(read);
        }

        void completeAll() {
            List<Runnable> reads = List.copyOf(this.held);
            this.held.clear();
            reads.forEach(Runnable::run);
        }
    }
}
