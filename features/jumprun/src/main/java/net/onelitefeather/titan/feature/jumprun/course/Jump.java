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
package net.onelitefeather.titan.feature.jumprun.course;

import java.util.Collection;
import java.util.List;
import net.onelitefeather.titan.feature.jumprun.space.Direction;

/** The step from one course block to the next, with its geometry and its difficulty cost. */
public record Jump(Placement from, Placement to) {

    static final int MIN_GAP = 1;
    static final int MAX_GAP = 4;
    static final int MAX_GAP_ASCENT = 3;

    /** A diagonal gap of n blocks is about 1.4 times as far as an axis gap of n. */
    static final int MAX_GAP_DIAGONAL = 2;
    static final double MAX_RISE = 1.0;

    private static final double TYPE_WEIGHT = 2.0;
    private static final double GAP_WEIGHT = 1.5;

    /**
     * Cost of the lowest tower, and what each block above it adds: steep enough that the tallest
     * tower the config allows costs as much as the hardest medium jump.
     */
    private static final double CLIMB_COST = 2.0;
    private static final double CLIMB_HEIGHT_WEIGHT = 2.1;
    private static final int CLIMB_LOWEST = 3;


    /**
     * Cost of the hardest allowed jump of the medium mode with any shape: the hardest surface over
     * the widest flat gap, or the highest tower when that is harder.
     */
    static final double MAX_COST = Math.max(maxCost(Mode.MEDIUM, List.of(Surface.values())), climbCost(Climb.MAX_LIMIT));

    /** Cost of a tower of the given height: a fixed part plus the blocks above the lowest tower. */
    static double climbCost(int height) {
        return CLIMB_COST + CLIMB_HEIGHT_WEIGHT * (height - CLIMB_LOWEST);
    }

    /** The tower height, unrounded, that costs {@code cost}: the inverse of {@link #climbCost}. */
    static double climbHeightFor(double cost) {
        return CLIMB_LOWEST + (cost - CLIMB_COST) / CLIMB_HEIGHT_WEIGHT;
    }

    /** Cost of the hardest jump onto one of the {@code surfaces} over the widest flat gap. */
    static double maxCost(Mode mode, Collection<Surface> surfaces) {
        int hardest = surfaces.stream().mapToInt(Surface::typeCost).max().orElse(0);
        return TYPE_WEIGHT * hardest + GAP_WEIGHT * (mode.maxGap() - MIN_GAP);
    }

    /** Air blocks between the two blocks: the horizontal Chebyshev distance minus one. */
    int gap() {
        int dx = Math.abs(to.pos().x() - from.pos().x());
        int dz = Math.abs(to.pos().z() - from.pos().z());
        return Math.max(dx, dz) - 1;
    }

    /** The way the jump leads, judged by the signs of the offset. */
    Direction direction() {
        return Direction.toward(to.pos().x() - from.pos().x(), to.pos().z() - from.pos().z());
    }

    /** Whether the jump leaves along both axes at once. */
    boolean isDiagonal() {
        return to.pos().x() != from.pos().x() && to.pos().z() != from.pos().z();
    }

    /**
     * How far the target's highest step lies above the source's lowest (negative when lower): the
     * worst case for stairs, whose facing is not known when the position is chosen.
     */
    double rise() {
        return to.topY() - from.lowTopY();
    }

    boolean isAscent() {
        return rise() > 0;
    }

    /** A diagonal jump is as hard as an axis jump one gap level wider; a tower costs its height. */
    double cost(Mode mode) {
        if (to.climb().isPresent()) {
            return climbCost(to.climb().get().height());
        }
        int gapLevel = gap() - MIN_GAP + (isDiagonal() ? 1 : 0);
        return TYPE_WEIGHT * to.surface().typeCost() + GAP_WEIGHT * gapLevel + (isAscent() ? mode.ascentWeight() : 0.0);
    }
}
