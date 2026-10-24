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

import static net.onelitefeather.titan.feature.jumprun.course.ModeWalks.Move;
import static net.onelitefeather.titan.feature.jumprun.course.ModeWalks.isTower;
import static net.onelitefeather.titan.feature.jumprun.course.ModeWalks.walk;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The mode checks that need many seeds to see a rare shape or tower. Each method takes the seed
 * count, so the PR set ({@link ModeGenerationTest}) and the sweep ({@link ModeGenerationSweepTest})
 * assert the same property over different ranges.
 */
final class ModeWalkChecks {

    private static final List<Surface> NEW_SHAPES = List.of(Surface.STAIRS, Surface.CARPET, Surface.SNOW, Surface.HEAD, Surface.FLOWER_POT, Surface.CANDLE);
    /**
     * The shapes with a top face narrower than a full block, or a thin post with a whole-cell
     * collision.
     */
    private static final List<Surface> NARROW = List.of(Surface.FENCE, Surface.PANE, Surface.POST, Surface.HEAD, Surface.FLOWER_POT, Surface.CANDLE);

    private ModeWalkChecks() {
    }

    static void assertMediumNeverMakesANarrowSurface(long seeds) {
        for (long seed = 1; seed <= seeds; seed++) {
            for (Move move : walk(Mode.MEDIUM, seed)) {
                assertFalse(NARROW.contains(move.jump().to().surface()), "seed " + seed + " made " + move.jump().to().surface() + " in medium at score " + move.score());
            }
        }
    }

    static void assertHardMakesNarrowSurfacesWithinTheWalk(long seeds) {
        Set<Surface> seen = new HashSet<>();
        for (long seed = 1; seed <= seeds; seed++) {
            walk(Mode.HARD, seed).forEach(move -> seen.add(move.jump().to().surface()));
        }

        assertTrue(seen.containsAll(NARROW), "hard made only " + seen);
    }

    static void assertMediumReachesTheShapesWithAWideTopFaceWithinTheWalk(long seeds) {
        Set<Surface> seen = new HashSet<>();
        for (long seed = 1; seed <= seeds; seed++) {
            walk(Mode.MEDIUM, seed).forEach(move -> seen.add(move.jump().to().surface()));
        }

        assertTrue(seen.containsAll(List.of(Surface.STAIRS, Surface.CARPET, Surface.SNOW)), "medium made only " + seen);
    }

    static void assertEasyMakesNoNewShape(long seeds) {
        for (long seed = 1; seed <= seeds; seed++) {
            for (Move move : walk(Mode.EASY, seed)) {
                assertTrue(!NEW_SHAPES.contains(move.jump().to().surface()), "seed " + seed + " made " + move.jump().to().surface() + " in easy");
            }
        }
    }

    static void assertNoJumpOverstepsTheLimitsOfGapAndRiseWhateverTheShape(long seeds) {
        for (Mode mode : Mode.values()) {
            for (long seed = 1; seed <= seeds; seed++) {
                for (Move move : walk(mode, seed)) {
                    assertTrue(JumpRules.isReachable(move.jump(), mode), mode + " seed " + seed + " score " + move.score() + ": " + move.jump());
                }
            }
        }
    }

    static void assertEasyMakesNoTowerAtAnyScore(long seeds) {
        for (long seed = 1; seed <= seeds; seed++) {
            for (Move move : walk(Mode.EASY, seed)) {
                assertFalse(isTower(move.jump()), "easy seed " + seed + " made a tower at score " + move.score());
            }
        }
    }

    static void assertMediumMakesNoTowerBeforeScore30(long seeds) {
        for (long seed = 1; seed <= seeds; seed++) {
            for (Move move : walk(Mode.MEDIUM, seed)) {
                assertFalse(move.score() < 30 && isTower(move.jump()), "medium seed " + seed + " made a tower at score " + move.score());
            }
        }
    }

    static void assertMediumMakesTowersFromScore30(long seeds) {
        long towers = 0;
        for (long seed = 1; seed <= seeds; seed++) {
            towers += walk(Mode.MEDIUM, seed).stream().filter(move -> move.score() >= 30 && isTower(move.jump())).count();
        }
        assertTrue(towers > 0, "medium at score 30 and above must make towers");
    }

    static void assertMediumMakesTowersAtScore80(long seeds) {
        long towers = 0;
        for (long seed = 1; seed <= seeds; seed++) {
            towers += walk(Mode.MEDIUM, seed).stream().filter(move -> move.score() >= 80 && isTower(move.jump())).count();
        }
        assertTrue(towers > 0, "medium at score 80 must make towers, made " + towers);
    }

    static void assertHardMakesTheTallestTowerWithTheDefaultLimitsAtScoreEighty(long seeds) {
        long tallest = 0;
        for (long seed = 1; seed <= seeds; seed++) {
            tallest += walk(Mode.HARD, seed).stream().filter(move -> move.score() >= 80 && isTower(move.jump()) && move.jump().to().climb().orElseThrow().height() == Climb.MAX_HEIGHT).count();
        }
        assertTrue(tallest > 0, "hard at score 80 must make towers of " + Climb.MAX_HEIGHT + ", made " + tallest);
    }

    static void assertEveryTowerIsStraightAndWithinTheDefaultHeights(long seeds) {
        for (long seed = 1; seed <= seeds; seed++) {
            for (Mode mode : List.of(Mode.MEDIUM, Mode.HARD)) {
                for (Move move : walk(mode, seed)) {
                    if (isTower(move.jump())) {
                        int height = move.jump().to().climb().orElseThrow().height();
                        assertEquals(0, move.jump().gap(), mode + " seed " + seed + ": tower gap");
                        assertTrue(height >= Climb.MIN_HEIGHT && height <= Climb.MAX_HEIGHT, mode + " seed " + seed + ": tower height " + height);
                    }
                }
            }
        }
    }

    static void assertNoJumpCostsMoreThanTheMaximumWithTowers(long seeds) {
        for (long seed = 1; seed <= seeds; seed++) {
            for (Move move : walk(Mode.HARD, seed)) {
                assertTrue(move.jump().cost(Mode.HARD) <= Jump.MAX_COST, "hard seed " + seed + ": " + move.jump().cost(Mode.HARD) + " above " + Jump.MAX_COST);
            }
        }
    }
}
