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
package net.onelitefeather.titan.feature.jumprun.space;

/**
 * Where the lobby spawn is, so a course can keep away from it. A pure value: the check needs no
 * world.
 */
public record SpawnZone(double x, double z) {

    /** Horizontal distance a course block keeps to the spawn once the ascent is over. */
    public static final int MIN_SPAWN_DISTANCE = 16;

    /** Whether the block cell is at least {@link #MIN_SPAWN_DISTANCE} blocks from the spawn. */
    public boolean isFarEnough(BlockPos pos) {
        return Math.hypot(pos.x() + 0.5 - x, pos.z() + 0.5 - z) >= MIN_SPAWN_DISTANCE;
    }
}
