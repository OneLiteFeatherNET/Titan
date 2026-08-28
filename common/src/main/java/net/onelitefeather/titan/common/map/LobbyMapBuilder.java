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

import java.util.List;

final class LobbyMapBuilder implements LobbyMap.Builder {

    private Pos spawn;
    private String name;
    private String[] author;

    @Override
    public LobbyMap.Builder spawn(Pos spawn) {
        this.spawn = spawn;
        return this;
    }

    @Override
    public LobbyMap.Builder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public LobbyMap.Builder author(String... author) {
        this.author = author;
        return this;
    }

    @Override
    public LobbyMap build() {
        List<String> builders = author == null ? null : List.of(author);
        return new LobbyMap(name, spawn, builders);
    }
}
