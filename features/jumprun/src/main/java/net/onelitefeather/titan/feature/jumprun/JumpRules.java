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

import java.util.List;

/** Decides whether a jump can be made and whether the real world leaves room for it. */
final class JumpRules {

    private final SpaceProbe probe;
    private final HeightBand band;

    JumpRules(SpaceProbe probe, HeightBand band) {
        this.probe = probe;
        this.band = band;
    }

    boolean isValid(Jump jump, Mode mode) {
        return isReachable(jump, mode) && isFree(jump);
    }

    /**
     * The target is in the height band and the world leaves room: at the target and along the way.
     */
    boolean isFree(Jump jump) {
        return band.allows(jump.to()) && hasRoomAtTarget(jump) && isFlightPathFree(jump);
    }

    /** Pure geometry: the player can bridge the gap and the rise, whatever stands in the way. */
    static boolean isReachable(Jump jump, Mode mode) {
        return jump.gap() >= Jump.MIN_GAP && jump.gap() <= maxGap(jump, mode) && jump.rise() <= Jump.MAX_RISE;
    }

    private static int maxGap(Jump jump, Mode mode) {
        int maxGap = jump.isAscent() ? mode.maxGapAscent() : mode.maxGap();
        return jump.isDiagonal() ? Math.min(maxGap, Jump.MAX_GAP_DIAGONAL) : maxGap;
    }

    private boolean hasRoomAtTarget(Jump jump) {
        BlockPos target = jump.to().pos();
        return probe.inBounds(target) && probe.isAir(target) && isColumnFree(target.x(), target.z(), target.y() + 1, jump.to().jumpRoomTopY());
    }

    /**
     * The straight line between the blocks, with both side cells at each diagonal step, must be
     * free up to the apex of the jump. An ascent lands at most 1 above the take-off, so below the
     * apex.
     */
    private boolean isFlightPathFree(Jump jump) {
        int lowest = (int) Math.floor(Math.min(jump.from().topY(), jump.to().topY()));
        int highest = Surface.highestBlockReached(jump.from().topY() + Surface.JUMP_HEIGHT);
        List<FlightPath.Cell> path = FlightPath.cellsBetween(jump.from().pos(), jump.to().pos());
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
}
