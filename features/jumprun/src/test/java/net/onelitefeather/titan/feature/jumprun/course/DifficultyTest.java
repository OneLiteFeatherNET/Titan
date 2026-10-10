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

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

class DifficultyTest {

    private static RandomGenerator random(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    @Test
    void levelStartsAtZero() {
        assertEquals(0.0, Difficulty.level(Mode.MEDIUM, 0), "no score, no difficulty");
    }

    @Test
    void levelRisesStrictlyWithTheScore() {
        for (int score = 0; score < 1000; score++) {
            assertTrue(Difficulty.level(Mode.MEDIUM, score + 1) > Difficulty.level(Mode.MEDIUM, score), "level must rise at score " + score);
        }
    }

    @Test
    void levelStaysBelowOneForRealisticScores() {
        for (int score = 0; score <= 1000; score++) {
            assertTrue(Difficulty.level(Mode.MEDIUM, score) < 1.0, "level must stay below 1 at score " + score);
        }
    }

    @Test
    void levelNeverExceedsOneEvenForAbsurdScores() {
        assertTrue(Difficulty.level(Mode.MEDIUM, 10_000) <= 1.0, "level is capped at 1");
    }

    @Test
    void levelReachesAboutHalfAtScoreFiftyFive() {
        // The scale doubled from 40 to 80 (D15), so the half-way point moved from 28 to 55.
        assertEquals(0.5, Difficulty.level(Mode.MEDIUM, 55), 0.01, "half difficulty near score 55");
    }

    @Test
    void targetCostStaysWithinZeroAndMaxCostForAnyScoreAndNoise() {
        RandomGenerator random = random(1L);
        for (int score = 0; score <= 500; score++) {
            for (int i = 0; i < 20; i++) {
                double target = Difficulty.targetCost(Mode.MEDIUM, score, random);
                assertTrue(target >= 0.0 && target <= Jump.MAX_COST, "target " + target + " outside [0, " + Jump.MAX_COST + "] at score " + score);
            }
        }
    }

    @Test
    void targetCostNeverExceedsTheHardestJumpOfTheShapesUnlockedAtTheScore() {
        RandomGenerator random = random(3L);
        for (int score : new int[]{0, 9, 10, 24, 25, 39, 40}) {
            double hardest = Jump.maxCost(Mode.MEDIUM, Mode.MEDIUM.unlockedAt(score));
            for (int i = 0; i < 500; i++) {
                double target = Difficulty.targetCost(Mode.MEDIUM, score, random);
                assertTrue(target <= hardest, "target " + target + " above " + hardest + " at score " + score);
            }
        }
    }

    @Test
    void theHardestJumpGrowsWithTheUnlockedShapes() {
        assertEquals(4.5, Jump.maxCost(Mode.MEDIUM, Mode.MEDIUM.unlockedAt(0)), "full blocks over the widest gap");
        assertEquals(6.5, Jump.maxCost(Mode.MEDIUM, Mode.MEDIUM.unlockedAt(10)), "slabs and trapdoors");
        assertEquals(6.5, Jump.maxCost(Mode.MEDIUM, Mode.MEDIUM.unlockedAt(25)), "no narrow shape in medium at 25");
        assertEquals(6.5, Jump.maxCost(Mode.MEDIUM, Mode.MEDIUM.unlockedAt(40)), "no post in medium at 40");
    }

    @Test
    void targetCostAtScoreZeroStaysNearZero() {
        RandomGenerator random = random(7L);
        double sum = 0;
        int samples = 2000;
        for (int i = 0; i < samples; i++) {
            sum += Difficulty.targetCost(Mode.MEDIUM, 0, random);
        }
        assertTrue(sum / samples < 1.0, "mean target at score 0 was " + sum / samples);
    }

    @Test
    void targetCostFollowsTheLevelOnAverage() {
        RandomGenerator random = random(7L);
        double sum = 0;
        int samples = 2000;
        for (int i = 0; i < samples; i++) {
            sum += Difficulty.targetCost(Mode.MEDIUM, 10_000, random);
        }
        assertEquals(Jump.maxCost(Mode.MEDIUM, Mode.MEDIUM.unlockedAt(10_000)), sum / samples, 1.0, "mean target at a huge score approaches the hardest medium jump");
    }

    @Test
    void towerHeightIsTheMinimumAtScoreZero() {
        assertEquals(3, Difficulty.towerHeight(Mode.MEDIUM, 0, 3, 8), "no score, the lowest tower");
    }

    @Test
    void towerHeightNeverDecreasesWithTheScore() {
        for (Mode mode : List.of(Mode.MEDIUM, Mode.HARD)) {
            int previous = Difficulty.towerHeight(mode, 0, 3, 8);
            for (int score = 1; score <= 300; score++) {
                int height = Difficulty.towerHeight(mode, score, 3, 8);
                assertTrue(height >= previous, mode + ": height " + height + " below " + previous + " at score " + score);
                previous = height;
            }
        }
    }

    @Test
    void towerHeightStaysWithinTheConfiguredLimits() {
        for (int score = 0; score <= 1000; score += 7) {
            int height = Difficulty.towerHeight(Mode.MEDIUM, score, 3, 5);
            assertTrue(height >= 3 && height <= 5, "height " + height + " outside 3 to 5 at score " + score);
        }
    }

    @Test
    void towerHeightIsClampedToTheMaximumWhenTheTargetIsHigher() {
        assertEquals(5, Difficulty.towerHeight(Mode.MEDIUM, 80, 3, 5), "target 7.9 is above a tower of 5 at 6.2");
    }

    @Test
    void towerCostFollowsTheTargetCostAtScoreForty() {
        assertTowerCostNearTarget(Mode.MEDIUM, 40);
    }

    @Test
    void towerCostFollowsTheTargetCostAtScoreEighty() {
        assertTowerCostNearTarget(Mode.MEDIUM, 80);
    }

    @Test
    void towerCostFollowsTheTargetCostAtScoreEightyInHard() {
        assertTowerCostNearTarget(Mode.HARD, 80);
    }

    @Test
    void towerCostAtScoreZeroIsTheCostOfTheLowestTower() {
        assertEquals(Jump.climbCost(3), Jump.climbCost(Difficulty.towerHeight(Mode.MEDIUM, 0, 3, 8)), 1e-9, "lowest tower at score 0");
    }

    /** The tower's cost is within half a height step of the target cost, with the limits of the config up to 8. */
    private static void assertTowerCostNearTarget(Mode mode, int score) {
        double target = Difficulty.level(mode, score) * Jump.maxCost(mode, mode.unlockedAt(score));
        double cost = Jump.climbCost(Difficulty.towerHeight(mode, score, 3, 8));
        assertEquals(target, cost, 1.05, mode + " at score " + score + ": tower cost " + cost + " vs target " + target);
    }

    @Test
    void sameSeedGivesSameTargets() {
        assertEquals(Difficulty.targetCost(Mode.MEDIUM, 30, random(5L)), Difficulty.targetCost(Mode.MEDIUM, 30, random(5L)), "same seed");
    }
}
