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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

class StepLandingTest {

    private static final List<String> FACINGS = List.of("north", "south", "east", "west");
    private static final BlockPos AT = new BlockPos(10, 20, 30);
    private static final double ABOVE_FLOOR = AT.y();

    private static CourseBlock block(Surface surface, Block material) {
        return new CourseBlock(AT, surface, surface.shape(material));
    }

    private static CourseBlock stairs(String facing) {
        return new CourseBlock(AT, Surface.STAIRS, Surface.STAIRS.shape(Block.OAK_STAIRS).withProperty("facing", facing));
    }

    private static Pos feet(double cellX, double height, double cellZ) {
        return new Pos(AT.x() + cellX, ABOVE_FLOOR + height, AT.z() + cellZ);
    }

    // --- stairs: the high step lies on the side the stairs face --------------------------------------

    @Test
    void aRunnerOnTheLowStepOfStairsHasLanded() {
        for (String facing : FACINGS) {
            Pos onLowStep = switch (facing) {
                case "north" -> feet(0.5, 0.5, 0.9);
                case "south" -> feet(0.5, 0.5, 0.1);
                case "east" -> feet(0.1, 0.5, 0.5);
                default -> feet(0.9, 0.5, 0.5);
            };

            assertTrue(stairs(facing).supports(onLowStep), facing + ": the low step carries the runner");
        }
    }

    @Test
    void aRunnerOnTheHighStepOfStairsHasLanded() {
        for (String facing : FACINGS) {
            Pos onHighStep = switch (facing) {
                case "north" -> feet(0.5, 1.0, 0.1);
                case "south" -> feet(0.5, 1.0, 0.9);
                case "east" -> feet(0.9, 1.0, 0.5);
                default -> feet(0.1, 1.0, 0.5);
            };

            assertTrue(stairs(facing).supports(onHighStep), facing + ": the high step carries the runner");
        }
    }

    @Test
    void aRunnerBetweenTheTwoStairStepsHasNotLanded() {
        assertFalse(stairs("north").supports(feet(0.5, 0.75, 0.5)), "0.75 is neither step");
    }

    @Test
    void aRunnerFarFromTheHighStepOnItsHeightHasNotLanded() {
        assertFalse(stairs("north").supports(feet(0.5, 1.0, 1.0 + 0.5)), "half a block past the low edge");
    }

    // --- narrow shapes: the real footprint plus the half width of the hitbox -------------------------

    @Test
    void aRunnerWhoseHitboxJustTouchesTheCandleHasLanded() {
        CourseBlock candle = block(Surface.CANDLE, Block.CANDLE);

        assertTrue(candle.supports(feet(0.14, 0.375, 0.5)), "0.14 reaches 0.4375 - 0.3 = 0.1375");
        assertTrue(candle.supports(feet(0.86, 0.375, 0.5)), "0.86 reaches 0.5625 + 0.3 = 0.8625");
    }

    @Test
    void aRunnerBesideTheCandleHasNotLanded() {
        CourseBlock candle = block(Surface.CANDLE, Block.CANDLE);

        assertFalse(candle.supports(feet(0.13, 0.375, 0.5)), "0.13 is past the left reach");
        assertFalse(candle.supports(feet(0.87, 0.375, 0.5)), "0.87 is past the right reach");
        assertFalse(candle.supports(feet(0.5, 0.375, 0.13)), "0.13 is past the reach on z");
    }

    @Test
    void aRunnerInTheMiddleOfTheCandleAtTheWrongHeightHasNotLanded() {
        CourseBlock candle = block(Surface.CANDLE, Block.CANDLE);

        assertTrue(candle.supports(feet(0.5, 0.375 + 0.049, 0.5)), "within the height tolerance");
        assertFalse(candle.supports(feet(0.5, 0.375 + 0.06, 0.5)), "just above the tolerance");
        assertFalse(candle.supports(feet(0.5, 0.375 - 0.06, 0.5)), "just below the tolerance");
    }

    @Test
    void aRunnerOnTheWholeCellOfACarpetHasLanded() {
        CourseBlock carpet = block(Surface.CARPET, Block.WHITE_CARPET);

        assertTrue(carpet.supports(feet(0.5, 0.0625, 0.5)), "middle");
        assertTrue(carpet.supports(feet(-0.29, 0.0625, 1.29)), "just inside the half width past the corner");
        assertFalse(carpet.supports(feet(0.5, 0.0625 + 0.2, 0.5)), "a trapdoor's height is no carpet");
    }

    // --- regression: the old shapes keep their lenient whole-cell landing ----------------------------

    @Test
    void aRunnerBesideAPostByTheHalfWidthStillLands() {
        CourseBlock post = block(Surface.POST, Block.END_ROD);

        assertTrue(post.supports(feet(-0.3, 1.0, 0.5)), "the whole cell plus the half width, as before");
        assertFalse(post.supports(feet(-0.31, 1.0, 0.5)), "not a hair further");
    }
}
