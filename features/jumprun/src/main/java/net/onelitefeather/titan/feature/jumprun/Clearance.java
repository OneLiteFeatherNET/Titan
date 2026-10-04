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

import java.util.Collection;
import java.util.List;

/**
 * Keeps a new jump away from the blocks the player still sees, so earlier blocks do not get in the
 * way of a jump. Pure geometry, the counterpart of {@link OccupiedProbe} for the surroundings of
 * blocks instead of the blocks themselves.
 */
final class Clearance {

    /** Horizontal Chebyshev distance the target and the flight path keep to a visible block. */
    static final int MIN_DISTANCE = 2;

    /** Blocks above and below the jump's own height span that still count as in the way. */
    static final int HEIGHT_MARGIN = 2;

    private Clearance() {
    }

    /**
     * Whether the target and every cell of the flight path keep {@link #MIN_DISTANCE} to each of
     * the {@code visible} blocks within the jump's height span, the source excepted: the jump
     * leaves from it.
     */
    static boolean isKept(Jump jump, Collection<? extends Placement> visible) {
        int low = Math.min(jump.from().pos().y(), jump.to().pos().y()) - HEIGHT_MARGIN;
        int high = Math.max(jump.from().pos().y(), jump.to().pos().y()) + HEIGHT_MARGIN;
        if (!staysOutOfPendingJumps(jump, visible)) {
            return false;
        }
        List<BlockPos> others = visible.stream().map(Placement::pos).filter(pos -> !pos.equals(jump.from().pos())).filter(pos -> pos.y() >= low && pos.y() <= high).toList();
        if (others.isEmpty()) {
            return true;
        }
        List<FlightPath.Cell> cells = FlightPath.cellsUpTo(jump.from().pos(), jump.to().pos());
        return others.stream().noneMatch(pos -> cells.stream().anyMatch(cell -> isTooClose(cell, pos)));
    }

    /**
     * The jumps up to the source are still to be made when the new one is drawn, so its target must
     * keep {@link #MIN_DISTANCE} to the cells each of them flies over, within its own height span.
     * {@code visible} is in course order.
     */
    private static boolean staysOutOfPendingJumps(Jump jump, Collection<? extends Placement> visible) {
        List<BlockPos> positions = visible.stream().map(Placement::pos).toList();
        BlockPos target = jump.to().pos();
        for (int i = 1; i <= positions.indexOf(jump.from().pos()); i++) {
            BlockPos start = positions.get(i - 1);
            BlockPos end = positions.get(i);
            boolean inSpan = target.y() >= Math.min(start.y(), end.y()) - HEIGHT_MARGIN && target.y() <= Math.max(start.y(), end.y()) + HEIGHT_MARGIN;
            if (inSpan && FlightPath.cellsBetween(start, end).stream().anyMatch(cell -> isTooClose(cell, target))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTooClose(FlightPath.Cell cell, BlockPos pos) {
        return Math.max(Math.abs(cell.x() - pos.x()), Math.abs(cell.z() - pos.z())) < MIN_DISTANCE;
    }
}
