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

import java.util.List;
import org.junit.jupiter.api.Test;

class SurfaceTest {

    @Test
    void fullBlockHasTopOfOneAndNoTypeCost() {
        assertEquals(1.0, Surface.FULL.top(), "full block top");
        assertEquals(0, Surface.FULL.typeCost(), "full block type cost");
    }

    @Test
    void slabHasTopOfHalfAndTypeCostOfOne() {
        assertEquals(0.5, Surface.SLAB.top(), "slab top");
        assertEquals(1, Surface.SLAB.typeCost(), "slab type cost");
    }

    @Test
    void fenceHasTopOfOneAndAHalfAndTypeCostOfTwo() {
        assertEquals(1.5, Surface.FENCE.top(), "fence top");
        assertEquals(2, Surface.FENCE.typeCost(), "fence type cost");
    }

    @Test
    void paneHasTopOfOneAndTypeCostOfThree() {
        assertEquals(1.0, Surface.PANE.top(), "pane top");
        assertEquals(3, Surface.PANE.typeCost(), "pane type cost");
    }

    @Test
    void headroomReachesTwoBlocksAboveAFlatTopAndThreeAboveAFence() {
        assertEquals(2, Surface.FULL.headroomTop(), "full block");
        assertEquals(2, Surface.SLAB.headroomTop(), "slab");
        assertEquals(3, Surface.FENCE.headroomTop(), "fence");
        assertEquals(2, Surface.PANE.headroomTop(), "pane");
    }

    @Test
    void trapdoorHasTopOfOneSixteenthTimesThreeAndCostOfOne() {
        assertEquals(0.1875, Surface.TRAPDOOR.top(), "trapdoor top");
        assertEquals(1, Surface.TRAPDOOR.typeCost(), "trapdoor type cost");
    }

    @Test
    void postHasTopOfOneAndTheHighestTypeCost() {
        assertEquals(1.0, Surface.POST.top(), "post top");
        assertEquals(4, Surface.POST.typeCost(), "post type cost");
    }

    @Test
    void headroomOfTheNewShapes() {
        assertEquals(1, Surface.TRAPDOOR.headroomTop(), "trapdoor");
        assertEquals(2, Surface.POST.headroomTop(), "post");
    }

    @Test
    void shapesUnlockAtTheirScoreThresholds() {
        assertEquals(0, Surface.FULL.minScore(), "full block");
        assertEquals(10, Surface.SLAB.minScore(), "slab");
        assertEquals(10, Surface.TRAPDOOR.minScore(), "trapdoor");
        assertEquals(25, Surface.FENCE.minScore(), "fence");
        assertEquals(25, Surface.PANE.minScore(), "pane");
        assertEquals(40, Surface.POST.minScore(), "post");
    }

    @Test
    void onlyFullBlocksAreUnlockedBelowScoreTen() {
        assertEquals(List.of(Surface.FULL), Surface.unlockedAt(0), "score 0");
        assertEquals(List.of(Surface.FULL), Surface.unlockedAt(9), "score 9");
    }

    @Test
    void slabAndTrapdoorUnlockAtTenAndFenceAndPaneAtTwentyFive() {
        assertEquals(List.of(Surface.FULL, Surface.TRAPDOOR, Surface.SLAB), Surface.unlockedAt(10), "score 10");
        assertEquals(List.of(Surface.FULL, Surface.TRAPDOOR, Surface.SLAB), Surface.unlockedAt(24), "score 24");
        assertEquals(List.of(Surface.FULL, Surface.TRAPDOOR, Surface.SLAB, Surface.FENCE, Surface.PANE), Surface.unlockedAt(25), "score 25");
    }

    @Test
    void everyShapeIsUnlockedFromScoreForty() {
        assertEquals(List.of(Surface.values()), Surface.unlockedAt(40), "score 40");
    }
}
