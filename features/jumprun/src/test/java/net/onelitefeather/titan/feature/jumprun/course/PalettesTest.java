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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

/** The palettes the module ships with, and how a palette draws. */
class PalettesTest {

    private static final int DRAWS = 4000;

    private static final Palettes SHIPPED = TestBlocks.shipped();

    private static RandomGenerator seeded(long seed) {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    }

    private static List<Block> shipped(Surface surface) {
        return SHIPPED.of(surface).blocks();
    }

    @Test
    void everySurfaceHasAPalette() {
        for (Surface surface : Surface.values()) {
            assertFalse(shipped(surface).isEmpty(), surface + " palette");
            assertEquals(shipped(surface).size(), Set.copyOf(shipped(surface)).size(), surface + " palette has no duplicates");
        }
    }

    @Test
    void noMaterialBelongsToTwoSurfaces() {
        Set<Block> seen = new HashSet<>();
        for (Surface surface : Surface.values()) {
            for (Block material : shipped(surface)) {
                assertTrue(seen.add(material), material + " of " + surface + " is also in another palette");
            }
        }
    }

    @Test
    void fullBlockPaletteHasConcreteWoolAndTerracotta() {
        List<Block> palette = shipped(Surface.FULL);

        assertEquals(33, palette.size(), "16 concrete, 16 wool, terracotta");
        assertTrue(palette.contains(Block.RED_CONCRETE) && palette.contains(Block.LIME_WOOL) && palette.contains(Block.TERRACOTTA), "samples");
    }

    @Test
    void theNewShapesShipTheirMaterials() {
        assertEquals(19, shipped(Surface.STAIRS).size(), "12 woods and 7 stone kinds");
        assertEquals(17, shipped(Surface.CARPET).size(), "16 colours and moss");
        assertEquals(1, shipped(Surface.SNOW).size(), "snow");
        assertEquals(5, shipped(Surface.HEAD).size(), "five floor heads");
        assertEquals(7, shipped(Surface.FLOWER_POT).size(), "empty and six planted");
        assertEquals(17, shipped(Surface.CANDLE).size(), "plain and 16 colours");
    }

    @Test
    void aMaterialOtherThanTheDrawnOneIsFoundEvenWithAnotherFacing() {
        Palette stairs = SHIPPED.of(Surface.STAIRS);
        Block east = stairs.blocks().getFirst().withProperty("facing", "east");
        RandomGenerator random = seeded(5L);

        for (int i = 0; i < 200; i++) {
            assertTrue(stairs.drawOther(east, random).id() != east.id(), "another type than the east facing one");
        }
    }

    @Test
    void trapdoorsAreClosedAndInTheLowerHalf() {
        assertEquals(13, shipped(Surface.TRAPDOOR).size(), "12 woods and iron");
        for (Block material : shipped(Surface.TRAPDOOR)) {
            assertEquals("bottom", material.getProperty("half"), material + " half");
            assertEquals("false", material.getProperty("open"), material + " open");
        }
        assertTrue(shipped(Surface.TRAPDOOR).contains(Block.IRON_TRAPDOOR.withProperty("half", "bottom").withProperty("open", "false")), "iron trapdoor");
    }

    @Test
    void slabsAreBottomSlabs() {
        assertEquals(18, shipped(Surface.SLAB).size(), "6 stone kinds and 12 woods");
        for (Block material : shipped(Surface.SLAB)) {
            assertEquals("bottom", material.getProperty("type"), material + " must sit in the lower half");
        }
    }

    @Test
    void fenceShapeIncludesWalls() {
        assertEquals(16, shipped(Surface.FENCE).size(), "12 wooden fences, nether brick fence and 3 walls");
        assertTrue(shipped(Surface.FENCE).containsAll(List.of(Block.OAK_FENCE, Block.NETHER_BRICK_FENCE, Block.COBBLESTONE_WALL, Block.STONE_BRICK_WALL)), "fences and walls");
    }

    @Test
    void panePaletteHasStainedGlassPanesAndIronBars() {
        assertEquals(17, shipped(Surface.PANE).size(), "16 colours and iron bars");
        assertTrue(shipped(Surface.PANE).contains(Block.IRON_BARS), "iron bars");
    }

    @Test
    void postsStandUpright() {
        List<Block> palette = shipped(Surface.POST);

        assertEquals(3, palette.size(), "end rod, chain, lightning rod");
        assertTrue(palette.contains(Block.END_ROD.withProperty("facing", "up")), "end rod");
        assertTrue(palette.contains(Block.LIGHTNING_ROD.withProperty("facing", "up")), "lightning rod");
        assertTrue(palette.stream().anyMatch(block -> "y".equals(block.getProperty("axis"))), "chain along y");
    }

    /** The material may change the look but never the standing height the rules rely on. */
    @Test
    void everyMaterialCollidesUpToItsSurfaceTop() {
        for (Surface surface : Surface.values()) {
            for (Block material : shipped(surface)) {
                assertEquals(surface.top(), material.collisionShape().relativeEnd().y(), 1e-9, material + " must collide up to the top of " + surface);
            }
        }
    }

    @Test
    void drawnMaterialBelongsToThePaletteAndIsReproducible() {
        for (Surface surface : Surface.values()) {
            Block first = SHIPPED.draw(surface, seeded(7L));

            assertTrue(shipped(surface).contains(first), surface + " draws from its own palette");
            assertEquals(first, SHIPPED.draw(surface, seeded(7L)), surface + " same seed, same material");
        }
    }

    @Test
    void tenFullBlocksInARowShowMoreThanOneMaterial() {
        RandomGenerator random = seeded(3L);

        Set<Block> drawn = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            drawn.add(SHIPPED.draw(Surface.FULL, random));
        }

        assertTrue(drawn.size() > 1, "ten draws gave only " + drawn);
    }

    @Test
    void aWeightOfThreeIsDrawnAboutThreeTimesAsOftenAsOne() {
        Palette palette = Palette.of(List.of(new Palette.Weighted(Block.RED_CONCRETE, 3), new Palette.Weighted(Block.BLUE_CONCRETE, 1)));
        RandomGenerator random = seeded(42L);

        long heavy = 0;
        for (int i = 0; i < DRAWS; i++) {
            if (palette.draw(random) == Block.RED_CONCRETE) {
                heavy++;
            }
        }

        double share = (double) heavy / DRAWS;
        assertTrue(share >= 0.70 && share <= 0.80, "the weight 3 entry has a share of " + share + ", expected 0.70 to 0.80");
    }

    @Test
    void everyEntryOfAPaletteCanBeDrawn() {
        Palette palette = Palette.of(List.of(new Palette.Weighted(Block.RED_CONCRETE, 1), new Palette.Weighted(Block.BLUE_CONCRETE, 2), new Palette.Weighted(Block.LIME_WOOL, 1)));
        RandomGenerator random = seeded(1L);

        Set<Block> drawn = new HashSet<>();
        for (int i = 0; i < DRAWS; i++) {
            drawn.add(palette.draw(random));
        }

        assertEquals(Set.of(Block.RED_CONCRETE, Block.BLUE_CONCRETE, Block.LIME_WOOL), drawn, "first, middle and last entry");
    }

    @Test
    void aSingleEntryIsAlwaysDrawn() {
        Palette palette = Palette.of(List.of(new Palette.Weighted(Block.LIME_WOOL, 5)));

        for (int i = 0; i < 50; i++) {
            assertEquals(Block.LIME_WOOL, palette.draw(seeded(i)), "seed " + i);
        }
    }

    @Test
    void aPaletteWithoutEntriesIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Palette.of(List.of()), "nothing to draw from");
    }

    @Test
    void aPaletteRejectsAWeightBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> Palette.of(List.of(new Palette.Weighted(Block.LIME_WOOL, 0))), "weight 0");
    }

    @Test
    void drawingAnotherMaterialNeverReturnsTheExcludedOne() {
        Palette palette = Palette.of(List.of(new Palette.Weighted(Block.RED_CONCRETE, 1), new Palette.Weighted(Block.BLUE_CONCRETE, 2), new Palette.Weighted(Block.LIME_WOOL, 1)));
        RandomGenerator random = seeded(1L);

        Set<Block> drawn = new HashSet<>();
        for (int i = 0; i < DRAWS; i++) {
            drawn.add(palette.drawOther(Block.BLUE_CONCRETE, random));
        }

        assertEquals(Set.of(Block.RED_CONCRETE, Block.LIME_WOOL), drawn, "every other entry, never the excluded one");
    }

    @Test
    void drawingAnotherMaterialKeepsTheWeightsOfTheRest() {
        Palette palette = Palette.of(List.of(new Palette.Weighted(Block.RED_CONCRETE, 1), new Palette.Weighted(Block.BLUE_CONCRETE, 3), new Palette.Weighted(Block.LIME_WOOL, 1)));
        RandomGenerator random = seeded(2L);

        int heavy = 0;
        for (int i = 0; i < DRAWS; i++) {
            if (palette.drawOther(Block.RED_CONCRETE, random) == Block.BLUE_CONCRETE) {
                heavy++;
            }
        }

        double share = (double) heavy / DRAWS;
        assertTrue(share >= 0.70 && share <= 0.80, "the weight 3 entry has a share of " + share + " among the others, expected 0.70 to 0.80");
    }

    @Test
    void aSingleEntryPaletteDrawsItsOnlyMaterialEvenWhenExcluded() {
        Palette palette = Palette.of(List.of(new Palette.Weighted(Block.LIME_WOOL, 5)));

        assertEquals(Block.LIME_WOOL, palette.drawOther(Block.LIME_WOOL, seeded(1L)));
    }
}
