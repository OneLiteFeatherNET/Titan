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
package net.onelitefeather.titan.common.map;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.minestom.server.coordinate.Point;
import net.minestom.server.event.instance.InstanceChunkLoadEvent;
import net.minestom.server.instance.Clock;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.onelitefeather.titan.core.portal.PortalValidator;
import net.theevilreaper.aves.file.GsonFileHandler;
import net.theevilreaper.aves.map.BaseMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MapProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapProvider.class);
    private static final String MAP_PATH = "worlds";
    private final GsonFileHandler fileHandler;
    private final MapPool mapPool;
    private final Gson gson;
    private InstanceContainer instance;
    private LobbyMap activeLobby;

    private MapProvider(@NotNull Path path, @NotNull InstanceContainer instance, Function<Stream<Path>, List<MapEntry>> filterMaps, @NotNull Optional<String> worldName) {
        this.mapPool = new MapPool(path.resolve(MAP_PATH), filterMaps, worldName);
        this.instance = instance;
        // Relight each chunk as it loads so unexplored regions light up (anvil chunks otherwise
        // stay dark until a block update triggers a relight).
        this.instance.eventNode().addListener(InstanceChunkLoadEvent.class, event -> LightingChunk.relight(event.getInstance(), List.of(event.getChunk())));
        this.gson = MapGson.create();
        this.fileHandler = new GsonFileHandler(this.gson);
        this.loadMapData();
    }


    private static List<MapEntry> defaultFilter(Stream<Path> pathStream) {
        return pathStream.map(MapEntry::new).filter(MapEntry::hasMapFile).collect(Collectors.toList());
    }

    public void saveMap(@NotNull BaseMap baseMap) {
        this.fileHandler.save(this.mapPool.getMapEntry().path().resolve(MapEntry.MAP_FILE_NAME), baseMap instanceof LobbyMap gameMap ? gameMap : baseMap);
        loadMapData();
    }

    private void loadMapData() {
        var lobbyData = this.readLobbyData();
        // LightingChunk computes and sends sky/block light; plain DynamicChunks send none, leaving
        // the lobby pitch black. Must be set before the AnvilLoader loads any chunk.
        this.instance.setChunkSupplier(LightingChunk::new);
        // Freeze the lobby at midday so it stays bright; otherwise the default
        // day/night cycle keeps advancing and the world renders dark.
        this.instance.setTime(6000);
        Clock clock = this.instance.defaultClock();
        if (clock != null) {
            clock.rate(0.0f);
        }
        this.instance.setChunkLoader(new AnvilLoader(mapPool.getMapEntry().path()));
        try {
            this.activeLobby = lobbyData.orElse(LobbyMap.lobbyMapBuilder().build());

            if (this.activeLobby.spawn() != null) {
                loadChunk(this.instance, this.activeLobby.spawn());
            }
        } catch (NoSuchElementException noSuchElementException) {
            LOGGER.error("Failed to load the lobby data");
        }

    }

    private Optional<LobbyMap> readLobbyData() {
        return readMap(activeMap());
    }

    /**
     * Reads the map file of any world without touching the loaded instance, its chunk loader or the
     * active lobby. Aves' handler lets a parse error of the portal adapter escape (pinned by
     * GsonFileHandlerLoadTest), so an unreadable or invalid portal aborts here instead of silently
     * reading a map without portals.
     */
    public Optional<LobbyMap> readMap(@NotNull MapEntry entry) {
        Path worldDirectory = entry.path();
        String world = worldDirectory.getFileName().toString();
        Optional<LobbyMap> lobbyData;
        try {
            lobbyData = this.fileHandler.load(entry.getMapFile(), LobbyMap.class);
        } catch (JsonParseException exception) {
            throw new IllegalStateException("Invalid portals in world '" + world + "': " + exception.getMessage(), exception);
        }
        lobbyData.ifPresent(map -> PortalValidator.requireValid(world, map.portals()));
        return lobbyData;
    }

    /** The map entry of the world this provider loaded. */
    public @NotNull MapEntry activeMap() {
        return this.mapPool.getMapEntry();
    }

    private <T extends Point> void loadChunk(@NotNull InstanceContainer instance, @NotNull T pos) {
        if (!ChunkUtils.isLoaded(instance, pos)) {
            instance.loadChunk(pos);
        }
    }

    public InstanceContainer getInstance() {
        return instance;
    }

    public @NotNull LobbyMap getActiveLobby() {
        return activeLobby;
    }

    public @NotNull
    @UnmodifiableView List<MapEntry> getAvailableMaps() {
        return Collections.unmodifiableList(this.mapPool.getAvailableMaps());
    }

    public static MapProvider create(@NotNull Path path, @NotNull InstanceContainer instance) {
        return new MapProvider(path, instance, MapProvider::defaultFilter, Optional.empty());
    }

    public static MapProvider create(@NotNull Path path, @NotNull InstanceContainer instance, @NotNull Optional<String> worldName) {
        return new MapProvider(path, instance, MapProvider::defaultFilter, worldName);
    }

    public static MapProvider create(@NotNull Path path, @NotNull InstanceContainer instance, Function<Stream<Path>, List<MapEntry>> filterMaps) {
        return new MapProvider(path, instance, filterMaps, Optional.empty());
    }
}
