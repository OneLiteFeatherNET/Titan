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

import com.google.gson.Gson;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.instance.InstanceChunkLoadEvent;
import net.minestom.server.instance.Clock;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.onelitefeather.falco.anvil.FalcoAnvilLoader;
import net.onelitefeather.falco.light.ChunkLightService;
import net.onelitefeather.titan.common.config.AppConfig;
import net.theevilreaper.aves.file.GsonFileHandler;
import net.theevilreaper.aves.file.gson.PositionGsonAdapter;
import net.theevilreaper.aves.map.BaseMap;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The {@link MapProvider} class owns the lobby instance: it picks the world the
 * {@link MapPool} selected, reads the map data next to it and wires the engine that serves the
 * chunks of that world.
 * <p>
 * Chunks are read by Falco's {@link FalcoAnvilLoader} instead of Minestom's {@code AnvilLoader}.
 * The difference that matters for a built lobby is what happens when a chunk cannot be read: the
 * Falco loader reports the failure, while the Minestom one reports the chunk as absent, which makes
 * the server generate a fresh chunk and overwrite the built one on the next save.
 * </p>
 * <p>
 * Block light is computed by Falco as well: every chunk that arrives is handed to a
 * {@link ChunkLightService}, which lights it from the chunks around it rather than on its own, so
 * the lobby does not end up with a dark line every sixteen blocks.
 * </p>
 *
 * @author theEvilReaper
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 1.0.0
 */
public final class MapProvider implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapProvider.class);
    private static final String MAP_PATH = "worlds";
    private final GsonFileHandler fileHandler;
    private final MapPool mapPool;
    private final Gson gson;
    private final ChunkLightService lightService;
    private InstanceContainer instance;
    private LobbyMap activeLobby;
    private @Nullable FalcoAnvilLoader chunkLoader;
    private @Nullable Path chunkLoaderRoot;

    private MapProvider(Path path, InstanceContainer instance, Function<Stream<Path>, List<MapEntry>> filterMaps) {
        this.mapPool = new MapPool(path.resolve(MAP_PATH), filterMaps);
        this.instance = instance;
        // Use LightingChunk so the world is actually lit: it computes and sends sky/block light.
        // Plain DynamicChunks send no light, leaving the lobby pitch black. Must be set before any
        // chunk is loaded by the chunk loader.
        this.instance.setChunkSupplier(LightingChunk::new);
        // "Exploration" lighting: light each chunk as it is loaded so regions light up while
        // players explore into new map sections (anvil chunks otherwise stay dark until a block
        // update triggers a relight). Falco computes it (US-1.03); writing the result clears the
        // update flag of the section, so Minestom does not recompute what was just calculated.
        this.lightService = new ChunkLightService();
        this.instance.eventNode().addListener(InstanceChunkLoadEvent.class, this::lightLoadedChunk);
        var typeAdapter = new PositionGsonAdapter();
        this.gson = new Gson().newBuilder().registerTypeAdapter(Pos.class, typeAdapter).registerTypeAdapter(Vec.class, typeAdapter).create();
        this.fileHandler = new GsonFileHandler(this.gson);
        this.loadMapData();
    }

    private MapProvider(Path path, InstanceContainer instance) {
        this(path, instance, MapProvider::defaultFilter);
    }

    /**
     * Lights a chunk that has just been loaded, together with the ring around it.
     * <p>
     * The cached packets of the chunk are dropped afterwards and its neighbourhood is scheduled for
     * a resend, because the chunk may already have been sent by the time this runs: the load event
     * is dispatched after the loading future completes.
     * </p>
     *
     * @param event the event of the chunk that was loaded
     */
    private void lightLoadedChunk(InstanceChunkLoadEvent event) {
        this.lightService.calculateWithNeighbours(event.getInstance(), event.getChunkX(), event.getChunkZ());
        if (event.getChunk() instanceof LightingChunk lightingChunk) {
            lightingChunk.invalidate();
            lightingChunk.invalidateResendDelay();
        }
    }

    private static List<MapEntry> defaultFilter(Stream<Path> pathStream) {
        return pathStream.map(MapEntry::new).filter(MapEntry::hasMapFile).collect(Collectors.toList());
    }

    /**
     * Writes the given map next to the world it belongs to and reads it back.
     *
     * @param baseMap the map to store
     */
    public void saveMap(BaseMap baseMap) {
        this.fileHandler.save(this.mapPool.getMapEntry().path().resolve(AppConfig.MAP_FILE_NAME), baseMap instanceof LobbyMap gameMap ? gameMap : baseMap);
        loadMapData();
    }

    private void loadMapData() {
        var lobbyData = this.fileHandler.load(this.mapPool.getMapEntry().path().resolve(AppConfig.MAP_FILE_NAME), LobbyMap.class);
        // Freeze the lobby at midday so it stays bright; otherwise the default
        // day/night cycle keeps advancing and the world renders dark.
        this.instance.setTime(6000);
        Clock clock = this.instance.defaultClock();
        if (clock != null) {
            clock.rate(0.0f);
        }
        this.installChunkLoader(this.mapPool.getMapEntry().path());
        try {
            this.activeLobby = lobbyData.orElse(LobbyMap.lobbyMapBuilder().build());

            if (this.activeLobby.spawn() != null) {
                loadChunk(this.instance, this.activeLobby.spawn());
            }
        } catch (NoSuchElementException noSuchElementException) {
            LOGGER.error("Failed to load the lobby data");
        }

    }

    /**
     * Points the instance at the world below the given root, unless it already reads from there.
     * <p>
     * The root is the world directory itself and not its {@code region} directory: the Falco loader
     * resolves {@code dimensions/<namespace>/<value>/region} below it and falls back to a plain
     * {@code region} for a world in the older layout.
     * </p>
     * <p>
     * A loader holds open region files, so the one it replaces is closed here. Reusing the loader
     * for an unchanged root matters for {@link #saveMap(BaseMap)}, which reads the map data back
     * and
     * would otherwise drop every open region file on each call.
     * </p>
     *
     * @param worldRoot the root directory of the world to read
     */
    private void installChunkLoader(Path worldRoot) {
        if (worldRoot.equals(this.chunkLoaderRoot)) {
            return;
        }
        this.closeChunkLoader();
        FalcoAnvilLoader loader = new FalcoAnvilLoader(worldRoot, this.instance.getDimensionType().key());
        this.chunkLoader = loader;
        this.chunkLoaderRoot = worldRoot;
        this.instance.setChunkLoader(loader);
    }

    private void closeChunkLoader() {
        FalcoAnvilLoader loader = this.chunkLoader;
        this.chunkLoader = null;
        this.chunkLoaderRoot = null;
        if (loader == null) {
            return;
        }
        try {
            loader.close();
        } catch (IOException exception) {
            LOGGER.error("Unable to close the chunk loader of the lobby world", exception);
        }
    }

    /**
     * Closes the chunk loader and with it every region file the lobby still holds open.
     */
    @Override
    public void close() {
        this.closeChunkLoader();
    }

    private <T extends Point> void loadChunk(InstanceContainer instance, T pos) {
        if (!ChunkUtils.isLoaded(instance, pos)) {
            instance.loadChunk(pos);
        }
    }

    /**
     * Gets the instance the lobby world is served from.
     *
     * @return the lobby instance
     */
    public InstanceContainer getInstance() {
        return instance;
    }

    /**
     * Gets the map data of the world that is currently active.
     *
     * @return the active lobby map
     */
    public LobbyMap getActiveLobby() {
        return activeLobby;
    }

    /**
     * Gets every world the pool found below {@code worlds}.
     *
     * @return an unmodifiable list with all available maps
     */
    public @UnmodifiableView List<MapEntry> getAvailableMaps() {
        return Collections.unmodifiableList(this.mapPool.getAvailableMaps());
    }

    /**
     * Creates a provider that reads its worlds below the given path.
     *
     * @param path     the directory that holds the {@code worlds} directory
     * @param instance the instance the lobby world is served from
     * @return a new provider
     */
    public static MapProvider create(Path path, InstanceContainer instance) {
        return new MapProvider(path, instance);
    }

    /**
     * Creates a provider that reads its worlds below the given path and keeps the entries the given
     * filter accepts.
     *
     * @param path       the directory that holds the {@code worlds} directory
     * @param instance   the instance the lobby world is served from
     * @param filterMaps the filter that decides which directories count as a world
     * @return a new provider
     */
    public static MapProvider create(Path path, InstanceContainer instance, Function<Stream<Path>, List<MapEntry>> filterMaps) {
        return new MapProvider(path, instance, filterMaps);
    }
}
