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

import java.util.Arrays;

/** The step from one course block to the next, with its geometry and its difficulty cost. */
record Jump(CourseBlock from, CourseBlock to) {

    static final int MIN_GAP = 1;
    static final int MAX_GAP = 4;
    static final int MAX_GAP_ASCENT = 3;

    /** A diagonal gap of n blocks is about 1.4 times as far as an axis gap of n. */
    static final int MAX_GAP_DIAGONAL = 2;
    static final double MAX_RISE = 1.0;

    private static final double TYPE_WEIGHT = 2.0;
    private static final double GAP_WEIGHT = 1.5;
    private static final double ASCENT_WEIGHT = 1.0;

    /** Cost of the hardest allowed jump: the hardest surface over the widest flat gap. */
    static final double MAX_COST = TYPE_WEIGHT * Arrays.stream(Surface.values()).mapToInt(Surface::typeCost).max().orElse(0) + GAP_WEIGHT * (MAX_GAP - MIN_GAP);

    /** Air blocks between the two blocks: the horizontal Chebyshev distance minus one. */
    int gap() {
        int dx = Math.abs(to.pos().x() - from.pos().x());
        int dz = Math.abs(to.pos().z() - from.pos().z());
        return Math.max(dx, dz) - 1;
    }

    /** Whether the jump leaves along both axes at once. */
    boolean isDiagonal() {
        return to.pos().x() != from.pos().x() && to.pos().z() != from.pos().z();
    }

    /** How far the target's walkable top lies above the source's (negative when lower). */
    double rise() {
        return to.topY() - from.topY();
    }

    boolean isAscent() {
        return rise() > 0;
    }

    /** A diagonal jump is as hard as an axis jump one gap level wider. */
    double cost() {
        int gapLevel = gap() - MIN_GAP + (isDiagonal() ? 1 : 0);
        return TYPE_WEIGHT * to.surface().typeCost() + GAP_WEIGHT * gapLevel + (isAscent() ? ASCENT_WEIGHT : 0.0);
    }
}
