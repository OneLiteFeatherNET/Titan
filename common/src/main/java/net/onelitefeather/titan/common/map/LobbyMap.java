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

import net.minestom.server.coordinate.Pos;
import net.theevilreaper.aves.map.BaseMap;

import java.util.List;

public final class LobbyMap extends BaseMap {

    public LobbyMap(String name, Pos spawn, List<String> builders) {
        super(name, spawn, builders);
    }

    public static Builder lobbyMapBuilder(LobbyMap map) {
        var builder = lobbyMapBuilder();
        if (map == null) {
            return builder;
        }
        if (map.spawn() != null) {
            builder.spawn(map.spawn());
        }
        if (map.name() != null) {
            builder.name(map.name());
        }
        if (map.builders() != null) {
            builder.author(map.builders().toArray(new String[0]));
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
         * @param spawn
         *              the spawn position
         * @return the builder
         */
        Builder spawn(Pos spawn);

        /**
         * Sets the name of the map.
         *
         * @param name
         *             the name of the map
         * @return the builder
         */
        Builder name(String name);

        /**
         * Sets the author of the map.
         *
         * @param author
         *               the author of the map
         * @return the builder
         */
        Builder author(String... author);

        /**
         * Builds the map.
         * 
         * @return the map
         */
        LobbyMap build();
    }
}
