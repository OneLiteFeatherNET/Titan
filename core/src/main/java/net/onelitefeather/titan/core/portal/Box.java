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
package net.onelitefeather.titan.core.portal;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

/**
 * An axis-parallel box. {@code min} and {@code max} are block coordinates, both inclusive, so the
 * box covers {@code [min, max + 1]} per axis in world coordinates.
 */
public record Box(Vec min, Vec max) implements PortalShape {

    @Override
    public boolean crossedBy(Point from, Point to) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public HorizontalBounds horizontalBounds() {
        return new HorizontalBounds(min.x(), min.z(), max.x() + 1, max.z() + 1);
    }
}
