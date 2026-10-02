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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

class AscentPhaseTest {

    private static final CourseBlock START = new CourseBlock(new BlockPos(0, 10, 0), Surface.FULL);
    private static final Heading EAST = new Heading(1.0, 0.0);

    private static RandomGenerator seeded(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    /** The five ascent jumps, or fewer when the generator runs out of room. */
    private static List<CourseBlock> ascent(FakeSpaceProbe world, Heading heading, long seed) {
        CourseGenerator generator = new CourseGenerator(world, seeded(seed));
        List<CourseBlock> course = new ArrayList<>(List.of(START));
        Phase phase = Phase.start(heading);
        while (phase instanceof Phase.Ascent) {
            Optional<CourseBlock> next = generator.next(course, phase);
            if (next.isEmpty()) {
                break;
            }
            course.add(next.get());
            phase = phase.next();
        }
        return course;
    }

    // --- phase sequence ---------------------------------------------------------------------------

    @Test
    void startsWithFiveAscentJumps() {
        Phase phase = Phase.start(EAST);
        for (int i = 0; i < Phase.ASCENT_JUMPS; i++) {
            assertInstanceOf(Phase.Ascent.class, phase, "jump " + (i + 1) + " is still ascent");
            phase = phase.next();
        }
        assertEquals(new Phase.Scored(0), phase, "the sixth jump is the first scored one");
    }

    @Test
    void scoredPhaseCountsUp() {
        assertEquals(new Phase.Scored(8), new Phase.Scored(7).next(), "score");
    }

    @Test
    void ascentJumpsAreShortAndUpwardOnFullBlocks() {
        Phase ascent = Phase.start(EAST);

        assertEquals(List.of(1, 2), ascent.gaps().boxed().toList(), "gaps");
        assertEquals(List.of(1), ascent.rises().boxed().toList(), "rises");
        assertEquals(List.of(Surface.FULL), ascent.surfaces(), "surfaces");
    }

    @Test
    void scoredJumpsMayHaveAnyGapRiseAndSurface() {
        Phase scored = new Phase.Scored(0);

        assertEquals(List.of(1, 2, 3, 4), scored.gaps().boxed().toList(), "gaps");
        assertEquals(List.of(-1, 0, 1), scored.rises().boxed().toList(), "rises");
        assertEquals(List.of(Surface.values()), scored.surfaces(), "surfaces");
    }

    // --- the ascent itself ------------------------------------------------------------------------

    @Test
    void ascentIsFiveFullBlocksEachOneHigherWithAShortGap() {
        List<CourseBlock> course = ascent(new FakeSpaceProbe(), EAST, 1L);

        assertEquals(Phase.ASCENT_JUMPS + 1, course.size(), "start plus five jumps");
        for (int i = 1; i < course.size(); i++) {
            Jump jump = new Jump(course.get(i - 1), course.get(i));
            assertEquals(Surface.FULL, jump.to().surface(), "full block at jump " + i);
            assertEquals(1.0, jump.rise(), "one block up at jump " + i);
            assertTrue(jump.gap() >= 1 && jump.gap() <= 2, "gap 1-2 at jump " + i + " but " + jump.gap());
        }
    }

    @Test
    void ascentLeadsAwayFromTheSpawn() {
        for (long seed = 0; seed < 30; seed++) {
            List<CourseBlock> course = ascent(new FakeSpaceProbe(), EAST, seed);
            for (int i = 1; i < course.size(); i++) {
                int dx = course.get(i).pos().x() - course.get(i - 1).pos().x();
                assertTrue(dx > 0, "jump " + i + " must go east, away from the spawn (seed " + seed + "), dx " + dx);
            }
        }
    }

    @Test
    void ascentVariesItsDirectionAmongThoseAwayFromTheSpawn() {
        long sidewaysJumps = 0;
        for (long seed = 0; seed < 30; seed++) {
            List<CourseBlock> course = ascent(new FakeSpaceProbe(), EAST, seed);
            for (int i = 1; i < course.size(); i++) {
                sidewaysJumps += course.get(i).pos().z() != course.get(i - 1).pos().z() ? 1 : 0;
            }
        }
        assertTrue(sidewaysJumps > 0, "diagonal steps should occur too");
    }

    @Test
    void ascentTurnsAwayFromAWallInTheWayOfTheSpawnDirection() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(3, 0, -50, 50, 100, 50);

        List<CourseBlock> course = ascent(world, EAST, 5L);

        assertEquals(Phase.ASCENT_JUMPS + 1, course.size(), "still room to ascend");
        for (CourseBlock block : course) {
            assertTrue(block.pos().x() < 3, "block inside the wall: " + block.pos());
        }
    }

    @Test
    void ascentFailsUnderALowCeiling() {
        FakeSpaceProbe world = new FakeSpaceProbe().occupyBox(-50, 12, -50, 50, 12, 50);

        List<CourseBlock> course = ascent(world, EAST, 1L);

        assertEquals(1, course.size(), "not even the first jump fits under a ceiling two blocks above the start");
    }

    // --- heading ----------------------------------------------------------------------------------

    @Test
    void headingPointsFromTheSpawnToThePlayer() {
        Heading heading = Heading.away(10.0, 0.0, 0.0, 0.0, 0.0, 1.0);

        assertEquals(1.0, heading.x(), 1e-9, "x");
        assertEquals(0.0, heading.z(), 1e-9, "z");
    }

    @Test
    void headingIsNormalized() {
        Heading heading = Heading.away(3.0, 4.0, 0.0, 0.0, 0.0, 1.0);

        assertEquals(1.0, Math.hypot(heading.x(), heading.z()), 1e-9, "unit length");
        assertEquals(0.6, heading.x(), 1e-9, "x");
    }

    @Test
    void headingFallsBackToTheFacingDirectionOnTheSpawn() {
        Heading heading = Heading.away(5.0, 5.0, 5.0, 5.0, 0.0, -2.0);

        assertEquals(0.0, heading.x(), 1e-9, "x");
        assertEquals(-1.0, heading.z(), 1e-9, "z");
    }

    @Test
    void headingFallsBackToTheFacingDirectionNextToTheSpawn() {
        Heading heading = Heading.away(5.02, 5.0, 5.0, 5.0, 0.0, 1.0);

        assertEquals(1.0, heading.z(), 1e-9, "facing, not the 2 cm offset");
    }

    @Test
    void headingHasADefaultWhenNeitherOffsetNorFacingGiveADirection() {
        Heading heading = Heading.away(5.0, 5.0, 5.0, 5.0, 0.0, 0.0);

        assertEquals(1.0, Math.hypot(heading.x(), heading.z()), 1e-9, "still a unit vector");
    }

    @Test
    void headingDotProductWeighsDirections() {
        assertEquals(1.0, EAST.dot(Direction.EAST), 1e-9, "same direction");
        assertEquals(-1.0, EAST.dot(Direction.WEST), 1e-9, "opposite");
        assertEquals(0.0, EAST.dot(Direction.SOUTH), 1e-9, "sideways");
        assertEquals(Math.sqrt(0.5), EAST.dot(Direction.SOUTH_EAST), 1e-9, "diagonal is normalized");
    }
}
