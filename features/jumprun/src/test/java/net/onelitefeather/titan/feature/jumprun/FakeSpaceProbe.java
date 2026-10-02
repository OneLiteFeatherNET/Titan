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

/**
 * Test double for {@link SpaceProbe}. An open world is air except for the positions occupied in it;
 * a solid world is occupied except for the positions carved out of it.
 */
final class FakeSpaceProbe implements SpaceProbe {

    private final boolean solid;
    private final Set<BlockPos> marked = new HashSet<>();
    private final BlockPos min;
    private final BlockPos max;

    /** An open world from (-50, 0, -50) to (50, 100, 50), both inclusive. */
    FakeSpaceProbe() {
        this(false, new BlockPos(-50, 0, -50), new BlockPos(50, 100, 50));
    }

    FakeSpaceProbe(BlockPos min, BlockPos max) {
        this(false, min, max);
    }

    private FakeSpaceProbe(boolean solid, BlockPos min, BlockPos max) {
        this.solid = solid;
        this.min = min;
        this.max = max;
    }

    /** A world that is solid everywhere until carved, same bounds as {@link #FakeSpaceProbe()}. */
    static FakeSpaceProbe solidWorld() {
        return new FakeSpaceProbe(true, new BlockPos(-50, 0, -50), new BlockPos(50, 100, 50));
    }

    /** Occupies the position; meant for an open world. */
    FakeSpaceProbe occupy(int x, int y, int z) {
        marked.add(new BlockPos(x, y, z));
        return this;
    }

    /** Occupies every position of the box between the two corners, inclusive. */
    FakeSpaceProbe occupyBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        forEachInBox(x1, y1, z1, x2, y2, z2, this::occupy);
        return this;
    }

    /** Carves the position out of a solid world. */
    FakeSpaceProbe carve(int x, int y, int z) {
        marked.add(new BlockPos(x, y, z));
        return this;
    }

    /** Carves a 1x1 column of {@code height} blocks, starting at y. */
    FakeSpaceProbe carveColumn(int x, int y, int z, int height) {
        for (int i = 0; i < height; i++) {
            carve(x, y + i, z);
        }
        return this;
    }

    private interface Visitor {
        void visit(int x, int y, int z);
    }

    private static void forEachInBox(int x1, int y1, int z1, int x2, int y2, int z2, Visitor visitor) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    visitor.visit(x, y, z);
                }
            }
        }
    }

    @Override
    public boolean isAir(BlockPos pos) {
        return solid == marked.contains(pos);
    }

    @Override
    public boolean inBounds(BlockPos pos) {
        return pos.x() >= min.x() && pos.x() <= max.x()
                && pos.y() >= min.y() && pos.y() <= max.y()
                && pos.z() >= min.z() && pos.z() <= max.z();
    }
}
