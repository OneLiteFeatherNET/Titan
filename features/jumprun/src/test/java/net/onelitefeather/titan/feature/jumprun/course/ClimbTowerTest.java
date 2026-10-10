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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;
import net.minestom.server.instance.block.Block;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;
import net.onelitefeather.titan.feature.jumprun.space.Direction;
import org.junit.jupiter.api.Test;

class ClimbTowerTest {

    private static final Direction[] AXES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final BlockPos BASE = new BlockPos(0, 64, 0);

    private static BlockPos targetOf(Direction direction, int height) {
        return BASE.offset(direction.dx(), height, direction.dz());
    }

    private static List<Cell> cellsOf(Direction direction, int height, Climb.Kind kind) {
        Block climbing = kind == Climb.Kind.LADDER ? Block.LADDER : Block.VINE;
        return new Climb(direction, height, kind).cells(targetOf(direction, height), Block.STONE, climbing);
    }

    private static List<Cell> ofBlock(List<Cell> cells, Block block) {
        return cells.stream().filter(cell -> cell.block().id() == block.id()).toList();
    }

    @Test
    void aTowerHasOneLadderCellPerHeightAndOnePillarCellLessThanHeight() {
        for (Direction direction : AXES) {
            for (int height = 3; height <= 5; height++) {
                List<Cell> cells = cellsOf(direction, height, Climb.Kind.LADDER);

                assertEquals(height, ofBlock(cells, Block.LADDER).size(), direction + " height " + height + ": ladder cells");
                assertEquals(height - 1, ofBlock(cells, Block.STONE).size(), direction + " height " + height + ": pillar cells");
            }
        }
    }

    @Test
    void theLadderStandsInTheColumnOfTheStartBlock() {
        for (Direction direction : AXES) {
            for (Cell ladder : ofBlock(cellsOf(direction, 4, Climb.Kind.LADDER), Block.LADDER)) {
                assertEquals(BASE.x(), ladder.pos().x(), direction + ": ladder x");
                assertEquals(BASE.z(), ladder.pos().z(), direction + ": ladder z");
            }
        }
    }

    @Test
    void theTopLadderCellIsLevelWithTheTarget() {
        for (Direction direction : AXES) {
            for (int height = 3; height <= 5; height++) {
                BlockPos top = ofBlock(cellsOf(direction, height, Climb.Kind.LADDER), Block.LADDER).stream().map(Cell::pos).max(Comparator.comparingInt(BlockPos::y)).orElseThrow();

                assertEquals(targetOf(direction, height).y(), top.y(), direction + " height " + height + ": top ladder cell level with the target");
            }
        }
    }

    @Test
    void thePillarStandsUnderTheTargetFromTheFirstStepUpToBelowIt() {
        for (Direction direction : AXES) {
            BlockPos target = targetOf(direction, 3);
            List<Cell> pillar = ofBlock(cellsOf(direction, 3, Climb.Kind.LADDER), Block.STONE);

            assertTrue(pillar.stream().allMatch(cell -> cell.pos().x() == target.x() && cell.pos().z() == target.z()), direction + ": pillar column");
            assertEquals(List.of(BASE.y() + 1, BASE.y() + 2), pillar.stream().map(cell -> cell.pos().y()).sorted().toList(), direction + ": pillar heights");
        }
    }

    @Test
    void aLadderFacesAwayFromItsPillar() {
        for (Direction direction : AXES) {
            Cell ladder = ofBlock(cellsOf(direction, 3, Climb.Kind.LADDER), Block.LADDER).getFirst();

            assertEquals(side(Direction.toward(-direction.dx(), -direction.dz())), ladder.block().getProperty("facing"), direction + ": ladder facing");
        }
    }

    @Test
    void aVineIsAttachedOnlyOnThePillarSide() {
        for (Direction direction : AXES) {
            Cell vine = ofBlock(cellsOf(direction, 3, Climb.Kind.VINE), Block.VINE).getFirst();

            assertEquals("true", vine.block().getProperty(side(direction)), direction + ": vine attached to the pillar");
            assertEquals("false", vine.block().getProperty(side(Direction.toward(-direction.dx(), -direction.dz()))), direction + ": vine not attached to the far side");
        }
    }

    private static String side(Direction direction) {
        return switch (direction) {
            case NORTH -> "north";
            case EAST -> "east";
            case SOUTH -> "south";
            case WEST -> "west";
            default -> throw new IllegalArgumentException("not an axis: " + direction);
        };
    }
}
