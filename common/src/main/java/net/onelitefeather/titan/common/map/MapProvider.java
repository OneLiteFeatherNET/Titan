package net.onelitefeather.titan.common.map;

import com.google.gson.Gson;
import de.icevizion.aves.file.GsonFileHandler;
import de.icevizion.aves.file.gson.PositionGsonAdapter;
import de.icevizion.aves.map.BaseMap;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.onelitefeather.titan.common.config.AppConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class MapProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapProvider.class);
    private static final String MAP_PATH = "maps";
    private final GsonFileHandler fileHandler;
    private final MapPool mapPool;
    private final Gson gson;
    private InstanceContainer instance;
    private LobbyMap activeLobby;

    private MapProvider(@NotNull Path path, @NotNull InstanceContainer instance) {
        this.mapPool = new MapPool(path.resolve(MAP_PATH));
        this.instance = instance;
        var typeAdapter = new PositionGsonAdapter();
        this.gson = new Gson().newBuilder()
                .registerTypeAdapter(Pos.class, typeAdapter)
                .registerTypeAdapter(Vec.class, typeAdapter)
                .create();
        this.fileHandler = new GsonFileHandler(this.gson);
        this.loadMapData();
    }

    public void saveMap(@NotNull Path path, @NotNull BaseMap baseMap) {
        this.fileHandler.save(path, baseMap instanceof LobbyMap gameMap ? gameMap : baseMap);
    }

    private void loadMapData() {
        var lobbyData = this.fileHandler.load(this.mapPool.getMapEntry().path().resolve(AppConfig.MAP_FILE_NAME), LobbyMap.class);
        this.instance.setChunkLoader(new AnvilLoader(mapPool.getMapEntry().path()));
        this.activeLobby = lobbyData.get();

        if (this.activeLobby.getSpawn() != null) {
            loadChunk(this.instance, this.activeLobby.getSpawn());
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

    public @NotNull @UnmodifiableView List<MapEntry> getAvailableMaps() {
        return Collections.unmodifiableList(this.mapPool.getAvailableMaps());
    }

    public static MapProvider create(@NotNull Path path, @NotNull InstanceContainer instance) {
        return new MapProvider(path, instance);
    }
}
