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

/** The area of a {@link Portal}: what a movement between two positions has to cross to trigger it. */
public sealed interface PortalShape permits Box, Disc {

    /** Whether the straight segment from {@code from} to {@code to} touches this shape. */
    boolean crossedBy(Point from, Point to);

    /** Smallest axis-parallel rectangle in X/Z that contains the shape, for the column index. */
    HorizontalBounds horizontalBounds();
}
