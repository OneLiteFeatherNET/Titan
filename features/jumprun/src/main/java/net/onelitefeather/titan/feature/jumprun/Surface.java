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

import net.minestom.server.instance.block.Block;

/**
 * The block types a jump can land on. Each carries what the course logic needs: the block to
 * show, how high its walkable top sits above the block position, and how much harder it makes a
 * jump.
 */
enum Surface {
    FULL(Block.STONE, 1.0, 0),
    SLAB(Block.STONE_SLAB.withProperty("type", "bottom"), 0.5, 1),
    FENCE(Block.OAK_FENCE, 1.5, 2),
    PANE(Block.GLASS_PANE, 1.0, 3);

    /** The player stands 1.8 blocks tall; clear space above the top is measured with this. */
    private static final double PLAYER_HEIGHT = 1.8;

    private final Block block;
    private final double top;
    private final int typeCost;

    Surface(Block block, double top, int typeCost) {
        this.block = block;
        this.top = top;
        this.typeCost = typeCost;
    }

    Block block() {
        return block;
    }

    /** Height of the walkable top above the block's own y (a fence collides up to 1.5). */
    double top() {
        return top;
    }

    int typeCost() {
        return typeCost;
    }

    /** Highest block offset above the block position a standing player reaches into (fence: y+3). */
    int headroomTop() {
        return highestBlockReached(top);
    }

    /** The y of the highest block a player standing at the given walkable top reaches into. */
    static int highestBlockReached(double standingY) {
        return (int) Math.ceil(standingY + PLAYER_HEIGHT) - 1;
    }
}
