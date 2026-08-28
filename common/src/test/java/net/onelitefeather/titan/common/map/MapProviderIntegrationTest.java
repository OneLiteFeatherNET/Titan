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
package net.onelitefeather.titan.common.map;

import net.minestom.server.instance.ChunkLoader;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.falco.anvil.FalcoAnvilLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class MapProviderIntegrationTest {

    private static List<MapEntry> allDirectories(Stream<Path> paths) {
        return paths.map(MapEntry::new).collect(Collectors.toList());
    }

    @AfterEach
    void clearProperty() {
        System.clearProperty(MapPool.LOBBY_MAP_PROPERTY);
    }

    private static MapProvider provider(Env env, Path root, String world) throws IOException {
        // A world in the layout the lobby worlds actually use: one directory per season below
        // worlds/, with the region files directly inside it (US-1.06).
        Files.createDirectories(root.resolve("worlds").resolve(world).resolve("region"));
        Instance instance = env.createEmptyInstance();
        return MapProvider.create(root, (InstanceContainer) instance, MapProviderIntegrationTest::allDirectories);
    }

    @Test
    @DisplayName("The provider serves the world through Falco's Anvil loader (US-1.01)")
    void testTheProviderInstallsTheFalcoLoader(Env env, @TempDir Path root) throws IOException {
        try (MapProvider provider = provider(env, root, "world")) {
            InstanceContainer instance = provider.getInstance();

            FalcoAnvilLoader loader = assertInstanceOf(FalcoAnvilLoader.class, instance.getChunkLoader());
            // The loader is handed the world root and resolves the region directory itself. A world
            // without a dimensions/ directory keeps the older layout, which is the one the lobby
            // worlds are in.
            assertEquals(root.resolve("worlds").resolve("world").resolve("region"), loader.regionDirectory(), "the loader resolves the region directory below the world root");
            assertTrue(loader.legacyLayout(), "a world without a dimensions directory keeps the older layout");
        }
    }

    // What the lobby does about light is asserted in MapProviderLightingTest, by loading chunks out
    // of a region file and reading light levels back. The test that used to stand here asked the
    // chunk supplier for a chunk and checked its type, which every one of the light defects passed.

    @Test
    @DisplayName("Closing the provider closes the chunk loader")
    void testClosingTheProviderClosesTheLoader(Env env, @TempDir Path root) throws IOException {
        MapProvider provider = provider(env, root, "world");
        FalcoAnvilLoader loader = (FalcoAnvilLoader) provider.getInstance().getChunkLoader();

        provider.close();

        assertThrows(IllegalStateException.class, () -> loader.loadChunk(provider.getInstance(), 0, 0), "a closed loader refuses further work instead of reporting the chunk as absent");
    }

    @Test
    @DisplayName("Closing the provider takes the closed loader off the instance")
    void testClosingTheProviderUnwiresTheLoader(Env env, @TempDir Path root) throws IOException {
        MapProvider provider = provider(env, root, "world");
        InstanceContainer instance = provider.getInstance();
        ChunkLoader closed = instance.getChunkLoader();

        provider.close();

        assertNotSame(closed, instance.getChunkLoader(), "the closed loader is not the loader of the instance any more");
        // A player who is still walking around while the server shuts down must not fall over the
        // IllegalStateException of a loader that was closed underneath the instance.
        assertDoesNotThrow(() -> instance.loadChunk(0, 0).join(), "a chunk request after the shutdown is answered rather than thrown at");
    }

    @Test
    @DisplayName("A closed provider refuses to save instead of reopening the world")
    void testAClosedProviderRefusesToSave(Env env, @TempDir Path root) throws IOException {
        MapProvider provider = provider(env, root, "world");
        ChunkLoader closed = provider.getInstance().getChunkLoader();

        provider.close();

        // Saving reads the map data back, which used to reinstall a chunk loader: the same-root
        // check compares against a field that close() had just nulled, so it built a fresh loader
        // and reopened the region files the shutdown had closed.
        assertThrows(IllegalStateException.class, () -> provider.saveMap(LobbyMap.lobbyMapBuilder().name("late").build()));
        assertNotSame(closed, provider.getInstance().getChunkLoader(), "no new loader was installed");
        assertSame(ChunkLoader.noop(), provider.getInstance().getChunkLoader(), "the instance keeps the no-op loader the shutdown left it with");
    }

    @Test
    @DisplayName("A closed provider stops asking for light")
    void testAClosedProviderStopsSchedulingLight(Env env, @TempDir Path root) throws IOException {
        MapProvider provider = provider(env, root, "world");
        provider.close();

        provider.getInstance().loadChunk(0, 0).join();

        assertFalse(provider.isLightPending(0, 0), "the chunk load listener is gone with the provider");
    }

    @Test
    @DisplayName("Closing twice is harmless")
    void testClosingTwiceIsHarmless(Env env, @TempDir Path root) throws IOException {
        MapProvider provider = provider(env, root, "world");

        provider.close();

        assertDoesNotThrow(provider::close);
        assertTrue(provider.isClosed());
    }
}
