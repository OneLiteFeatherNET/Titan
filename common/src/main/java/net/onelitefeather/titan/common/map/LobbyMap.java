package net.onelitefeather.titan.common.map;

import net.minestom.server.coordinate.Pos;
import net.theevilreaper.aves.map.BaseMap;

public final class LobbyMap extends BaseMap {


    public static Builder lobbyMapBuilder(LobbyMap map) {
        var builder = lobbyMapBuilder();
        if (map == null) {
            return builder;
        }
        if (map.getSpawn() != null) {
            builder.spawn(map.getSpawn());
        }
        if (map.getName() != null) {
            builder.name(map.getName());
        }
        if (map.getBuilders() != null) {
            builder.author(map.getBuilders());
        }
        return builder;
    }

    public static Builder lobbyMapBuilder() {
        return new LobbyMapBuilder();
    }

    public sealed interface Builder permits LobbyMapBuilder {

        /**
         * Sets the spawn position of the map.
         *
         * @param spawn the spawn position
         * @return the builder
         */
        Builder spawn(Pos spawn);

        /**
         * Sets the name of the map.
         *
         * @param name the name of the map
         * @return the builder
         */
        Builder name(String name);

        /**
         * Sets the author of the map.
         *
         * @param author the author of the map
         * @return the builder
         */
        Builder author(String... author);

        /**
         * Builds the map.
         * @return the map
         */
        LobbyMap build();
    }
}
