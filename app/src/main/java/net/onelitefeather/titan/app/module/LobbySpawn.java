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
package net.onelitefeather.titan.app.module;

import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.Nullable;

/**
 * The lobby's current spawn position, read lazily on every use rather than captured once - the
 * lobby map's spawn point can change after a module was built (e.g. a map reload).
 *
 * <p>A dedicated platform type rather than a bare {@code Supplier<Pos>}: {@code Supplier<Pos>} is a
 * generic type with no meaning of its own, which would make it an ambiguous bean for a dependency
 * injection container to wire (see {@code openspec/changes/avaje-dependency-injection/design.md},
 * decision 4). {@code app/.../bootstrap/PlatformBeans} provides the platform's single instance,
 * backed by the active lobby map's spawn point.
 */
@FunctionalInterface
public interface LobbySpawn {

    /**
     * @return the current lobby spawn position, or {@code null} if the active lobby map has none
     */
    @Nullable
    Pos position();
}
