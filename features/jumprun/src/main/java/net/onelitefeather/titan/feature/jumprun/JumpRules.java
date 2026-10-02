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

import java.util.ArrayList;
import java.util.List;

/** Decides whether a jump can be made and whether the real world leaves room for it. */
final class JumpRules {

    /**
     * Distance kept to the dimension's top. The spawn column teleports players above its own
     * limit, which this column does not read, so it stays clear of the top instead.
     */
    static final int MAX_Y_MARGIN = 5;

    private final SpaceProbe probe;

    JumpRules(SpaceProbe probe) {
        this.probe = probe;
    }

    boolean isValid(Jump jump) {
        return isReachable(jump) && isFree(jump);
    }

    /** The world leaves room for the jump: at the target and along the way. */
    boolean isFree(Jump jump) {
        return hasRoomAtTarget(jump) && isFlightPathFree(jump);
    }

    /** Pure geometry: the player can bridge the gap and the rise, whatever stands in the way. */
    static boolean isReachable(Jump jump) {
        return jump.gap() >= Jump.MIN_GAP && jump.gap() <= maxGap(jump) && jump.rise() <= Jump.MAX_RISE;
    }

    private static int maxGap(Jump jump) {
        int maxGap = jump.isAscent() ? Jump.MAX_GAP_ASCENT : Jump.MAX_GAP;
        return jump.isDiagonal() ? Math.min(maxGap, Jump.MAX_GAP_DIAGONAL) : maxGap;
    }

    private boolean hasRoomAtTarget(Jump jump) {
        BlockPos target = jump.to().pos();
        boolean inBounds = probe.inBounds(target) && probe.inBounds(target.above(MAX_Y_MARGIN));
        return inBounds && probe.isAir(target) && isColumnFree(target.x(), target.z(), target.y() + 1, jump.to().headroomTopY());
    }

    /**
     * The straight line between the blocks, with both side cells at each diagonal step, must be
     * free.
     */
    private boolean isFlightPathFree(Jump jump) {
        double higherTop = Math.max(jump.from().topY(), jump.to().topY());
        int lowest = (int) Math.floor(higherTop);
        int highest = Surface.highestBlockReached(higherTop);
        List<Cell> path = cellsBetween(jump.from().pos(), jump.to().pos());
        return path.stream().allMatch(cell -> isColumnFree(cell.x(), cell.z(), lowest, highest));
    }

    private boolean isColumnFree(int x, int z, int fromY, int toY) {
        for (int y = fromY; y <= toY; y++) {
            if (!probe.isAir(new BlockPos(x, y, z))) {
                return false;
            }
        }
        return true;
    }

    private record Cell(int x, int z) {
    }

    /** Bresenham line in the XZ plane without its two ends. */
    private static List<Cell> cellsBetween(BlockPos from, BlockPos to) {
        int dx = Math.abs(to.x() - from.x());
        int dz = Math.abs(to.z() - from.z());
        int stepX = Integer.signum(to.x() - from.x());
        int stepZ = Integer.signum(to.z() - from.z());
        int error = dx - dz;
        int x = from.x();
        int z = from.z();
        List<Cell> cells = new ArrayList<>();
        while (x != to.x() || z != to.z()) {
            int doubled = 2 * error;
            boolean movesX = doubled > -dz;
            boolean movesZ = doubled < dx;
            if (movesX) {
                error -= dz;
                x += stepX;
            }
            if (movesZ) {
                error += dx;
                z += stepZ;
            }
            if (movesX && movesZ) {
                cells.add(new Cell(x, z - stepZ));
                cells.add(new Cell(x - stepX, z));
            }
            if (x != to.x() || z != to.z()) {
                cells.add(new Cell(x, z));
            }
        }
        return cells;
    }
}
