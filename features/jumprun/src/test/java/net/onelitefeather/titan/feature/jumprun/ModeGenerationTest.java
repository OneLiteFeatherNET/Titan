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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

/** What the generator makes in each mode, over long walks with a fixed seed. */
class ModeGenerationTest {

    private static final CourseBlock SOURCE = TestBlocks.at(new BlockPos(0, 10, 0), Surface.FULL);
    private static final Heading EAST = new Heading(1.0, 0.0);
    private static final int JUMPS = 400;

    private record Move(int score, Jump jump) {
    }

    private static RandomGenerator seeded(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    private static List<Move> walk(Mode mode, long seed) {
        CourseGenerator generator = TestBlocks.generator(new FakeSpaceProbe(), TestBlocks.FAR_SPAWN, seeded(seed));
        List<CourseBlock> course = new ArrayList<>(List.of(SOURCE));
        List<Move> moves = new ArrayList<>();
        Phase phase = new Phase.Scored(0, EAST, mode);
        for (int score = 0; score < JUMPS; score++) {
            Optional<CourseBlock> next = generator.next(course, phase);
            if (next.isEmpty()) {
                break;
            }
            moves.add(new Move(score, new Jump(course.getLast(), next.get())));
            course.add(next.get());
            phase = generator.after(course, phase);
        }
        return moves;
    }

    @Test
    void easyOnlyEverMakesFullBlocksAndSlabsBeyondSixtyPoints() {
        List<Move> moves = walk(Mode.EASY, 1L);

        assertTrue(moves.size() > 60, "the walk goes past sixty points, got " + moves.size());
        for (Move move : moves) {
            Surface surface = move.jump().to().surface();
            assertTrue(surface == Surface.FULL || surface == Surface.SLAB, "easy made " + surface + " at score " + move.score());
        }
    }

    @Test
    void easyNeverLeavesAGapOfMoreThanTwo() {
        for (Move move : walk(Mode.EASY, 1L)) {
            assertTrue(move.jump().gap() <= 2, "gap " + move.jump().gap() + " at score " + move.score());
        }
    }

    @Test
    void easyMakesSlabsOnceTheyAreUnlocked() {
        assertTrue(walk(Mode.EASY, 1L).stream().anyMatch(move -> move.jump().to().surface() == Surface.SLAB), "slabs appear in easy");
    }

    @Test
    void hardMakesNoShapeBeforeItsThreshold() {
        for (Move move : walk(Mode.HARD, 2L)) {
            Surface surface = move.jump().to().surface();
            assertTrue(Mode.HARD.minScore(surface) <= move.score(), surface + " appeared at score " + move.score());
        }
    }

    @Test
    void hardReachesPostsWithinTheWalk() {
        assertTrue(walk(Mode.HARD, 2L).stream().anyMatch(move -> move.jump().to().surface() == Surface.POST), "posts appear in hard");
    }

    @Test
    void mediumDoesNotMakeAPostBeforeFortyPoints() {
        for (Move move : walk(Mode.MEDIUM, 3L)) {
            assertTrue(move.jump().to().surface() != Surface.POST || move.score() >= 40, "post at score " + move.score());
        }
    }

    @Test
    void mediumIsReproducibleFromTheSameSeed() {
        assertEquals(walk(Mode.MEDIUM, 5L), walk(Mode.MEDIUM, 5L), "same seed, same course");
    }

    @Test
    void hardIsHarderThanEasyOnAverage() {
        double easy = meanCostFrom(Mode.EASY, 40);
        double hard = meanCostFrom(Mode.HARD, 40);

        assertTrue(hard > easy, "hard " + hard + " must cost more than easy " + easy);
    }

    private static double meanCostFrom(Mode mode, int fromScore) {
        return walk(mode, 7L).stream().filter(move -> move.score() >= fromScore).mapToDouble(move -> move.jump().cost(mode)).average().orElseThrow();
    }
}
