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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.minestom.server.instance.block.Block;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;
import net.onelitefeather.titan.feature.jumprun.space.Direction;
import org.junit.jupiter.api.Test;

class CourseBlockTowerTest {

    private static final BlockPos TARGET = new BlockPos(10, 67, 0);

    private static CourseBlock tower(int height) {
        Climb climb = new Climb(Direction.EAST, height, Climb.Kind.LADDER);
        return new Spot(TARGET, Surface.FULL, Optional.of(climb)).withMaterial(Block.STONE, Optional.empty(), Block.LADDER);
    }

    @Test
    void aTowerBlockCarriesAPillarAndALadderCellPerHeight() {
        for (int height = 3; height <= 5; height++) {
            assertEquals(2 * height - 1, tower(height).attachments().size(), "height " + height + ": pillar plus ladder cells");
        }
    }

    @Test
    void aPlainBlockHasNoAttachments() {
        assertTrue(TestBlocks.at(TARGET, Surface.FULL).attachments().isEmpty(), "no tower, no cells");
    }

    @Test
    void aTowerBlockShowsItsOwnCellAndItsAttachments() {
        CourseBlock block = tower(3);

        assertEquals(1 + block.attachments().size(), block.cells().size(), "block plus attachments");
        assertTrue(block.cells().contains(new Cell(TARGET, Block.STONE)), "the target block itself");
    }

    @Test
    void aBlockWithoutTowerIsNoClimbingPlacement() {
        assertFalse(TestBlocks.at(TARGET, Surface.FULL).climb().isPresent(), "plain block");
        assertTrue(tower(3).climb().isPresent(), "tower block");
    }

    @Test
    void redrawingATowerChangesThePillarAndKeepsTheLadder() {
        CourseBlock before = tower(3);
        Block other = TestBlocks.shipped().of(Surface.FULL).blocks().stream().filter(block -> block.id() != Block.STONE.id()).findFirst().orElseThrow();
        CourseBlock after = new CourseBlock(before.pos(), before.surface(), other, before.skin(), before.tower());

        List<Cell> ladderBefore = before.attachments().stream().filter(cell -> cell.block().id() == Block.LADDER.id()).toList();
        List<Cell> ladderAfter = after.attachments().stream().filter(cell -> cell.block().id() == Block.LADDER.id()).toList();
        List<Cell> pillarAfter = after.attachments().stream().filter(cell -> cell.block().id() == other.id()).toList();

        assertEquals(ladderBefore, ladderAfter, "the ladder stays");
        assertEquals(2, pillarAfter.size(), "the pillar takes the new material");
    }
}
