/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.app.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.common.ResourcePackPushPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.resourcepack.BedrockDetector;
import net.onelitefeather.titan.common.resourcepack.DaemonPackTimeoutScheduler;
import net.onelitefeather.titan.common.resourcepack.HeldPackRegistry;
import net.onelitefeather.titan.common.resourcepack.PackSlot;
import net.onelitefeather.titan.common.resourcepack.ResourcePackDefinition;
import net.onelitefeather.titan.common.resourcepack.ResourcePackService;
import net.onelitefeather.titan.common.resourcepack.ResourcePackSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class ResourcePackConfigurationListenerTest {

    private static final String HASH = "4444444444444444444444444444444444444444";
    private static final String OPTIONAL_HASH = "5555555555555555555555555555555555555555";
    private static final ResourcePackDefinition REQUIRED_PACK = new ResourcePackDefinition(UUID.fromString("00000000-0000-4000-8000-0000000000aa"), "https://packs.invalid/pack-" + HASH + ".zip", HASH, true, null);
    private static final ResourcePackDefinition OPTIONAL_PACK = new ResourcePackDefinition(UUID.fromString("00000000-0000-4000-8000-0000000000bb"), "https://packs.invalid/pack-" + OPTIONAL_HASH + ".zip", OPTIONAL_HASH, false, null);

    @Test
    @DisplayName("A client that never answers does not park the configuration thread forever")
    void testSilentClientDoesNotBlockConfiguration(Env env) {
        // The test connection never answers a pack push, and
        // ConnectionManager.doConfiguration() joins the pack future without a timeout - so
        // without the service's own guard this connect() would never return.
        ResourcePackSettings settings = new ResourcePackSettings(REQUIRED_PACK, null, 250L, false, ".");
        try (DaemonPackTimeoutScheduler scheduler = new DaemonPackTimeoutScheduler()) {
            ResourcePackService service = new ResourcePackService(settings, new HeldPackRegistry(), BedrockDetector.never(), scheduler);
            MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, new ResourcePackConfigurationListener(service));

            Instance instance = env.createFlatInstance();
            TestConnection connection = env.createConnection();
            Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);

            Player player = assertTimeoutPreemptively(Duration.ofSeconds(15), () -> connection.connect(instance));

            List<ResourcePackPushPacket> pushed = pushes.collect();
            assertEquals(1, pushed.size());
            assertTrue(pushed.getFirst().forced());
            // The guard answers on the silent client's behalf, so Minestom clears its own
            // bookkeeping instead of being left with a pack that is pending forever. For this
            // pack that answer also means a kick, because the configuration marks it required.
            assertNull(player.getResourcePackFuture(), "The expired request must not stay in the player's future field");
            assertFalse(service.registry().holds(player.getUuid(), PackSlot.BASE, REQUIRED_PACK.id()), "A pack that timed out is not held");
        }
    }

    @Test
    @DisplayName("An optional pack that times out lets the player in without it")
    void testSilentClientKeepsAnOptionalPack(Env env) {
        ResourcePackSettings settings = new ResourcePackSettings(OPTIONAL_PACK, null, 250L, false, ".");
        try (DaemonPackTimeoutScheduler scheduler = new DaemonPackTimeoutScheduler()) {
            ResourcePackService service = new ResourcePackService(settings, new HeldPackRegistry(), BedrockDetector.never(), scheduler);
            MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, new ResourcePackConfigurationListener(service));

            Instance instance = env.createFlatInstance();
            TestConnection connection = env.createConnection();
            Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);

            Player player = assertTimeoutPreemptively(Duration.ofSeconds(15), () -> connection.connect(instance));

            pushes.assertSingle(packet -> assertFalse(packet.forced()));
            assertTrue(player.isOnline(), "An optional pack that timed out must not cost the player their session");
            assertNull(player.getResourcePackFuture(), "The expired request must not stay in the player's future field");
        }
    }

    @Test
    @DisplayName("With the kick flag active a required pack that times out costs the session")
    void testRequiredPackKickFlagActiveDisconnectsTheSilentClient(Env env) {
        ResourcePackSettings settings = new ResourcePackSettings(REQUIRED_PACK, null, 250L, false, ".");
        try (DaemonPackTimeoutScheduler scheduler = new DaemonPackTimeoutScheduler()) {
            ResourcePackService service = new ResourcePackService(settings, new HeldPackRegistry(), BedrockDetector.never(), scheduler, () -> true);
            MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, new ResourcePackConfigurationListener(service));

            Instance instance = env.createFlatInstance();
            TestConnection connection = env.createConnection();
            Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);

            Player player = assertTimeoutPreemptively(Duration.ofSeconds(15), () -> connection.connect(instance));

            pushes.assertSingle(packet -> assertTrue(packet.forced(), "An enforced pack is pushed with the forced bit set"));
            assertFalse(player.isOnline(), "A silent client must lose its session while the kick flag is active");
        }
    }

    @Test
    @DisplayName("With the kick flag inactive the same pack lets the silent client in, and still leaves no stale state")
    void testRequiredPackKickFlagInactiveAdmitsTheSilentClient(Env env) {
        // The very same REQUIRED_PACK as the test above - only the flag differs.
        ResourcePackSettings settings = new ResourcePackSettings(REQUIRED_PACK, null, 250L, false, ".");
        try (DaemonPackTimeoutScheduler scheduler = new DaemonPackTimeoutScheduler()) {
            ResourcePackService service = new ResourcePackService(settings, new HeldPackRegistry(), BedrockDetector.never(), scheduler, () -> false);
            MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, new ResourcePackConfigurationListener(service));

            Instance instance = env.createFlatInstance();
            TestConnection connection = env.createConnection();
            Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);

            Player player = assertTimeoutPreemptively(Duration.ofSeconds(15), () -> connection.connect(instance));

            // A pack the lobby will not enforce is not advertised as forced either, so the
            // client's accept dialog does not promise something the server does not do.
            pushes.assertSingle(packet -> assertFalse(packet.forced(), "An unenforced pack must not be pushed as forced"));
            assertTrue(player.isOnline(), "With the kick flag inactive a silent client keeps its session");
            // The reason the guard exists in the first place has to survive the flag being off.
            // Minestom nulls the future field only after emptying its own pending map, so a null
            // future here is proof that the pack is gone from that map as well - and that a later
            // configuration pass gets a fresh future instead of an already-completed one.
            assertNull(player.getResourcePackFuture(), "The expired request must not stay in the player's future field");
            assertFalse(service.registry().holds(player.getUuid(), PackSlot.BASE, REQUIRED_PACK.id()), "A pack that timed out is not held");
        }
    }

    @Test
    @DisplayName("A disconnect makes the lobby forget which packs the player held")
    void testDisconnectListenerClearsTheBook(Env env) {
        ResourcePackSettings settings = new ResourcePackSettings(REQUIRED_PACK, null, -1L, false, ".");
        ResourcePackService service = new ResourcePackService(settings, new HeldPackRegistry(), BedrockDetector.never(), (task, delay) -> {
        });

        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        service.onConfiguration(player);
        assertEquals(1, service.registry().trackedPlayers());

        new ResourcePackDisconnectListener(service).accept(new PlayerDisconnectEvent(player));

        assertEquals(0, service.registry().trackedPlayers());
    }
}
