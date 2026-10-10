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
package net.onelitefeather.titan.feature.lobbyswitcher;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Clicking a lobby: the target is checked against a fresh reading, and only a joinable one is
 * delivered. Slots in name order: 0 is Lobby-1, 1 is Lobby-2 (this lobby), 2 is Lobby-3 (full).
 */
@ExtendWith(MicrotusExtension.class)
class SwitcherSelectionTest {

    private static final int LOBBY_1 = 0;
    private static final int LOBBY_2_OWN = 1;
    private static final int LOBBY_3_FULL = 2;

    /** A player with a chat collector that has the list open. */
    private record Viewer(Player player, Collector<SystemChatPacket> chat) {

        String lastMessage() {
            return PlainTextComponentSerializer.plainText().serialize(this.chat.collect().getLast().message());
        }
    }

    private static Viewer viewer(Env env, SwitcherFixture fixture, Locale locale) {
        TestConnection connection = env.createConnection();
        Player player = connection.connect(env.createFlatInstance());
        player.setLocale(locale);
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);
        fixture.use(player);
        fixture.settle();
        return new Viewer(player, chat);
    }

    @DisplayName("Clicking a joinable lobby delivers the player to that service exactly once")
    @Test
    void joinableLobbyIsDeliveredOnce(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = fixture.joinAndOpen();

            fixture.click(player, LOBBY_1);
            env.tick();

            Assertions.assertEquals(List.of(new RecordingDeliver.Delivery(player, "Lobby-1")), fixture.deliver().deliveries(), "one delivery by service name");
        }
    }

    @DisplayName("A click still delivers when the lobby protection cancels every inventory click first")
    @Test
    void clickSurvivesTheProtectionCancelling(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env); FeatureNode protection = FeatureNode.attach(fixture.titanNode(), "protection", 100, Telemetry.noop()).on(InventoryPreClickEvent.class, event -> event.setCancelled(true))) {
            Player player = fixture.joinAndOpen();

            fixture.click(player, LOBBY_1);
            env.tick();

            Assertions.assertEquals(List.of(new RecordingDeliver.Delivery(player, "Lobby-1")), fixture.deliver().deliveries(), "one delivery although protection (priority 100) cancelled the click");
        }
    }

    @DisplayName("Clicking a joinable lobby closes the inventory")
    @Test
    void joinableLobbyClosesTheInventory(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = fixture.joinAndOpen();

            fixture.click(player, LOBBY_1);
            env.tick();

            Assertions.assertNull(player.getOpenInventory(), "the inventory must be closed after sending");
        }
    }

    @DisplayName("A second click before the check has concluded does not deliver twice")
    @Test
    void doubleClickDeliversOnce(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = fixture.joinAndOpen();

            fixture.click(player, LOBBY_1);
            fixture.click(player, LOBBY_1);
            env.tick();

            Assertions.assertEquals(1, fixture.deliver().deliveries().size(), "the second click must be ignored while the first is pending");
        }
    }

    @DisplayName("Clicking the own lobby delivers nobody and says so")
    @Test
    void ownLobbyIsNotDelivered(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Viewer viewer = viewer(env, fixture, Locale.ENGLISH);

            fixture.click(viewer.player(), LOBBY_2_OWN);
            env.tick();

            Assertions.assertTrue(fixture.deliver().deliveries().isEmpty(), "no delivery to the own lobby");
            Assertions.assertTrue(viewer.lastMessage().contains("already"), "the player is told they are already there: " + viewer.lastMessage());
        }
    }

    @DisplayName("Clicking a full lobby delivers nobody and says it is full")
    @Test
    void fullLobbyIsNotDelivered(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Viewer viewer = viewer(env, fixture, Locale.ENGLISH);

            fixture.click(viewer.player(), LOBBY_3_FULL);
            env.tick();

            Assertions.assertTrue(fixture.deliver().deliveries().isEmpty(), "no delivery to a full lobby");
            Assertions.assertTrue(viewer.lastMessage().contains("full"), "the player is told the lobby is full: " + viewer.lastMessage());
        }
    }

    @DisplayName("A lobby that has not announced a limit is not delivered either")
    @Test
    void notReadyLobbyIsNotDelivered(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            fixture.counts().serve(new ServiceCount("Lobby-1", 0, 0), SwitcherFixture.LOBBY_2);
            Viewer viewer = viewer(env, fixture, Locale.ENGLISH);

            fixture.click(viewer.player(), LOBBY_1);
            env.tick();

            Assertions.assertTrue(fixture.deliver().deliveries().isEmpty(), "no delivery to a lobby without a limit");
            Assertions.assertTrue(viewer.lastMessage().contains("not ready"), "the player is told the lobby is not ready: " + viewer.lastMessage());
        }
    }

    @DisplayName("The message after a click is in the player's language")
    @Test
    void messageIsInThePlayersLocale(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Viewer viewer = viewer(env, fixture, Locale.GERMAN);

            fixture.click(viewer.player(), LOBBY_3_FULL);
            env.tick();

            Assertions.assertTrue(viewer.lastMessage().contains("voll"), "German message expected: " + viewer.lastMessage());
        }
    }

    @DisplayName("A lobby that filled up between listing and click is not delivered, and the view shows the new state")
    @Test
    void lobbyFilledUpAtCheckTime(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Viewer viewer = viewer(env, fixture, Locale.ENGLISH);
            fixture.counts().serve(new ServiceCount("Lobby-1", 50, 50), SwitcherFixture.LOBBY_2, SwitcherFixture.LOBBY_3);

            fixture.click(viewer.player(), LOBBY_1);
            env.tick();
            fixture.settle();

            Assertions.assertTrue(fixture.deliver().deliveries().isEmpty(), "no delivery to a lobby that is full now");
            Assertions.assertTrue(viewer.lastMessage().contains("full"), "the player is told the lobby is full: " + viewer.lastMessage());
            Assertions.assertEquals(Material.RED_CONCRETE, viewer.player().getOpenInventory().getItemStack(LOBBY_1).material(), "the view must show Lobby-1 as full now");
        }
    }

    @DisplayName("A lobby that stopped running between listing and click is not delivered and reads as gone")
    @Test
    void lobbyGoneAtCheckTime(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Viewer viewer = viewer(env, fixture, Locale.ENGLISH);
            fixture.counts().serve(SwitcherFixture.LOBBY_2, SwitcherFixture.LOBBY_3);

            fixture.click(viewer.player(), LOBBY_1);
            env.tick();

            Assertions.assertTrue(fixture.deliver().deliveries().isEmpty(), "no delivery to a lobby that is gone");
            Assertions.assertTrue(viewer.lastMessage().contains("no longer running"), "the player is told the lobby is gone: " + viewer.lastMessage());
        }
    }

    @DisplayName("When the check itself fails nobody is delivered and the player is told lobbies are unavailable")
    @Test
    void failingCheckIsAnError(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Viewer viewer = viewer(env, fixture, Locale.ENGLISH);
            fixture.counts().failWith(new IllegalStateException("provider down"));

            fixture.click(viewer.player(), LOBBY_1);
            env.tick();

            Assertions.assertTrue(fixture.deliver().deliveries().isEmpty(), "a failed check must not deliver");
            Assertions.assertTrue(viewer.lastMessage().contains("not available"), "the player is told lobbies are unavailable: " + viewer.lastMessage());
        }
    }

    @DisplayName("After a refused click the inventory stays open")
    @Test
    void refusedClickKeepsTheInventoryOpen(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = fixture.joinAndOpen();

            fixture.click(player, LOBBY_3_FULL);
            env.tick();

            Assertions.assertNotNull(player.getOpenInventory(), "the player keeps the list to pick another lobby");
        }
    }
}
