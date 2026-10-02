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

/**
 * How much free space surrounds a block position, read from a {@link SpaceProbe}. Cells outside
 * the world count as closed: nothing can be placed or stood on there.
 */
record Openness(SpaceProbe probe) {

    /** Air blocks a scored block needs below it to count as standing in the open. */
    static final int SCORED_AIR_BELOW = 6;

    /** Air blocks below the last ascent block, so the scored part starts well above the ground. */
    static final int ASCENT_AIR_BELOW = 8;

    /** How far down the column below a target is looked at. */
    private static final int COLUMN_DEPTH = 16;

    /**
     * The cells around a position that are looked at: the eight neighbours on two levels. The
     * eight are {@link Direction#values()}, so the count follows the enum.
     */
    private static final int NEIGHBOURS = 2 * Direction.values().length;

    /** Weight of the vertical air in {@link #of}: space below matters more than beside. */
    private static final double COLUMN_WEIGHT = 0.7;

    /** Whether the {@code blocks} blocks directly below the position are all air. */
    boolean hasAirBelow(BlockPos pos, int blocks) {
        return airInColumn(pos, blocks) == blocks;
    }

    /**
     * How open the position is, from 1 in the open to 0 when walled in: the share of air in the
     * column below it, weighted 0.7, plus the share in the eight cells around it at its own and
     * the feet level, weighted 0.3.
     */
    double of(BlockPos pos) {
        return COLUMN_WEIGHT * airInColumn(pos, COLUMN_DEPTH) / COLUMN_DEPTH + (1.0 - COLUMN_WEIGHT) * airAround(pos) / NEIGHBOURS;
    }

    private int airAround(BlockPos pos) {
        int air = 0;
        for (int level = 0; level <= 1; level++) {
            for (Direction direction : Direction.values()) {
                air += isOpen(pos.offset(direction.dx(), level, direction.dz())) ? 1 : 0;
            }
        }
        return air;
    }

    private int airInColumn(BlockPos pos, int depth) {
        int air = 0;
        for (int below = 1; below <= depth; below++) {
            air += isOpen(pos.offset(0, -below, 0)) ? 1 : 0;
        }
        return air;
    }

    private boolean isOpen(BlockPos pos) {
        return probe.inBounds(pos) && probe.isAir(pos);
    }
}
