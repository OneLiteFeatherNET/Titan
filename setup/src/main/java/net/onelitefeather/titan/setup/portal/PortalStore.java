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
package net.onelitefeather.titan.setup.portal;

import net.onelitefeather.titan.core.portal.Portal;

import java.util.List;

/**
 * Where the portals of the loaded world live; the editor neither knows files nor the map provider.
 */
public interface PortalStore {

    /** The saved portals of the world, in file order. */
    List<Portal> portals();

    /** Replaces the world's portals with {@code portals} and keeps everything else in the map. */
    void save(List<Portal> portals);

    /** Name of the world for log lines. */
    default String world() {
        return "unknown";
    }
}
