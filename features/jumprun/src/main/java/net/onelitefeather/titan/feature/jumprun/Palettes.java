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
package net.onelitefeather.titan.feature.jumprun;

import java.util.EnumMap;
import java.util.Map;
import java.util.random.RandomGenerator;
import net.minestom.server.instance.block.Block;

/**
 * The materials a {@link Surface} can show, one {@link Palette} per shape. Every entry of a
 * palette collides up to the shape's height, so the material never changes what a jump demands.
 * Built from the config by {@link JumprunSettings}.
 */
final class Palettes {

    private final Map<Surface, Palette> byShape;

    /**
     * @throws IllegalArgumentException when a shape has no palette
     */
    Palettes(Map<Surface, Palette> byShape) {
        for (Surface surface : Surface.values()) {
            if (!byShape.containsKey(surface)) {
                throw new IllegalArgumentException("no palette for " + surface);
            }
        }
        this.byShape = new EnumMap<>(byShape);
    }

    Palette of(Surface surface) {
        return byShape.get(surface);
    }

    Block draw(Surface surface, RandomGenerator random) {
        return of(surface).draw(random);
    }
}
