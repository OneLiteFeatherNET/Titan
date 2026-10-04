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
import java.util.Locale;
import java.util.Map;
import java.util.random.RandomGenerator;
import net.minestom.server.instance.block.Block;

/**
 * The shapes a jump can land on. Each carries what the course logic needs: the steps a player can
 * stand on, how much harder it makes a jump, and the block states its materials need to collide
 * like that.
 */
enum Surface {
    /** A whole block. */
    FULL(0, Map.of(), cell(1.0)),
    /** A closed trapdoor in the lower half. */
    TRAPDOOR(1, Map.of("half", "bottom", "open", "false"), cell(0.1875)),
    /** A slab in the lower half. */
    SLAB(1, Map.of("type", "bottom"), cell(0.5)),
    /** Straight stairs in the lower half: the high step first, then the low one. */
    STAIRS(1, Map.of("half", "bottom", "shape", "straight", "waterlogged", "false"), new Step(1.0, 0, 1, 0, 0.5), new Step(0.5, 0, 1, 0.5, 1)),
    /** A carpet, 1/16 high. */
    CARPET(1, Map.of(), cell(0.0625)),
    /** Three layers of snow, which collide up to a quarter. */
    SNOW(1, Map.of("layers", "3"), cell(0.25)),
    /** A fence or wall, which collides up to 1.5. */
    FENCE(2, Map.of(), cell(1.5)),
    /** A glass pane or iron bars, lenient like the whole cell. */
    PANE(3, Map.of(), cell(1.0)),
    /** A head on the floor. */
    HEAD(2, Map.of(), new Step(0.5, 0.25, 0.75, 0.25, 0.75)),
    /** A flower pot, empty or planted. */
    FLOWER_POT(2, Map.of(), new Step(0.375, 0.3125, 0.6875, 0.3125, 0.6875)),
    /** One candle that is not lit. */
    CANDLE(3, Map.of("candles", "1", "lit", "false", "waterlogged", "false"), new Step(0.375, 0.4375, 0.5625, 0.4375, 0.5625)),
    /** An upright rod or chain. */
    POST(4, Map.of("facing", "up", "axis", "y"), cell(1.0));

    /** The player stands 1.8 blocks tall; clear space above the top is measured with this. */
    private static final double PLAYER_HEIGHT = 1.8;

    /**
     * How high a standing jump lifts the feet (vanilla, no jump boost). Minestom does not simulate
     * jumps, the client collides, so the course needs its own value.
     */
    static final double JUMP_HEIGHT = 1.2522;

    private static final List<String> FACINGS = List.of("north", "east", "south", "west");
    private static final int SKULL_ROTATIONS = 16;

    /** The steps with the high step first, as the shape lies when it faces north. */
    private final List<Step> steps;
    private final double top;
    private final double lowTop;
    private final int typeCost;
    private final Map<String, String> states;

    Surface(int typeCost, Map<String, String> states, Step... steps) {
        this.steps = List.of(steps);
        this.top = this.steps.stream().mapToDouble(Step::top).max().orElseThrow();
        this.lowTop = this.steps.stream().mapToDouble(Step::top).min().orElseThrow();
        this.typeCost = typeCost;
        this.states = states;
    }

    /** A step over the whole cell, which is how the shapes without a narrow footprint count. */
    private static Step cell(double top) {
        return new Step(top, 0, 1, 0, 1);
    }

    /** The key of this shape below {@code jumprun.palettes}. */
    String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * The block as this shape shows it: the states that make it collide like the shape (lower
     * half, closed, upright) are set, those the block does not have are left out.
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

    /**
     * The steps a material of this shape offers; stairs turn with the {@code facing} of the
     * material.
     */
    List<Step> steps(Block material) {
        if (this != STAIRS) {
            return steps;
        }
        int quarterTurns = FACINGS.indexOf(material.getProperty("facing"));
        List<Step> turned = steps;
        for (int turn = 0; turn < quarterTurns; turn++) {
            turned = turned.stream().map(Step::turnedClockwise).toList();
        }
        return turned;
    }

    /**
     * Whether materials of this shape must collide over exactly the footprint of the steps. The
     * shapes that count as the whole cell are lenient, so their collision is not compared.
     */
    boolean hasNarrowFootprint() {
        Step hull = footprint();
        return hull.minX() > 0 || hull.maxX() < 1 || hull.minZ() > 0 || hull.maxZ() < 1;
    }

    /** The rectangle in the cell that all steps lie in; its top is the highest step. */
    Step footprint() {
        return new Step(top, steps.stream().mapToDouble(Step::minX).min().orElseThrow(), steps.stream().mapToDouble(Step::maxX).max().orElseThrow(), steps.stream().mapToDouble(Step::minZ).min().orElseThrow(), steps.stream().mapToDouble(Step::maxZ).max().orElseThrow());
    }

    /**
     * The material with its look drawn at random, which never changes how it collides: the way
     * stairs face and the turn of a head.
     */
    Block varied(Block material, RandomGenerator random) {
        return switch (this) {
            case STAIRS ->
                withProperty(material, "facing", FACINGS.get(random.nextInt(FACINGS.size())));
            case HEAD ->
                withProperty(material, "rotation", Integer.toString(random.nextInt(SKULL_ROTATIONS)));
            default -> material;
        };
    }

    /** The look {@link #varied} drew for {@code old}, put on another material of the shape. */
    Block withLookOf(Block old, Block material) {
        return switch (this) {
            case STAIRS -> withProperty(material, "facing", old.getProperty("facing"));
            case HEAD -> withProperty(material, "rotation", old.getProperty("rotation"));
            default -> material;
        };
    }

    private static Block withProperty(Block block, String name, String value) {
        return block.properties().containsKey(name) && value != null ? block.withProperty(name, value) : block;
    }

    /** Height of the highest walkable top above the block's own y (a fence collides up to 1.5). */
    double top() {
        return top;
    }

    /** Height of the lowest step: the one a runner can take off from or fall past. */
    double lowTop() {
        return lowTop;
    }

    int typeCost() {
        return typeCost;
    }

    /**
     * Highest block offset above the block position the head reaches at the apex of a jump from
     * here: a ceiling any lower lets the player stand but not jump.
     */
    int jumpRoomTop() {
        return highestBlockReached(top + JUMP_HEIGHT);
    }

    /** The y of the highest block a player standing at the given walkable top reaches into. */
    static int highestBlockReached(double standingY) {
        return (int) Math.ceil(standingY + PLAYER_HEIGHT) - 1;
    }
}
