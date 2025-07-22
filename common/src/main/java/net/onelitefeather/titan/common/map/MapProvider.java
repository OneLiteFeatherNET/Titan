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
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.onelitefeather.titan.common.config.AppConfig;
import net.theevilreaper.aves.file.GsonFileHandler;
import net.theevilreaper.aves.file.gson.PositionGsonAdapter;
import net.theevilreaper.aves.map.BaseMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
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

    private MapProvider(@NotNull Path path, @NotNull InstanceContainer instance, Function<Stream<Path>, List<MapEntry>> filterMaps) {
        this.mapPool = new MapPool(path.resolve(MAP_PATH), filterMaps);
        this.instance = instance;
        var typeAdapter = new PositionGsonAdapter();
        this.gson = new Gson().newBuilder().registerTypeAdapter(Pos.class, typeAdapter).registerTypeAdapter(Vec.class, typeAdapter).create();
        this.fileHandler = new GsonFileHandler(this.gson);
        this.loadMapData();
    }

    private MapProvider(@NotNull Path path, @NotNull InstanceContainer instance) {
        this(path, instance, MapProvider::defaultFilter);
    }

    private static List<MapEntry> defaultFilter(Stream<Path> pathStream) {
        return pathStream.map(MapEntry::new).filter(MapEntry::hasMapFile).collect(Collectors.toList());
    }

    public void saveMap(@NotNull BaseMap baseMap) {
        this.fileHandler.save(this.mapPool.getMapEntry().path().resolve(AppConfig.MAP_FILE_NAME), baseMap instanceof LobbyMap gameMap ? gameMap : baseMap);
        loadMapData();
    }

    private void loadMapData() {
        var lobbyData = this.fileHandler.load(this.mapPool.getMapEntry().path().resolve(AppConfig.MAP_FILE_NAME), LobbyMap.class);
        this.instance.setChunkLoader(new AnvilLoader(mapPool.getMapEntry().path()));
        try {
            this.activeLobby = lobbyData.orElse(LobbyMap.lobbyMapBuilder().build());

            if (this.activeLobby.getSpawn() != null) {
                loadChunk(this.instance, this.activeLobby.getSpawn());
            }
        } catch (NoSuchElementException noSuchElementException) {
            LOGGER.error("Failed to load the lobby data");
        }

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
        return new MapProvider(path, instance);
    }

    public static MapProvider create(@NotNull Path path, @NotNull InstanceContainer instance, Function<Stream<Path>, List<MapEntry>> filterMaps) {
        return new MapProvider(path, instance, filterMaps);
    }
}
