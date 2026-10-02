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

/** How much free space surrounds a block position, read from a {@link SpaceProbe}. */
record Openness(SpaceProbe probe) {

    /** Air blocks a block needs below it to count as standing in the open. */
    static final int MIN_AIR_BELOW = 4;

    /** How far down the column below a target is looked at. */
    private static final int COLUMN_DEPTH = 12;

    /** Whether the {@link #MIN_AIR_BELOW} blocks directly below the position are all air. */
    boolean hasAirBelow(BlockPos pos) {
        return airInColumn(pos, MIN_AIR_BELOW) == MIN_AIR_BELOW;
    }

    /**
     * The share of air in the column below the position and in the eight cells around it at its own
     * and the feet level: 1 in the open, 0 when walled in.
     */
    double of(BlockPos pos) {
        int cells = COLUMN_DEPTH;
        int air = airInColumn(pos, COLUMN_DEPTH);
        for (int level = 0; level <= 1; level++) {
            for (Direction direction : Direction.values()) {
                cells++;
                air += probe.isAir(pos.offset(direction.dx(), level, direction.dz())) ? 1 : 0;
            }
        }
        return (double) air / cells;
    }

    private int airInColumn(BlockPos pos, int depth) {
        int air = 0;
        for (int below = 1; below <= depth; below++) {
            air += probe.isAir(pos.offset(0, -below, 0)) ? 1 : 0;
        }
        return air;
    }
}
