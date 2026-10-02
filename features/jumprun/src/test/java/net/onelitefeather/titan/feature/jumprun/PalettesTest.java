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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

class PalettesTest {

    @Test
    void everySurfaceHasAPalette() {
        for (Surface surface : Surface.values()) {
            assertFalse(surface.palette().isEmpty(), surface + " palette");
            assertEquals(surface.palette().size(), Set.copyOf(surface.palette()).size(), surface + " palette has no duplicates");
        }
    }

    @Test
    void noMaterialBelongsToTwoSurfaces() {
        Set<Block> seen = new HashSet<>();
        for (Surface surface : Surface.values()) {
            for (Block material : surface.palette()) {
                assertTrue(seen.add(material), material + " of " + surface + " is also in another palette");
            }
        }
    }

    @Test
    void fullBlockPaletteHasConcreteWoolAndTerracotta() {
        List<Block> palette = Surface.FULL.palette();

        assertEquals(33, palette.size(), "16 concrete, 16 wool, terracotta");
        assertTrue(palette.contains(Block.RED_CONCRETE) && palette.contains(Block.LIME_WOOL) && palette.contains(Block.TERRACOTTA), "samples");
    }

    @Test
    void trapdoorsAreClosedAndInTheLowerHalf() {
        for (Block material : Surface.TRAPDOOR.palette()) {
            assertEquals("bottom", material.getProperty("half"), material + " half");
            assertEquals("false", material.getProperty("open"), material + " open");
        }
        assertTrue(Surface.TRAPDOOR.palette().contains(Block.IRON_TRAPDOOR), "iron trapdoor");
    }

    @Test
    void slabsAreBottomSlabs() {
        for (Block material : Surface.SLAB.palette()) {
            assertEquals("bottom", material.getProperty("type"), material + " must sit in the lower half");
        }
    }

    @Test
    void fenceShapeIncludesWalls() {
        assertTrue(Surface.FENCE.palette().containsAll(List.of(Block.OAK_FENCE, Block.NETHER_BRICK_FENCE, Block.COBBLESTONE_WALL, Block.STONE_BRICK_WALL)), "fences and walls");
    }

    @Test
    void panePaletteHasStainedGlassPanesAndIronBars() {
        assertEquals(17, Surface.PANE.palette().size(), "16 colours and iron bars");
        assertTrue(Surface.PANE.palette().contains(Block.IRON_BARS), "iron bars");
    }

    @Test
    void postsStandUpright() {
        List<Block> palette = Surface.POST.palette();

        assertEquals(3, palette.size(), "end rod, chain, lightning rod");
        assertTrue(palette.contains(Block.END_ROD.withProperty("facing", "up")), "end rod");
        assertTrue(palette.contains(Block.LIGHTNING_ROD.withProperty("facing", "up")), "lightning rod");
        assertTrue(palette.stream().anyMatch(block -> "y".equals(block.getProperty("axis"))), "chain along y");
    }

    /** The material may change the look but never the standing height the rules rely on. */
    @Test
    void everyMaterialCollidesUpToItsSurfaceTop() {
        for (Surface surface : Surface.values()) {
            for (Block material : surface.palette()) {
                assertEquals(surface.top(), material.collisionShape().relativeEnd().y(), 1e-9, material + " must collide up to the top of " + surface);
            }
        }
    }

    @Test
    void everyPaletteResolvesToRealBlocks() {
        for (List<Block> palette : List.of(Palettes.full(), Palettes.trapdoor(), Palettes.slab(), Palettes.fence(), Palettes.pane(), Palettes.post())) {
            assertFalse(palette.isEmpty(), "palette is not empty");
            assertFalse(palette.contains(Block.AIR), "every name resolved to a block");
        }
    }
}
