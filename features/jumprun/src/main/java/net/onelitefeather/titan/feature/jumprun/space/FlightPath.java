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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** The cells a jump flies over, shared by the checks that look at the way between two blocks. */
public final class FlightPath {

    private FlightPath() {
    }

    /** A cell of the XZ plane. */
    public record Cell(int x, int z) {
    }

    /** What a jump flies over and lands in: {@link #cellsBetween} plus the target's cell. */
    public static List<Cell> cellsUpTo(BlockPos from, BlockPos target) {
        return Stream.concat(cellsBetween(from, target).stream(), Stream.of(new Cell(target.x(), target.z()))).toList();
    }

    /** Bresenham line in the XZ plane without its two ends. */
    public static List<Cell> cellsBetween(BlockPos from, BlockPos to) {
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
