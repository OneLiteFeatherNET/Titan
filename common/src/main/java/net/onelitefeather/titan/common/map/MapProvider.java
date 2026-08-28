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
import net.minestom.server.event.EventListener;
import net.minestom.server.event.instance.InstanceChunkLoadEvent;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.ChunkLoader;
import net.minestom.server.instance.Clock;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.event.EventNode;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.onelitefeather.falco.anvil.FalcoAnvilLoader;
import net.onelitefeather.falco.light.ChunkArea;
import net.onelitefeather.falco.light.ChunkLightScheduler;
import net.onelitefeather.falco.light.ChunkLightService;
import net.onelitefeather.titan.common.config.AppConfig;
import net.theevilreaper.aves.file.GsonFileHandler;
import net.theevilreaper.aves.file.gson.PositionGsonAdapter;
import net.theevilreaper.aves.map.BaseMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
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
 * <b>Light is scheduled, not computed on the spot.</b> A chunk that arrives is only <em>marked</em>
 * as needing light; a {@link ChunkLightScheduler} collects every mark of a tick, groups the marked
 * chunks into areas which do not overlap and computes each area off the tick thread. Two properties
 * follow from that, and both of them are the reason this is not done inline:
 * </p>
 * <ul>
 * <li><b>A chunk is lit again when its neighbours arrive.</b> Marking a chunk marks the eight
 * around it as well, so the chunk that loaded first — and was lit with nothing beside it — is
 * corrected as soon as its neighbour shows up. Lighting a chunk inline cannot do that: the write
 * goes through {@code Light#set}, which clears the update flag of the section, so neither Falco
 * nor Minestom would ever look at that chunk again and the border would stay dark for good.</li>
 * <li><b>Two chunks are never lit against each other.</b> The Falco loader loads in parallel, so
 * several adjacent chunks arriving at once is the normal case rather than a corner one, and
 * lighting them from two threads reads chunks another thread is writing. The result of that is a
 * seam, it is never an error, and — again because the update flag is cleared — it is
 * permanent.</li>
 * </ul>
 * <p>
 * The scheduler owns the sky pass too. A section which arrives without a {@code SkyLight} array in
 * its NBT is <em>not</em> recomputed by Minestom, because a fresh {@code Light} reports itself as
 * valid, so a lobby that leaves the sky to {@code LightingChunk} is a lobby whose sky light is
 * whatever the region file happened to carry. {@link LightingChunk} stays the chunk type for what
 * it does do: sending the light to the players.
 * </p>
 *
 * @author theEvilReaper
 * @author TheMeinerLP
 * @version 1.2.0
 * @since 1.0.0
 */
public final class MapProvider implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapProvider.class);
    private static final String MAP_PATH = "worlds";
    private final GsonFileHandler fileHandler;
    private final MapPool mapPool;
    private final Gson gson;
    private final ChunkLightScheduler lightScheduler;
    private final EventListener<InstanceChunkLoadEvent> chunkLoadListener;
    private final EventListener<InstanceTickEvent> tickListener;
    /**
     * Counts the light passes. The scheduler only needs a value that differs from the one it saw
     * last, and the tick event of Minestom carries a duration rather than a timestamp.
     */
    private final AtomicLong lightPass = new AtomicLong();
    private final InstanceContainer instance;
    private LobbyMap activeLobby;
    private @Nullable FalcoAnvilLoader chunkLoader;
    private @Nullable Path chunkLoaderRoot;
    private volatile boolean closed;

    private MapProvider(Path path, InstanceContainer instance, Function<Stream<Path>, List<MapEntry>> filterMaps) {
        this.mapPool = new MapPool(path.resolve(MAP_PATH), filterMaps);
        this.instance = instance;
        // Use LightingChunk so the world is actually lit: it is what sends sky and block light to
        // the players. Plain DynamicChunks send none, leaving the lobby pitch black. Must be set
        // before any chunk is loaded by the chunk loader.
        this.instance.setChunkSupplier(LightingChunk::new);
        // The scheduler is built per instance, as its contract requires, and it is what keeps the
        // light of the lobby correct while chunks keep arriving; see the class comment.
        this.lightScheduler = ChunkLightScheduler.builder(new ChunkLightService()).skyLight(ChunkLightScheduler.SkyLight.FROM_DIMENSION).onAreaCompleted(this::resendLight).build();
        this.chunkLoadListener = EventListener.of(InstanceChunkLoadEvent.class, this::markLoadedChunk);
        this.tickListener = EventListener.of(InstanceTickEvent.class, this::runLightPass);
        this.instance.eventNode().addListener(this.chunkLoadListener);
        this.instance.eventNode().addListener(this.tickListener);
        var typeAdapter = new PositionGsonAdapter();
        this.gson = new Gson().newBuilder().registerTypeAdapter(Pos.class, typeAdapter).registerTypeAdapter(Vec.class, typeAdapter).create();
        this.fileHandler = new GsonFileHandler(this.gson);
        try {
            this.loadMapData();
        } catch (RuntimeException | Error failure) {
            // The chunk loader is installed by loadMapData and holds open region files. A provider
            // whose constructor threw is never closed by anybody, so it closes itself here.
            this.close();
            throw failure;
        }
    }

    private MapProvider(Path path, InstanceContainer instance) {
        this(path, instance, MapProvider::defaultFilter);
    }

    /**
     * Marks a chunk that has just been loaded as needing light.
     * <p>
     * This runs on the thread that loaded the chunk — a virtual thread, since the Falco loader
     * loads in parallel — and does no work beyond the mark, which is the point: the light itself is
     * computed by the scheduler, once per tick, over areas that do not overlap.
     * </p>
     *
     * @param event the event of the chunk that was loaded
     */
    private void markLoadedChunk(InstanceChunkLoadEvent event) {
        if (this.closed) {
            return;
        }
        // markChanged rather than markDirty: nothing is known about which blocks arrived, so the
        // light kept for the chunk is dropped, and the eight chunks around it are marked as well
        // because the new blocks reach into them.
        this.lightScheduler.markChanged(event.getInstance(), event.getChunkX(), event.getChunkZ());
    }

    /**
     * Runs the light pass of one tick.
     *
     * @param event the tick event of the lobby instance
     */
    private void runLightPass(InstanceTickEvent event) {
        if (this.closed) {
            return;
        }
        this.lightScheduler.onTick(event.getInstance(), this.lightPass.incrementAndGet());
    }

    /**
     * Sends the light of the chunks a pass rewrote to the players who already hold them.
     * <p>
     * The scheduler drops the cached packets of every chunk it wrote, which covers everybody who
     * receives the chunk afterwards. It cannot cover a player who is already looking at it, because
     * a {@link LightingChunk} does not announce its own light; that is what the resend timer of
     * Minestom is for, and this is where it is armed.
     * </p>
     *
     * @param claimed the chunks the pass took
     * @param written the chunks it actually wrote
     */
    private void resendLight(List<ChunkArea> claimed, List<ChunkArea> written) {
        for (ChunkArea position : written) {
            if (this.instance.getChunk(position.x(), position.z()) instanceof LightingChunk chunk) {
                chunk.invalidateResendDelay();
            }
        }
    }

    private static List<MapEntry> defaultFilter(Stream<Path> pathStream) {
        return pathStream.map(MapEntry::new).filter(MapEntry::hasMapFile).collect(Collectors.toList());
    }

    /**
     * Writes the given map next to the world it belongs to and reads it back.
     *
     * @param baseMap the map to store
     * @throws IllegalStateException if the provider is closed
     */
    public void saveMap(BaseMap baseMap) {
        ensureOpen();
        this.fileHandler.save(this.mapPool.getMapEntry().path().resolve(AppConfig.MAP_FILE_NAME), baseMap instanceof LobbyMap gameMap ? gameMap : baseMap);
        loadMapData();
    }

    private void loadMapData() {
        ensureOpen();
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

    private void ensureOpen() {
        if (this.closed) {
            throw new IllegalStateException("The map provider of the lobby world is closed");
        }
    }

    /**
     * Closes the chunk loader and with it every region file the lobby still holds open.
     * <p>
     * Unwiring comes before closing, and that order is the whole point of this method. A closed
     * loader that is still the loader of the instance refuses every chunk a player walks into with
     * an {@link IllegalStateException}, and the listeners would keep marking chunks for a light
     * engine nobody is going to drive again. The instance is handed the no-op loader instead, which
     * reports a chunk as absent rather than throwing — nothing is saved after this point, so
     * nothing built can be overwritten by it.
     * </p>
     * <p>
     * Closing twice is allowed and does nothing the second time. Everything the provider still
     * offers after this point is read-only; {@link #saveMap(BaseMap)} refuses, rather than building
     * a fresh loader and reopening the files that were just closed.
     * </p>
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        EventNode<InstanceEvent> node = this.instance.eventNode();
        if (node != null) {
            node.removeListener(this.chunkLoadListener);
            node.removeListener(this.tickListener);
        }
        this.instance.setChunkLoader(ChunkLoader.noop());
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
     * Answers whether this provider has been closed.
     *
     * @return true if the provider is closed, otherwise false
     */
    public boolean isClosed() {
        return this.closed;
    }

    /**
     * Answers whether the given chunk is still waiting for its light.
     * <p>
     * The light of a chunk is written a tick or more after the chunk itself arrives, so a test that
     * reads a light level has to know when to look. Nothing in production asks this.
     * </p>
     *
     * @param chunkX the chunk x coordinate
     * @param chunkZ the chunk z coordinate
     * @return true if the chunk is still marked as needing light, otherwise false
     */
    @ApiStatus.Internal
    public boolean isLightPending(int chunkX, int chunkZ) {
        return this.lightScheduler.isDirty(chunkX, chunkZ);
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
