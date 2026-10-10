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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;
import net.onelitefeather.titan.feature.jumprun.space.Direction;
import org.junit.jupiter.api.Test;

/**
 * Only standing on the target block counts as reaching a tower; the climb itself counts nothing.
 */
class ClimbProgressTest {

    private static final BlockPos START = new BlockPos(0, 64, 0);

    private static CourseBlock target(int height) {
        BlockPos pos = START.offset(1, height, 0);
        return new Spot(pos, Surface.FULL, Optional.of(new Climb(Direction.EAST, height, Climb.Kind.LADDER))).withMaterial(Block.STONE, Optional.empty(), Block.LADDER);
    }

    @Test
    void standingOnTheTargetBlockAtItsTopReachesIt() {
        for (int height = 3; height <= 5; height++) {
            CourseBlock target = target(height);
            double top = target.topY();

            assertTrue(target.supports(new Pos(1.5, top, 0.5)), "height " + height + ": feet on the target top");
            assertTrue(target.supports(new Pos(1.5, top + 0.02, 0.5)), "height " + height + ": a hair above the top still counts");
        }
    }

    @Test
    void standingHalfwayUpTheLadderDoesNotReachTheTarget() {
        for (int height = 3; height <= 5; height++) {
            CourseBlock target = target(height);
            double halfway = START.y() + 1 + (height - 1) / 2.0;

            assertFalse(target.supports(new Pos(0.5, halfway, 0.5)), "height " + height + ": halfway up the ladder column");
        }
    }

    @Test
    void standingJustBelowTheTargetTopDoesNotReachIt() {
        for (int height = 3; height <= 5; height++) {
            CourseBlock target = target(height);

            assertFalse(target.supports(new Pos(1.5, target.topY() - 0.2, 0.5)), "height " + height + ": 0.2 below the top");
        }
    }

    @Test
    void standingOnTheStartBlockDoesNotReachTheTarget() {
        for (int height = 3; height <= 5; height++) {
            CourseBlock target = target(height);

            assertFalse(target.supports(new Pos(0.5, START.y() + 1, 0.5)), "height " + height + ": still on the start block");
        }
    }
}
