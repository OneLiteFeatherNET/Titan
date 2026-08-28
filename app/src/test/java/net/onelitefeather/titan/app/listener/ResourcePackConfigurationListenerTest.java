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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class ResourcePackConfigurationListenerTest {

    private static final String HASH = "4444444444444444444444444444444444444444";
    private static final ResourcePackDefinition REQUIRED_PACK = new ResourcePackDefinition(UUID.fromString("00000000-0000-4000-8000-0000000000aa"), "https://packs.invalid/pack-" + HASH + ".zip", HASH, true, null);

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
            assertTrue(service.registry().holds(player.getUuid(), PackSlot.BASE, REQUIRED_PACK.id()));
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
