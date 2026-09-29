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
import com.google.gson.GsonBuilder;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Portal;
import net.theevilreaper.aves.file.gson.PositionGsonAdapter;

/** The one Gson configuration for {@code map.json}, shared by the provider and its tests. */
final class MapGson {

    private MapGson() {
    }

    static Gson create() {
        var positionAdapter = new PositionGsonAdapter();
        return new GsonBuilder().registerTypeAdapter(Pos.class, positionAdapter).registerTypeAdapter(Vec.class, positionAdapter).registerTypeAdapter(Portal.class, new PortalGsonAdapter()).create();
    }
}
