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

import java.util.Random;
import net.minestom.server.instance.block.Block;
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
    void drawnMaterialBelongsToThePaletteAndIsReproducible() {
        for (Surface surface : Surface.values()) {
            Block first = surface.draw(new Random(7L));

            assertTrue(surface.palette().contains(first), surface + " draws from its own palette");
            assertEquals(first, surface.draw(new Random(7L)), surface + " same seed, same material");
        }
    }
}
