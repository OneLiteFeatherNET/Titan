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
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import net.minestom.server.coordinate.Pos;
import net.onelitefeather.titan.feature.jumprun.space.BlockPos;
import net.onelitefeather.titan.feature.jumprun.space.FakeSpaceProbe;
import net.onelitefeather.titan.feature.jumprun.space.Heading;
import org.junit.jupiter.api.Test;

/**
 * A course that reaches a tower: only the top counts, the climb counts nothing, a fall ends the
 * run.
 */
class CourseTowerTest {

    private static final BlockPos START = new BlockPos(0, 10, 0);
    private static final int SEEDS = 50;
    private static final int MAX_JUMPS = 200;

    private static RandomGenerator seeded(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    private static Pos standOn(CourseBlock block) {
        return new Pos(block.pos().x() + 0.5, block.topY(), block.pos().z() + 0.5);
    }

    /** The course of the first seed whose walk reaches a tower, stopped on the block before it. */
    private static Course courseBeforeATower() {
        for (long seed = 1; seed <= SEEDS; seed++) {
            Optional<Course> started = Course.startSteered(new Pos(START.x() + 0.5, START.y() + 1, START.z() + 0.5), START, new Heading(1.0, 0.0), TestBlocks.FAR_SPAWN, new FakeSpaceProbe(), TestBlocks.BAND, seeded(seed), TestBlocks.shipped(), PortalClearance.NONE, Mode.MEDIUM);
            if (started.isEmpty()) {
                continue;
            }
            Course course = started.get();
            for (int jump = 0; jump < MAX_JUMPS; jump++) {
                Optional<CourseBlock> next = course.next();
                if (next.isEmpty()) {
                    break;
                }
                if (next.get().climb().isPresent()) {
                    return course;
                }
                course.advanceTo(standOn(next.get()));
            }
        }
        throw new AssertionError("no seed in 1 to " + SEEDS + " reached a tower within " + MAX_JUMPS + " jumps");
    }

    @Test
    void reachingTheTopOfATowerCountsOneJump() {
        Course course = courseBeforeATower();
        CourseBlock tower = course.next().orElseThrow();

        Course.Advance advance = course.advanceTo(standOn(tower));

        assertEquals(1, advance.jumps(), "the tower is one jump, whatever its height");
        assertEquals(1, advance.scored(), "the tower scores once");
    }

    @Test
    void standingOnTheTopBelowTheLevelOfTheTargetDoesNotCount() {
        Course course = courseBeforeATower();
        CourseBlock tower = course.next().orElseThrow();
        Pos justBelow = new Pos(tower.pos().x() + 0.5, tower.topY() - 0.2, tower.pos().z() + 0.5);

        assertEquals(0, course.advanceTo(justBelow).jumps(), "0.2 below the top is not on the block");
    }

    @Test
    void climbingTheLadderCountsNothing() {
        Course course = courseBeforeATower();
        CourseBlock start = course.current();
        CourseBlock tower = course.next().orElseThrow();
        int height = tower.climb().orElseThrow().height();
        int scoreBefore = course.score();

        for (int step = 1; step < height; step++) {
            Pos halfway = new Pos(start.pos().x() + 0.5, start.topY() + step, start.pos().z() + 0.5);
            assertEquals(0, course.advanceTo(halfway).jumps(), "halfway up, step " + step);
        }
        assertEquals(scoreBefore, course.score(), "the climb leaves the score unchanged");
    }

    @Test
    void theRunDoesNotEndWhileClimbingAboveTheStartBlock() {
        Course course = courseBeforeATower();
        CourseBlock start = course.current();
        CourseBlock tower = course.next().orElseThrow();

        assertFalse(course.hasFallen(start.topY() + 1.0), "halfway up the ladder");
        assertFalse(course.hasFallen(tower.topY() - 0.2), "just under the top");
    }

    @Test
    void fallingFromTheTowerEndsTheRun() {
        Course course = courseBeforeATower();
        CourseBlock start = course.current();

        assertTrue(course.hasFallen(start.topY() - 3.01), "below the threshold of a fall from the tower");
    }

    @Test
    void theWindowShowsTheTowerWithItsAttachmentsAndCountsNoAttachmentAsABlock() {
        Course course = courseBeforeATower();
        CourseBlock tower = course.next().orElseThrow();
        int height = tower.climb().orElseThrow().height();

        assertEquals(2 * height - 1, tower.attachments().size(), "pillar and ladder cells");
        assertTrue(course.window().contains(tower), "the tower is in the window");
        assertTrue(course.window().size() <= 5, "the window holds blocks only, not their attachments");
    }

    @Test
    void standingOnTheStartBlockKeepsTheRerollCountingAndTheLadderDoesNot() {
        Course course = courseBeforeATower();
        CourseBlock start = course.current();
        int height = course.next().orElseThrow().climb().orElseThrow().height();

        assertTrue(course.standsOnCurrent(new Pos(start.pos().x() + 0.5, start.topY(), start.pos().z() + 0.5)), "on the start block");
        assertFalse(course.standsOnCurrent(new Pos(start.pos().x() + 0.5, start.topY() + height - 1, start.pos().z() + 0.5)), "on the ladder, above the start block");
    }

    @Test
    void recolouringATowerKeepsItsLadder() {
        Course course = courseBeforeATower();
        CourseBlock tower = course.next().orElseThrow();
        List<Cell> climbingBefore = climbingCells(tower);

        CourseBlock recoloured = course.recolor(List.of(tower)).getFirst();

        assertEquals(climbingBefore, climbingCells(recoloured), "the ladder or vine stays");
        assertTrue(pillarCells(recoloured).stream().allMatch(cell -> cell.block().id() == recoloured.material().id()), "the pillar takes the new material of the target: " + recoloured.material().name());
    }

    /** The cells outside the target's column: the ladder or vine. */
    private static List<Cell> climbingCells(CourseBlock tower) {
        return tower.attachments().stream().filter(cell -> cell.pos().x() != tower.pos().x() || cell.pos().z() != tower.pos().z()).toList();
    }

    /** The cells in the target's column below it: the pillar. */
    private static List<Cell> pillarCells(CourseBlock tower) {
        return tower.attachments().stream().filter(cell -> cell.pos().x() == tower.pos().x() && cell.pos().z() == tower.pos().z()).toList();
    }
}
