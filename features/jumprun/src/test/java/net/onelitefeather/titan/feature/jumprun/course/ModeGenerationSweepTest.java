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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * The seed-loop mode checks over the full seed ranges (12, 20 and 200 seeds). Tagged {@code sweep}:
 * they run in {@code sweepTest} on main and nightly, while {@link ModeGenerationTest} keeps the
 * same checks with a few seeds on every pull request.
 */
@Tag("sweep")
class ModeGenerationSweepTest {

    private static final long SHAPE_SEEDS = 12;
    private static final long TOWER_SEEDS = 20;
    private static final long LONG_SEEDS = 200;

    @Test
    void mediumNeverMakesANarrowSurface() {
        ModeWalkChecks.assertMediumNeverMakesANarrowSurface(SHAPE_SEEDS);
    }

    @Test
    void hardMakesNarrowSurfacesWithinTheWalk() {
        ModeWalkChecks.assertHardMakesNarrowSurfacesWithinTheWalk(SHAPE_SEEDS);
    }

    @Test
    void mediumReachesTheShapesWithAWideTopFaceWithinTheWalk() {
        ModeWalkChecks.assertMediumReachesTheShapesWithAWideTopFaceWithinTheWalk(SHAPE_SEEDS);
    }

    @Test
    void easyMakesNoNewShape() {
        ModeWalkChecks.assertEasyMakesNoNewShape(SHAPE_SEEDS);
    }

    @Test
    void noJumpOverstepsTheLimitsOfGapAndRiseWhateverTheShape() {
        ModeWalkChecks.assertNoJumpOverstepsTheLimitsOfGapAndRiseWhateverTheShape(SHAPE_SEEDS);
    }

    @Test
    void easyMakesNoTowerAtAnyScore() {
        ModeWalkChecks.assertEasyMakesNoTowerAtAnyScore(TOWER_SEEDS);
    }

    @Test
    void mediumMakesNoTowerBeforeScore30() {
        ModeWalkChecks.assertMediumMakesNoTowerBeforeScore30(TOWER_SEEDS);
    }

    @Test
    void mediumMakesTowersFromScore30() {
        ModeWalkChecks.assertMediumMakesTowersFromScore30(TOWER_SEEDS);
    }

    @Test
    void mediumMakesTowersAtScore80() {
        ModeWalkChecks.assertMediumMakesTowersAtScore80(TOWER_SEEDS);
    }

    @Test
    void hardMakesTheTallestTowerWithTheDefaultLimitsAtScoreEighty() {
        ModeWalkChecks.assertHardMakesTheTallestTowerWithTheDefaultLimitsAtScoreEighty(TOWER_SEEDS);
    }

    @Test
    void everyTowerIsStraightAndWithinTheDefaultHeights() {
        ModeWalkChecks.assertEveryTowerIsStraightAndWithinTheDefaultHeights(LONG_SEEDS);
    }

    @Test
    void noJumpCostsMoreThanTheMaximumWithTowers() {
        ModeWalkChecks.assertNoJumpCostsMoreThanTheMaximumWithTowers(LONG_SEEDS);
    }
}
