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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minestom.server.instance.block.Block;

/**
 * The shapes a jump can land on. Each carries what the course logic needs: how high its walkable
 * top sits above the block position, how much harder it makes a jump, and the block states its
 * materials need to collide up to that top.
 */
enum Surface {
    FULL(1.0, 0, 0, Map.of()), TRAPDOOR(0.1875, 1, 10, Map.of("half", "bottom", "open", "false")), SLAB(0.5, 1, 10, Map.of("type", "bottom")), FENCE(1.5, 2, 25, Map.of()), PANE(1.0, 3, 25, Map.of()), POST(1.0, 4, 40, Map.of("facing", "up", "axis", "y"));

    /** The player stands 1.8 blocks tall; clear space above the top is measured with this. */
    private static final double PLAYER_HEIGHT = 1.8;

    private final double top;
    private final int typeCost;
    private final int minScore;
    private final Map<String, String> states;

    Surface(double top, int typeCost, int minScore, Map<String, String> states) {
        this.top = top;
        this.typeCost = typeCost;
        this.minScore = minScore;
        this.states = states;
    }

    /** The key of this shape below {@code jumprun.palettes}. */
    String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * The block as this shape shows it: the states that make it collide up to {@link #top()}
     * (lower half, closed, upright) are set, those the block does not have are left out.
     */
    Block shape(Block block) {
        Block shaped = block;
        for (Map.Entry<String, String> state : states.entrySet()) {
            if (shaped.properties().containsKey(state.getKey())) {
                shaped = shaped.withProperty(state.getKey(), state.getValue());
            }
        }
        return shaped;
    }

    /** Height of the walkable top above the block's own y (a fence collides up to 1.5). */
    double top() {
        return top;
    }

    int typeCost() {
        return typeCost;
    }

    /** The score from which this shape may appear, so the narrow ones come late. */
    int minScore() {
        return minScore;
    }

    /** The shapes that may appear once the score has reached {@code score}. */
    static List<Surface> unlockedAt(int score) {
        return Arrays.stream(values()).filter(surface -> surface.minScore <= score).toList();
    }

    /**
     * Highest block offset above the block position a standing player reaches into (fence: y+3).
     */
    int headroomTop() {
        return highestBlockReached(top);
    }

    /** The y of the highest block a player standing at the given walkable top reaches into. */
    static int highestBlockReached(double standingY) {
        return (int) Math.ceil(standingY + PLAYER_HEIGHT) - 1;
    }
}
