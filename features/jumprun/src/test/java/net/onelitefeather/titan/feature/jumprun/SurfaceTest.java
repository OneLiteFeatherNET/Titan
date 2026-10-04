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
    void jumpRoomReachesFourBlocksAboveAFullTopAndOnlyThreeAboveAHalfBlock() {
        assertEquals(4, Surface.FULL.jumpRoomTop(), "full block");
        assertEquals(3, Surface.SLAB.jumpRoomTop(), "slab");
        assertEquals(4, Surface.FENCE.jumpRoomTop(), "fence");
        assertEquals(3, Surface.TRAPDOOR.jumpRoomTop(), "trapdoor");
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
    void shapesUnlockAtTheirScoreThresholds() {
        assertEquals(0, Mode.MEDIUM.minScore(Surface.FULL), "full block");
        assertEquals(10, Mode.MEDIUM.minScore(Surface.SLAB), "slab");
        assertEquals(10, Mode.MEDIUM.minScore(Surface.TRAPDOOR), "trapdoor");
        assertEquals(25, Mode.MEDIUM.minScore(Surface.FENCE), "fence");
        assertEquals(25, Mode.MEDIUM.minScore(Surface.PANE), "pane");
        assertEquals(40, Mode.MEDIUM.minScore(Surface.POST), "post");
    }

    @Test
    void onlyFullBlocksAreUnlockedBelowScoreTen() {
        assertEquals(List.of(Surface.FULL), Mode.MEDIUM.unlockedAt(0), "score 0");
        assertEquals(List.of(Surface.FULL), Mode.MEDIUM.unlockedAt(9), "score 9");
    }

    @Test
    void theGentleShapesUnlockAtTenTheNarrowOnesAtTwentyFiveAndTheNarrowestAtForty() {
        List<Surface> atTen = List.of(Surface.FULL, Surface.TRAPDOOR, Surface.SLAB, Surface.STAIRS, Surface.CARPET, Surface.SNOW);
        List<Surface> atTwentyFive = List.of(Surface.FULL, Surface.TRAPDOOR, Surface.SLAB, Surface.STAIRS, Surface.CARPET, Surface.SNOW, Surface.FENCE, Surface.PANE, Surface.HEAD, Surface.FLOWER_POT);

        assertEquals(atTen, Mode.MEDIUM.unlockedAt(10), "score 10");
        assertEquals(atTen, Mode.MEDIUM.unlockedAt(24), "score 24");
        assertEquals(atTwentyFive, Mode.MEDIUM.unlockedAt(25), "score 25");
        assertEquals(atTwentyFive, Mode.MEDIUM.unlockedAt(39), "score 39");
    }

    @Test
    void everyShapeIsUnlockedFromScoreForty() {
        assertEquals(List.of(Surface.values()), Mode.MEDIUM.unlockedAt(40), "score 40");
    }
}
