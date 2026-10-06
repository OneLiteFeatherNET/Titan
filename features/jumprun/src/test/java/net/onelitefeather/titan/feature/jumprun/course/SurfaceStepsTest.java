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

import java.util.List;
import java.util.Map;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.ShapeImpl;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

class SurfaceStepsTest {

    private static final List<Surface> OLD_SHAPES = List.of(Surface.FULL, Surface.TRAPDOOR, Surface.SLAB, Surface.FENCE, Surface.PANE, Surface.POST);
    private static final List<String> FACINGS = List.of("north", "south", "east", "west");
    private static final double SAMPLE_STEP = 0.1;

    private static Block stairs(String facing) {
        return Surface.STAIRS.shape(Block.OAK_STAIRS).withProperty("facing", facing);
    }

    /** The highest collision top at a point of the cell, or -1 where nothing collides. */
    private static double collisionTopAt(Block block, double x, double z) {
        double top = -1;
        for (BoundingBox box : ((ShapeImpl) block.collisionShape()).boundingBoxes()) {
            if (x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ()) {
                top = Math.max(top, box.maxY());
            }
        }
        return top;
    }

    private static double stepTopAt(List<Step> steps, double x, double z) {
        return steps.stream().filter(step -> x >= step.minX() && x <= step.maxX() && z >= step.minZ() && z <= step.maxZ()).mapToDouble(Step::top).max().orElse(-1);
    }

    @Test
    void everyOldShapeHasOneStepOverTheWholeCell() {
        for (Surface surface : OLD_SHAPES) {
            assertEquals(List.of(new Step(surface.top(), 0, 1, 0, 1)), surface.steps(Block.STONE), surface + " has one step over the whole cell");
        }
    }

    @Test
    void newFlatShapesHaveOneStepOverTheWholeCell() {
        assertEquals(List.of(new Step(0.0625, 0, 1, 0, 1)), Surface.CARPET.steps(Block.WHITE_CARPET), "carpet");
        assertEquals(List.of(new Step(0.25, 0, 1, 0, 1)), Surface.SNOW.steps(Block.SNOW), "snow");
    }

    @Test
    void narrowShapesStandOnTheirRealFootprint() {
        assertEquals(List.of(new Step(0.5, 0.25, 0.75, 0.25, 0.75)), Surface.HEAD.steps(Block.PLAYER_HEAD), "head");
        assertEquals(List.of(new Step(0.375, 0.3125, 0.6875, 0.3125, 0.6875)), Surface.FLOWER_POT.steps(Block.FLOWER_POT), "flower pot");
        assertEquals(List.of(new Step(0.375, 0.4375, 0.5625, 0.4375, 0.5625)), Surface.CANDLE.steps(Block.CANDLE), "candle");
    }

    @Test
    void stairsFacingNorthHaveTheirHighStepToTheNorth() {
        assertEquals(List.of(new Step(1.0, 0, 1, 0, 0.5), new Step(0.5, 0, 1, 0.5, 1)), Surface.STAIRS.steps(stairs("north")), "high step first");
    }

    @Test
    void stairStepsMatchTheCollisionShapeOfTheBlockForEveryFacing() {
        for (String facing : FACINGS) {
            Block block = stairs(facing);
            List<Step> steps = Surface.STAIRS.steps(block);

            for (double x = SAMPLE_STEP / 2; x < 1; x += SAMPLE_STEP) {
                for (double z = SAMPLE_STEP / 2; z < 1; z += SAMPLE_STEP) {
                    assertEquals(collisionTopAt(block, x, z), stepTopAt(steps, x, z), 1e-9, facing + " at " + x + "/" + z);
                }
            }
        }
    }

    @Test
    void stairsHaveTheirLowStepHalfABlockBelowTheHighOne() {
        assertEquals(1.0, Surface.STAIRS.top(), "top is the high step");
        assertEquals(0.5, Surface.STAIRS.lowTop(), "lowTop is the low step");
        assertEquals(1, Surface.STAIRS.typeCost(), "cost");
    }

    @Test
    void everyOtherShapeHasItsTopAsLowTop() {
        for (Surface surface : Surface.values()) {
            if (surface != Surface.STAIRS) {
                assertEquals(surface.top(), surface.lowTop(), surface + " low top");
            }
        }
    }

    @Test
    void newShapesCarryTheDifficultyCostsOfTheDesign() {
        Map<Surface, Integer> costs = Map.of(Surface.STAIRS, 1, Surface.CARPET, 1, Surface.SNOW, 1, Surface.HEAD, 2, Surface.FLOWER_POT, 2, Surface.CANDLE, 3);

        costs.forEach((surface, cost) -> assertEquals(cost, surface.typeCost(), surface + " type cost"));
    }

    @Test
    void newShapesCarryTheirTops() {
        Map<Surface, Double> tops = Map.of(Surface.CARPET, 0.0625, Surface.SNOW, 0.25, Surface.HEAD, 0.5, Surface.FLOWER_POT, 0.375, Surface.CANDLE, 0.375);

        tops.forEach((surface, top) -> assertEquals(top, surface.top(), surface + " top"));
    }

    @Test
    void shapesForceTheStatesThatFixTheirCollision() {
        assertEquals("3", Surface.SNOW.shape(Block.SNOW).getProperty("layers"), "snow layers");
        assertEquals("1", Surface.CANDLE.shape(Block.CANDLE).getProperty("candles"), "one candle");
        assertEquals("false", Surface.CANDLE.shape(Block.CANDLE).getProperty("lit"), "not lit");
        assertEquals("bottom", Surface.STAIRS.shape(Block.OAK_STAIRS).getProperty("half"), "lower half");
        assertEquals("straight", Surface.STAIRS.shape(Block.OAK_STAIRS).getProperty("shape"), "no corner");
    }
}
