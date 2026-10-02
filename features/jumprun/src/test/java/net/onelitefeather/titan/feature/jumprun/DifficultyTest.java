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

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.Test;

class DifficultyTest {

    private static RandomGenerator random(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    @Test
    void levelStartsAtZero() {
        assertEquals(0.0, Difficulty.level(0), "no score, no difficulty");
    }

    @Test
    void levelRisesStrictlyWithTheScore() {
        for (int score = 0; score < 1000; score++) {
            assertTrue(Difficulty.level(score + 1) > Difficulty.level(score), "level must rise at score " + score);
        }
    }

    @Test
    void levelStaysBelowOneForRealisticScores() {
        for (int score = 0; score <= 1000; score++) {
            assertTrue(Difficulty.level(score) < 1.0, "level must stay below 1 at score " + score);
        }
    }

    @Test
    void levelNeverExceedsOneEvenForAbsurdScores() {
        assertTrue(Difficulty.level(10_000) <= 1.0, "level is capped at 1");
    }

    @Test
    void levelReachesAboutHalfAtScoreFiftyFive() {
        // The scale doubled from 40 to 80 (D15), so the half-way point moved from 28 to 55.
        assertEquals(0.5, Difficulty.level(55), 0.01, "half difficulty near score 55");
    }

    @Test
    void targetCostStaysWithinZeroAndMaxCostForAnyScoreAndNoise() {
        RandomGenerator random = random(1L);
        for (int score = 0; score <= 500; score++) {
            for (int i = 0; i < 20; i++) {
                double target = Difficulty.targetCost(score, random);
                assertTrue(target >= 0.0 && target <= Jump.MAX_COST, "target " + target + " outside [0, " + Jump.MAX_COST + "] at score " + score);
            }
        }
    }

    @Test
    void targetCostNeverExceedsTheHardestJumpOfTheShapesUnlockedAtTheScore() {
        RandomGenerator random = random(3L);
        for (int score : new int[]{0, 9, 10, 24, 25, 39, 40}) {
            double hardest = Jump.maxCost(Surface.unlockedAt(score));
            for (int i = 0; i < 500; i++) {
                double target = Difficulty.targetCost(score, random);
                assertTrue(target <= hardest, "target " + target + " above " + hardest + " at score " + score);
            }
        }
    }

    @Test
    void theHardestJumpGrowsWithTheUnlockedShapes() {
        assertEquals(4.5, Jump.maxCost(Surface.unlockedAt(0)), "full blocks over the widest gap");
        assertEquals(6.5, Jump.maxCost(Surface.unlockedAt(10)), "slabs and trapdoors");
        assertEquals(10.5, Jump.maxCost(Surface.unlockedAt(25)), "panes");
        assertEquals(Jump.MAX_COST, Jump.maxCost(Surface.unlockedAt(40)), "posts");
    }

    @Test
    void targetCostAtScoreZeroStaysNearZero() {
        RandomGenerator random = random(7L);
        double sum = 0;
        int samples = 2000;
        for (int i = 0; i < samples; i++) {
            sum += Difficulty.targetCost(0, random);
        }
        assertTrue(sum / samples < 1.0, "mean target at score 0 was " + sum / samples);
    }

    @Test
    void targetCostFollowsTheLevelOnAverage() {
        RandomGenerator random = random(7L);
        double sum = 0;
        int samples = 2000;
        for (int i = 0; i < samples; i++) {
            sum += Difficulty.targetCost(10_000, random);
        }
        assertEquals(Jump.MAX_COST, sum / samples, 1.0, "mean target at a huge score approaches the max cost");
    }

    @Test
    void sameSeedGivesSameTargets() {
        assertEquals(Difficulty.targetCost(30, random(5L)), Difficulty.targetCost(30, random(5L)), "same seed");
    }
}
