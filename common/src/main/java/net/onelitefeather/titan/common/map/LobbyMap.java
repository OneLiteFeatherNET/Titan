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

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.titan.core.portal.Portal;
import net.theevilreaper.aves.map.BaseMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class LobbyMap extends BaseMap {

    /**
     * Gson builds this class without a constructor, so a map file without portals leaves it
     * {@code null}.
     */
    private @Nullable List<Portal> portals;

    public LobbyMap(String name, Pos spawn, List<String> builders) {
        this(name, spawn, builders, List.of());
    }

    public LobbyMap(String name, Pos spawn, List<String> builders, List<Portal> portals) {
        super(name, spawn, builders);
        // Kept null when empty so a map without portals is written back without a "portals" key.
        this.portals = portals == null || portals.isEmpty() ? null : List.copyOf(portals);
    }

    /** Unmodifiable; empty, never {@code null}, if the map has no portals. */
    public List<Portal> portals() {
        return this.portals == null ? List.of() : this.portals;
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
        // Every copy path must carry the portals, or /setup map setspawn|setname|setauthor would delete them.
        builder.portals(map.portals());
        return builder;
    }

    public static Builder lobbyMapBuilder() {
        return new LobbyMapBuilder();
    }

    public sealed interface Builder permits LobbyMapBuilder {

        Builder spawn(Pos spawn);

        Builder name(String name);

        Builder author(String... author);

        Builder portals(List<Portal> portals);

        LobbyMap build();
    }
}
