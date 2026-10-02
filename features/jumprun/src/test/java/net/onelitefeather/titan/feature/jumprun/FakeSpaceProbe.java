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

import java.util.HashSet;
import java.util.Set;

/** Test double for {@link SpaceProbe}: a world that is air except for the positions occupied in it. */
final class FakeSpaceProbe implements SpaceProbe {

    private final Set<BlockPos> occupied = new HashSet<>();
    private final BlockPos min;
    private final BlockPos max;

    /** An open world from (-50, 0, -50) to (50, 100, 50), both inclusive. */
    FakeSpaceProbe() {
        this(new BlockPos(-50, 0, -50), new BlockPos(50, 100, 50));
    }

    FakeSpaceProbe(BlockPos min, BlockPos max) {
        this.min = min;
        this.max = max;
    }

    FakeSpaceProbe occupy(int x, int y, int z) {
        occupied.add(new BlockPos(x, y, z));
        return this;
    }

    /** Occupies every position of the box between the two corners, inclusive. */
    FakeSpaceProbe occupyBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    occupy(x, y, z);
                }
            }
        }
        return this;
    }

    @Override
    public boolean isAir(BlockPos pos) {
        return !occupied.contains(pos);
    }

    @Override
    public boolean inBounds(BlockPos pos) {
        return pos.x() >= min.x() && pos.x() <= max.x()
                && pos.y() >= min.y() && pos.y() <= max.y()
                && pos.z() >= min.z() && pos.z() <= max.z();
    }
}
