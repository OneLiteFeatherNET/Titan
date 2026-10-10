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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minestom.server.instance.block.Block;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;
import net.onelitefeather.titan.feature.jumprun.space.Direction;

/**
 * A climbing tower along one axis: a pillar and a ladder or vine beside the block the runner
 * starts from, ending in the target block on top. The client does the climbing; the server only
 * shows the cells.
 */
public record Climb(Direction direction, int height, Kind kind) {

    /** The heights a tower may have when the config says nothing else. */
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 7;

    /** The lowest and highest height the config may set for a tower, in blocks. */
    public static final int MIN_LIMIT = 2;
    public static final int MAX_LIMIT = 8;

    /** The directions a tower may lead in: the four axes. */
    static final List<Direction> DIRECTIONS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    /** The climbing block of a tower: a ladder, or a vine attached to the pillar. */
    public enum Kind {
        LADDER, VINE;

        /** The key of this block below {@code jumprun.palettes}. */
        public String configKey() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * The cells of the tower for the target block: the pillar under the target, and the climbing
     * cells in the column of the start block up to the level of the target. The climbing block
     * is set against the pillar.
     */
    List<Cell> cells(BlockPos target, Block pillar, Block climbing) {
        List<Cell> cells = new ArrayList<>();
        for (BlockPos pos : pillarPositions(target)) {
            cells.add(new Cell(pos, pillar));
        }
        Block set = kind == Kind.LADDER ? climbing.withProperty("facing", side(away())) : climbing.withProperty(side(direction), "true");
        for (BlockPos pos : climbingPositions(target)) {
            cells.add(new Cell(pos, set));
        }
        return cells;
    }

    /** Every cell the tower takes up in the real world, pillar and ladder or vine. */
    List<BlockPos> positions(BlockPos target) {
        List<BlockPos> positions = new ArrayList<>(pillarPositions(target));
        positions.addAll(climbingPositions(target));
        return positions;
    }

    /** The pillar: the cells under the target, from the first step up to just below it. */
    private List<BlockPos> pillarPositions(BlockPos target) {
        List<BlockPos> positions = new ArrayList<>();
        for (int y = baseOf(target).y() + 1; y < target.y(); y++) {
            positions.add(new BlockPos(target.x(), y, target.z()));
        }
        return positions;
    }

    /** The ladder or vine: the column of the start block, up to the level of the target. */
    private List<BlockPos> climbingPositions(BlockPos target) {
        BlockPos base = baseOf(target);
        List<BlockPos> positions = new ArrayList<>();
        for (int y = base.y() + 1; y <= target.y(); y++) {
            positions.add(new BlockPos(base.x(), y, base.z()));
        }
        return positions;
    }

    /** The block the runner starts from: the foot of the ladder column, one step back. */
    BlockPos baseOf(BlockPos target) {
        return target.offset(-direction.dx(), -height, -direction.dz());
    }

    /** The side a ladder faces: away from the pillar, which stands on the side of the direction. */
    private Direction away() {
        return Direction.toward(-direction.dx(), -direction.dz());
    }

    private static String side(Direction direction) {
        return switch (direction) {
            case NORTH -> "north";
            case EAST -> "east";
            case SOUTH -> "south";
            case WEST -> "west";
            default ->
                throw new IllegalArgumentException("a tower leads along an axis, not " + direction);
        };
    }
}
