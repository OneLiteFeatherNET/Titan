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

/** The eight horizontal ways a jump can lead away from a block. */
public enum Direction {
    EAST(1, 0), WEST(-1, 0), SOUTH(0, 1), NORTH(0, -1), SOUTH_EAST(1, 1), SOUTH_WEST(-1, 1), NORTH_EAST(1, -1), NORTH_WEST(-1, -1);

    private final int dx;
    private final int dz;

    Direction(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    public int dx() {
        return dx;
    }

    public int dz() {
        return dz;
    }

    boolean isDiagonal() {
        return dx != 0 && dz != 0;
    }

    /** Length of the step {@code (dx, dz)}: 1 along an axis, the square root of 2 diagonally. */
    double length() {
        return isDiagonal() ? Math.sqrt(2.0) : 1.0;
    }

    /** The direction of a step by {@code (dx, dz)} blocks, judged by the signs only. */
    public static Direction toward(int dx, int dz) {
        for (Direction direction : values()) {
            if (direction.dx == Integer.signum(dx) && direction.dz == Integer.signum(dz)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("not a horizontal step: " + dx + ", " + dz);
    }
}
