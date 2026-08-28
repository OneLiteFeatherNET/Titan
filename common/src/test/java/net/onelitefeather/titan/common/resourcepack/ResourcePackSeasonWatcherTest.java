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
package net.onelitefeather.titan.common.resourcepack;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.common.ResourcePackPopPacket;
import net.minestom.server.network.packet.server.common.ResourcePackPushPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the trigger, not the swap. {@link ResourcePackService#applySeason} is exercised
 * elsewhere; what these tests are about is that something in the running lobby actually reaches
 * it when an operator edits {@code resource-packs.json}.
 */
@ExtendWith(MicrotusExtension.class)
class ResourcePackSeasonWatcherTest {

    private static final String BASE_HASH = "1111111111111111111111111111111111111111";
    private static final String SEASON_HASH = "2222222222222222222222222222222222222222";
    private static final String NEXT_SEASON_HASH = "3333333333333333333333333333333333333333";

    private static final UUID BASE_ID = UUID.fromString("00000000-0000-4000-8000-00000000ba5e");
    private static final UUID SEASON_ID = UUID.fromString("00000000-0000-4000-8000-000000005ea0");
    private static final UUID NEXT_SEASON_ID = UUID.fromString("00000000-0000-4000-8000-000000005ea1");

    private static final Duration POLL = Duration.ofMillis(20);
    private static final Duration PATIENCE = Duration.ofSeconds(10);

    /**
     * Writes a configuration with the base pack and, optionally, a season pack.
     *
     * @param directory  the directory to write into
     * @param seasonId   the season pack identifier, or {@code null} for no season
     * @param seasonHash the season pack hash, ignored when there is no season
     * @throws IOException when the file cannot be written
     */
    private static void writeConfiguration(Path directory, UUID seasonId, String seasonHash) throws IOException {
        String season = seasonId == null ? "" : """
                ,
                  "season": {
                    "id": "%s",
                    "url": "https://packs.example/season-%s.zip",
                    "hash": "%s"
                  }""".formatted(seasonId, seasonHash, seasonHash);
        Files.writeString(directory.resolve(ResourcePackSettingsProvider.FILE_NAME), """
                {
                  "base": {
                    "id": "%s",
                    "url": "https://packs.example/pack-%s.zip",
                    "hash": "%s"
                  }%s,
                  "responseTimeoutMillis": -1
                }
                """.formatted(BASE_ID, BASE_HASH, BASE_HASH, season));
    }

    private static ResourcePackService service(Path directory) {
        return new ResourcePackService(ResourcePackSettingsProvider.load(directory), new HeldPackRegistry(), BedrockDetector.never(), PackTimeoutScheduler.never());
    }

    @Test
    @DisplayName("A season swapped in resource-packs.json reaches a player who is already online")
    void testEditedSeasonReachesOnlinePlayers(Env env, @TempDir Path directory) throws IOException {
        writeConfiguration(directory, SEASON_ID, SEASON_HASH);
        ResourcePackService service = service(directory);
        assertEquals(SEASON_ID, service.settings().season().id());

        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        service.onConfiguration(player);
        assertTrue(service.registry().holds(player.getUuid(), PackSlot.SEASON, SEASON_ID));

        Collector<ResourcePackPopPacket> pops = connection.trackIncoming(ResourcePackPopPacket.class);
        Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);

        // The operator edits the file at midnight. Nobody calls anything else.
        writeConfiguration(directory, NEXT_SEASON_ID, NEXT_SEASON_HASH);

        // Nothing but the timer drives this. A collector cannot be used as the tick condition:
        // Collector#collect() unregisters the tracker, so asking it would throw the packets away.
        try (ResourcePackSeasonWatcher watcher = new ResourcePackSeasonWatcher(directory, service, () -> List.of(player), POLL, env.process().scheduler())) {
            watcher.start();
            assertTrue(env.tickWhile(() -> !service.registry().holds(player.getUuid(), PackSlot.SEASON, NEXT_SEASON_ID), PATIENCE), "The lobby never picked the new season up from the configuration file");
        }

        pops.assertSingle(packet -> assertEquals(SEASON_ID, packet.id(), "Only the season pack is removed"));
        pushes.assertSingle(packet -> assertEquals(NEXT_SEASON_ID, packet.id()));
        assertTrue(service.registry().holds(player.getUuid(), PackSlot.BASE, BASE_ID), "The base pack survives a season change");
        assertEquals(NEXT_SEASON_ID, service.settings().season().id(), "The swap also applies to everyone who joins later");
    }

    @Test
    @DisplayName("An unchanged configuration file swaps nothing, however often it is read")
    void testUnchangedFileDoesNothing(Env env, @TempDir Path directory) throws IOException {
        writeConfiguration(directory, SEASON_ID, SEASON_HASH);
        ResourcePackService service = service(directory);

        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        service.onConfiguration(player);

        Collector<ResourcePackPopPacket> pops = connection.trackIncoming(ResourcePackPopPacket.class);
        Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);

        ResourcePackSeasonWatcher watcher = new ResourcePackSeasonWatcher(directory, service, () -> List.of(player), POLL, env.process().scheduler());
        assertFalse(watcher.poll());
        assertFalse(watcher.poll());

        pops.assertEmpty();
        pushes.assertEmpty();
    }

    @Test
    @DisplayName("A season removed from the file ends the season without touching the base pack")
    void testSeasonEndReachesOnlinePlayers(Env env, @TempDir Path directory) throws IOException {
        writeConfiguration(directory, SEASON_ID, SEASON_HASH);
        ResourcePackService service = service(directory);

        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        service.onConfiguration(player);

        Collector<ResourcePackPopPacket> pops = connection.trackIncoming(ResourcePackPopPacket.class);
        Collector<ResourcePackPushPacket> pushes = connection.trackIncoming(ResourcePackPushPacket.class);

        writeConfiguration(directory, null, null);
        ResourcePackSeasonWatcher watcher = new ResourcePackSeasonWatcher(directory, service, () -> List.of(player), POLL, env.process().scheduler());
        assertTrue(watcher.poll());

        pops.assertSingle(packet -> assertEquals(SEASON_ID, packet.id()));
        pushes.assertEmpty();
        assertTrue(service.registry().holds(player.getUuid(), PackSlot.BASE, BASE_ID));
    }

    @Test
    @DisplayName("A half-written or deleted file does not strip the packs the players already hold")
    void testUnreadableFileKeepsTheCurrentSeason(Env env, @TempDir Path directory) throws IOException {
        writeConfiguration(directory, SEASON_ID, SEASON_HASH);
        ResourcePackService service = service(directory);

        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        service.onConfiguration(player);

        Collector<ResourcePackPopPacket> pops = connection.trackIncoming(ResourcePackPopPacket.class);
        ResourcePackSeasonWatcher watcher = new ResourcePackSeasonWatcher(directory, service, () -> List.of(player), POLL, env.process().scheduler());

        Files.writeString(directory.resolve(ResourcePackSettingsProvider.FILE_NAME), "{ \"base\": ");
        assertFalse(watcher.poll(), "Broken json must not read as an ended season");

        Files.delete(directory.resolve(ResourcePackSettingsProvider.FILE_NAME));
        assertFalse(watcher.poll(), "A missing file must not read as an ended season");

        pops.assertEmpty();
        assertNotNull(service.settings().season());
        assertTrue(service.registry().holds(player.getUuid(), PackSlot.SEASON, SEASON_ID));
    }
}
