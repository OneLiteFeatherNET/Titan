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
package net.onelitefeather.titan.app.feature.spawn;

/**
 * The pure height rule behind the lobby's out-of-bounds teleport: a player is out of bounds once
 * their {@code y} coordinate leaves the {@code [minHeight, maxHeight]} range.
 *
 * <p>Deliberately free of any Minestom or module type so it can be unit-tested without a server -
 * {@link SpawnModule} is the only thing that turns "out of bounds" into an actual teleport, via
 * {@link SpawnBoundsListener}.
 */
final class HeightBounds {

    private final int minHeight;
    private final int maxHeight;

    /**
     * @param minHeight the lowest {@code y} coordinate still considered in bounds
     * @param maxHeight the highest {@code y} coordinate still considered in bounds
     */
    HeightBounds(int minHeight, int maxHeight) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }

    /**
     * @param y the coordinate to check
     * @return {@code true} if {@code y} is below {@code minHeight} or above {@code maxHeight}
     */
    boolean isOutOfBounds(double y) {
        return y < this.minHeight || y > this.maxHeight;
    }
}
