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

import net.minestom.server.coordinate.CoordConversion;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.Generator;
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
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Holds the lobby to the light it promises.
 * <p>
 * Every test here reads a real light level out of a chunk the provider loaded from a real region
 * file. That is the point: a chunk which comes from the loader is never handed to
 * {@code Chunk#onGenerate()}, so Minestom does not invalidate its sections on the way in, and a
 * light bug that only shows up on that path is invisible to any test which generates its world.
 * </p>
 */
@ExtendWith(MicrotusExtension.class)
class MapProviderLightingTest {

    /**
     * The largest amount of time a light pass may take before the test gives up on it.
     */
    private static final Duration SETTLE = Duration.ofSeconds(15);

    /**
     * The height of the stone floor every test world carries.
     */
    private static final int FLOOR_TOP = 1;

    /**
     * The block coordinates of the lamp which stands in the chunk east of the first one.
     */
    private static final int LAMP_X = 16;
    private static final int LAMP_Y = 5;
    private static final int LAMP_Z = 8;

    /**
     * The block coordinates of the lamp which stands in the far corner of the western chunk, out of
     * reach of the border.
     */
    private static final int CORNER_LAMP_X = 0;
    private static final int CORNER_LAMP_Z = 0;

    private static List<MapEntry> allDirectories(Stream<Path> paths) {
        return paths.map(MapEntry::new).collect(Collectors.toList());
    }

    @AfterEach
    void clearProperty() {
        System.clearProperty(MapPool.LOBBY_MAP_PROPERTY);
    }

    /**
     * Writes a world to disk and returns the root of it.
     * <p>
     * The chunks are generated in a throwaway instance whose chunks are plain {@code DynamicChunk}s
     * and are then written through a Falco loader. Nothing computes light on the way, so the
     * sections of the region file carry neither a {@code BlockLight} nor a {@code SkyLight} array —
     * which is exactly the state a world built in a world editor arrives in.
     * </p>
     *
     * @param env       the test environment which owns the throwaway instance
     * @param root      the directory which holds the {@code worlds} directory
     * @param world     the name of the world directory
     * @param generator the generator which shapes the written chunks
     * @param chunks    the chunks to write, as pairs of chunk coordinates
     * @return the root directory of the written world
     */
    private static Path writeWorld(Env env, Path root, String world, Generator generator, int[][] chunks) throws IOException {
        Path worldRoot = root.resolve("worlds").resolve(world);
        Files.createDirectories(worldRoot.resolve("region"));

        Instance source = env.createEmptyInstance();
        source.setGenerator(generator);
        List<Chunk> written = new ArrayList<>(chunks.length);
        for (int[] position : chunks) {
            written.add(source.loadChunk(position[0], position[1]).join());
        }
        try (FalcoAnvilLoader writer = new FalcoAnvilLoader(worldRoot, source.getDimensionType().key())) {
            for (Chunk chunk : written) {
                writer.saveChunk(chunk);
            }
        }
        env.destroyInstance(source);
        return worldRoot;
    }

    /**
     * Builds the shape every test world uses: a stone floor and two glowstone blocks.
     * <p>
     * The first lamp sits in the far corner of the western chunk and is what tells a test that the
     * light of that chunk has been written at all. It is far enough from the border — more than
     * fifteen blocks of travel — that it contributes nothing there, so the level at the border is a
     * statement about the second lamp alone, which stands one block east of that border in the
     * chunk next door.
     * </p>
     *
     * @return the generator of the test world
     */
    private static Generator floorWithALampOnEachSideOfTheBorder() {
        return unit -> {
            unit.modifier().fillHeight(0, FLOOR_TOP, Block.STONE);
            int startX = unit.absoluteStart().blockX();
            int startZ = unit.absoluteStart().blockZ();

            if (startX == 0 && startZ == 0) {
                unit.modifier().setBlock(CORNER_LAMP_X, LAMP_Y, CORNER_LAMP_Z, Block.GLOWSTONE);
            }
            if (startX == LAMP_X && startZ == 0) {
                unit.modifier().setBlock(LAMP_X, LAMP_Y, LAMP_Z, Block.GLOWSTONE);
            }
        };
    }

    private static MapProvider provider(Env env, Path root) {
        Instance instance = env.createEmptyInstance();
        return MapProvider.create(root, (InstanceContainer) instance, MapProviderLightingTest::allDirectories);
    }

    private static int blockLightAt(Instance instance, int x, int y, int z) {
        return sectionOf(instance, x, y, z).blockLight().getLevel(x & 15, y & 15, z & 15);
    }

    private static int skyLightAt(Instance instance, int x, int y, int z) {
        return sectionOf(instance, x, y, z).skyLight().getLevel(x & 15, y & 15, z & 15);
    }

    private static net.minestom.server.instance.Section sectionOf(Instance instance, int x, int y, int z) {
        Chunk chunk = instance.getChunk(CoordConversion.globalToChunk(x), CoordConversion.globalToChunk(z));
        assertTrue(chunk != null, "the chunk holding " + x + "/" + y + "/" + z + " is loaded");
        return chunk.getSection(CoordConversion.globalToChunk(y));
    }

    @Test
    @DisplayName("A chunk that was lit before its neighbour arrived is lit again (D1)")
    void testAChunkIsRelitWhenItsNeighbourArrives(Env env, @TempDir Path root) throws IOException {
        writeWorld(env, root, "world", floorWithALampOnEachSideOfTheBorder(), new int[][]{{0, 0}, {1, 0}});

        try (MapProvider provider = provider(env, root)) {
            InstanceContainer instance = provider.getInstance();

            // The western chunk arrives alone, with nothing east of it. Its corner lamp is what
            // makes "the light of this chunk has been written" observable at all.
            instance.loadChunk(0, 0).join();
            assertTrue(env.tickWhile(() -> blockLightAt(instance, CORNER_LAMP_X + 1, LAMP_Y, CORNER_LAMP_Z) == 0, SETTLE), "the chunk is lit from its own lamp");
            assertNull(instance.getChunk(1, 0), "the chunk east of the border is not loaded yet");
            assertEquals(0, blockLightAt(instance, LAMP_X - 1, LAMP_Y, LAMP_Z), "the border is dark while the chunk behind it is missing");

            // The eastern chunk arrives afterwards. Its lamp stands one block across the border, so
            // a level has to appear on the western side — which happens only if the chunk that was
            // already lit is lit a second time.
            instance.loadChunk(1, 0).join();
            env.tickWhile(() -> blockLightAt(instance, LAMP_X - 1, LAMP_Y, LAMP_Z) == 0, SETTLE);

            assertEquals(14, blockLightAt(instance, LAMP_X - 1, LAMP_Y, LAMP_Z), "the lamp of the chunk that arrived later reaches across the border");
        }
    }

    @Test
    @DisplayName("A chunk whose region file carries no sky light still gets some (D3)")
    void testSkyLightIsComputedForAWorldWithout(Env env, @TempDir Path root) throws IOException {
        Path worldRoot = writeWorld(env, root, "world", floorWithALampOnEachSideOfTheBorder(), new int[][]{{0, 0}});

        // First the premise of the test: the world on disk really does carry no sky light, so a
        // lobby which only reads it and never computes any is a lobby in the dark.
        Instance bare = env.createEmptyInstance();
        try (FalcoAnvilLoader reader = new FalcoAnvilLoader(worldRoot, bare.getDimensionType().key())) {
            Chunk raw = reader.loadChunk(bare, 0, 0);
            assertTrue(raw != null, "the written chunk is readable");
            assertEquals(0, raw.getSection(0).skyLight().getLevel(8, 10, 8), "the region file carries no sky light for the section above the floor");
        }
        env.destroyInstance(bare);

        try (MapProvider provider = provider(env, root)) {
            InstanceContainer instance = provider.getInstance();
            instance.loadChunk(0, 0).join();

            assertTrue(env.tickWhile(() -> skyLightAt(instance, 8, 10, 8) == 0, SETTLE), "the lobby computes the sky light the region file does not carry");
            assertEquals(15, skyLightAt(instance, 8, 10, 8), "open sky is fully lit");
            assertEquals(0, skyLightAt(instance, 8, 0, 8), "the block below the floor sees no sky");
        }
    }

    @Test
    @DisplayName("Chunks that arrive together are lit without a seam between them (D2)")
    void testChunksThatArriveTogetherHaveNoSeam(Env env, @TempDir Path root) throws IOException {
        int[][] area = {{0, 0}, {1, 0}, {0, 1}, {1, 1}};
        writeWorld(env, root, "world", floorWithALampOnEachSideOfTheBorder(), area);

        try (MapProvider provider = provider(env, root)) {
            InstanceContainer instance = provider.getInstance();

            // Falco's loader loads in parallel, so this is the normal case rather than a contrived
            // one: four adjacent chunks, four virtual threads, overlapping neighbourhoods.
            List<CompletableFuture<Chunk>> loading = new ArrayList<>(area.length);
            for (int[] position : area) {
                loading.add(instance.loadChunk(position[0], position[1]));
            }
            loading.forEach(CompletableFuture::join);

            env.tickWhile(() -> blockLightAt(instance, LAMP_X - 1, LAMP_Y, LAMP_Z) == 0 || blockLightAt(instance, LAMP_X + 1, LAMP_Y, LAMP_Z) == 0, SETTLE);

            int inside = blockLightAt(instance, LAMP_X + 1, LAMP_Y, LAMP_Z);
            int across = blockLightAt(instance, LAMP_X - 1, LAMP_Y, LAMP_Z);
            assertEquals(14, inside, "the chunk that carries the lamp is lit by it");
            assertEquals(inside, across, "the border is not a seam: both sides of the lamp are equally bright");
        }
    }
}
