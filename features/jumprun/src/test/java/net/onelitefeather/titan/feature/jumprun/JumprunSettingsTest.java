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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.avaje.config.Configuration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import net.minestom.server.instance.block.Block;
import net.onelitefeather.titan.feature.jumprun.course.Palettes;
import net.onelitefeather.titan.feature.jumprun.course.Surface;
import net.onelitefeather.titan.feature.jumprun.course.TestBlocks;
import org.junit.jupiter.api.Test;

/** What a start accepts of {@code jumprun.palettes}, and that every refusal names its key. */
class JumprunSettingsTest {

    private static IllegalArgumentException refusal(Surface surface, Map<String, String> weights) {
        return assertThrows(IllegalArgumentException.class, () -> JumprunSettings.palettes(TestBlocks.shippedWith(surface, weights)));
    }

    private static void assertNamesKey(String key, IllegalArgumentException refusal) {
        assertTrue(refusal.getMessage().startsWith(key), "the message must start with " + key + " but was: " + refusal.getMessage());
    }

    @Test
    void theShippedDefaultsAreValid() {
        Palettes palettes = JumprunSettings.palettes(TestBlocks.shippedConfiguration());

        for (Surface surface : Surface.values()) {
            assertTrue(palettes.of(surface).blocks().size() > 0, surface + " has materials");
        }
    }

    @Test
    void aShapeIsShownWithTheStatesItNeeds() {
        Palettes palettes = JumprunSettings.palettes(TestBlocks.shippedWith(Surface.SLAB, Map.of("quartz_slab", "1")));

        assertEquals(List.of(Block.QUARTZ_SLAB.withProperty("type", "bottom")), palettes.of(Surface.SLAB).blocks(), "the operator names the block, the shape sets the half");
    }

    @Test
    void anUnknownBlockNamesItsKey() {
        assertNamesKey("jumprun.palettes.full.not_a_block", refusal(Surface.FULL, Map.of("not_a_block", "1")));
    }

    @Test
    void aBlockNameThatIsNoKeyNamesItsKey() {
        assertNamesKey("jumprun.palettes.full.Bad Name", refusal(Surface.FULL, Map.of("Bad Name", "1")));
    }

    @Test
    void aBlockOfTheWrongShapeNamesItsKey() {
        IllegalArgumentException refusal = refusal(Surface.TRAPDOOR, Map.of("stone", "1"));

        assertNamesKey("jumprun.palettes.trapdoor.stone", refusal);
        assertTrue(refusal.getMessage().contains("collides up to"), "the reason is the collision top: " + refusal.getMessage());
    }

    @Test
    void aWeightOfZeroSwitchesTheMaterialOff() {
        Configuration config = TestBlocks.shippedConfiguration();
        config.setProperty("jumprun.palettes.full.white_concrete", "0");

        Palettes palettes = JumprunSettings.palettes(config);

        assertFalse(palettes.of(Surface.FULL).blocks().contains(Block.WHITE_CONCRETE), "white concrete is off");
        assertEquals(32, palettes.of(Surface.FULL).blocks().size(), "the other 32 materials stay");
    }

    @Test
    void aSwitchedOffMaterialIsNeverDrawnWhileTheOthersAre() {
        Configuration config = TestBlocks.shippedConfiguration();
        config.setProperty("jumprun.palettes.full.white_concrete", "0");
        Palettes palettes = JumprunSettings.palettes(config);
        RandomGenerator random = RandomGeneratorFactory.of("L64X128MixRandom").create(11L);

        Set<Block> drawn = new HashSet<>();
        for (int i = 0; i < 4000; i++) {
            drawn.add(palettes.draw(Surface.FULL, random));
        }

        assertFalse(drawn.contains(Block.WHITE_CONCRETE), "white concrete was drawn");
        assertTrue(drawn.contains(Block.ORANGE_CONCRETE) && drawn.contains(Block.LIME_WOOL), "the other materials still appear");
    }

    @Test
    void aSwitchedOffMaterialIsStillCheckedForItsShape() {
        assertNamesKey("jumprun.palettes.trapdoor.stone", refusal(Surface.TRAPDOOR, Map.of("stone", "0", "oak_trapdoor", "1")));
    }

    @Test
    void aSwitchedOffMaterialWithATypoNamesItsKey() {
        assertNamesKey("jumprun.palettes.full.lime_woool", refusal(Surface.FULL, Map.of("lime_woool", "0", "lime_wool", "1")));
    }

    @Test
    void aShapeWithOnlyZeroWeightsNamesTheShape() {
        IllegalArgumentException refusal = refusal(Surface.FULL, Map.of("lime_wool", "0", "red_wool", "0"));

        assertNamesKey("jumprun.palettes.full", refusal);
        assertTrue(refusal.getMessage().contains("weight above 0"), "reason: " + refusal.getMessage());
    }

    @Test
    void aNegativeWeightNamesItsKey() {
        IllegalArgumentException refusal = refusal(Surface.PANE, Map.of("iron_bars", "-2", "white_stained_glass_pane", "1"));

        assertNamesKey("jumprun.palettes.pane.iron_bars", refusal);
        assertTrue(refusal.getMessage().contains("0 or greater"), "reason: " + refusal.getMessage());
    }

    @Test
    void aWeightThatIsNoNumberNamesItsKey() {
        IllegalArgumentException refusal = refusal(Surface.FULL, Map.of("lime_wool", "many"));

        assertNamesKey("jumprun.palettes.full.lime_wool", refusal);
        assertTrue(refusal.getMessage().contains("whole number"), "reason: " + refusal.getMessage());
    }

    @Test
    void anEmptyShapeNamesTheShape() {
        IllegalArgumentException refusal = refusal(Surface.FENCE, Map.of());

        assertNamesKey("jumprun.palettes.fence", refusal);
        assertTrue(refusal.getMessage().contains("must not be empty"), "reason: " + refusal.getMessage());
    }

    // --- the new shapes -------------------------------------------------------------------------

    @Test
    void aSlabInTheStairsListIsRefusedForItsHeight() {
        IllegalArgumentException refusal = refusal(Surface.STAIRS, Map.of("oak_slab", "1"));

        assertNamesKey("jumprun.palettes.stairs.oak_slab", refusal);
        assertTrue(refusal.getMessage().contains("collides up to 0.5 but the stairs shape needs 1.0"), "reason: " + refusal.getMessage());
    }

    @Test
    void aCarpetInTheSnowListIsRefusedForItsHeight() {
        IllegalArgumentException refusal = refusal(Surface.SNOW, Map.of("white_carpet", "1"));

        assertNamesKey("jumprun.palettes.snow.white_carpet", refusal);
        assertTrue(refusal.getMessage().contains("collides up to 0.0625 but the snow shape needs 0.25"), "reason: " + refusal.getMessage());
    }

    @Test
    void aHeadWithAnotherFootprintIsRefusedWithFoundAndExpectedFootprint() {
        IllegalArgumentException refusal = refusal(Surface.HEAD, Map.of("piglin_head", "1"));

        assertNamesKey("jumprun.palettes.head.piglin_head", refusal);
        assertTrue(refusal.getMessage().contains("collides over x 0.1875..0.8125, z 0.1875..0.8125 but the head shape needs x 0.25..0.75, z 0.25..0.75"), "found and expected footprint: " + refusal.getMessage());
    }

    @Test
    void aCandleInTheFlowerPotListIsRefusedForItsFootprintOrHeight() {
        IllegalArgumentException refusal = refusal(Surface.FLOWER_POT, Map.of("candle", "1"));

        assertNamesKey("jumprun.palettes.flower_pot.candle", refusal);
        assertTrue(refusal.getMessage().contains("collides over"), "the footprint differs: " + refusal.getMessage());
    }

    @Test
    void theNewShapesAreShownWithTheStatesThatFixTheirCollision() {
        Palettes palettes = JumprunSettings.palettes(TestBlocks.shippedConfiguration());

        assertEquals(List.of(Block.SNOW.withProperty("layers", "3")), palettes.of(Surface.SNOW).blocks(), "three layers");
        assertTrue(palettes.of(Surface.CANDLE).blocks().stream().allMatch(candle -> "1".equals(candle.getProperty("candles")) && "false".equals(candle.getProperty("lit"))), "one candle, not lit");
        assertTrue(palettes.of(Surface.STAIRS).blocks().stream().allMatch(stairs -> "bottom".equals(stairs.getProperty("half")) && "straight".equals(stairs.getProperty("shape"))), "lower half, straight");
    }

    @Test
    void aSnowListWithOneLayerIsStillShownWithThree() {
        Palettes palettes = JumprunSettings.palettes(TestBlocks.shippedWith(Surface.SNOW, Map.of("snow", "1")));

        assertEquals("3", palettes.of(Surface.SNOW).blocks().getFirst().getProperty("layers"), "the shape forces the layers");
    }

    @Test
    void anOperatorOverrideThatSwitchesOneStairsOffLeavesTheOthers() {
        Configuration config = TestBlocks.shippedConfiguration();
        config.setProperty("jumprun.palettes.stairs.oak_stairs", "0");

        Palettes palettes = JumprunSettings.palettes(config);

        assertFalse(palettes.of(Surface.STAIRS).blocks().stream().anyMatch(stairs -> stairs.id() == Block.OAK_STAIRS.id()), "oak stairs are off");
        assertEquals(TestBlocks.shipped().of(Surface.STAIRS).blocks().size() - 1, palettes.of(Surface.STAIRS).blocks().size(), "the others stay");
    }

    @Test
    void anOverrideWithOneMaterialPerNewShapeGivesExactlyThatPalette() {
        for (Surface surface : List.of(Surface.STAIRS, Surface.CARPET, Surface.SNOW, Surface.HEAD, Surface.FLOWER_POT, Surface.CANDLE)) {
            String block = TestBlocks.shipped().of(surface).blocks().getFirst().key().value();

            Palettes palettes = JumprunSettings.palettes(TestBlocks.shippedWith(surface, Map.of(block, "2")));

            assertEquals(List.of(TestBlocks.shipped().of(surface).blocks().getFirst()), palettes.of(surface).blocks(), surface + " has only " + block);
        }
    }

    @Test
    void aNegativeWeightInANewShapeNamesItsKey() {
        assertNamesKey("jumprun.palettes.candle.candle", refusal(Surface.CANDLE, Map.of("candle", "-1")));
    }

    @Test
    void anEmptyNewShapeAndOneWithOnlyZeroWeightsNameTheShape() {
        assertNamesKey("jumprun.palettes.carpet", refusal(Surface.CARPET, Map.of()));
        assertNamesKey("jumprun.palettes.carpet", refusal(Surface.CARPET, Map.of("white_carpet", "0")));
    }

    @Test
    void anUnknownBlockInANewShapeNamesItsKey() {
        assertNamesKey("jumprun.palettes.stairs.oak_stair", refusal(Surface.STAIRS, Map.of("oak_stair", "1")));
    }

    private static final List<String> REROLL_KEYS = List.of(JumprunSettings.RAINBOW_REROLL_TICKS_KEY, JumprunSettings.ULTRA_REROLL_TICKS_KEY);

    private static IllegalArgumentException rerollRefusal(String key, String value) {
        Configuration config = TestBlocks.shippedConfiguration();
        config.setProperty(key, value);
        return assertThrows(IllegalArgumentException.class, () -> JumprunSettings.rerollTicks(config, key));
    }

    @Test
    void theShippedRerollIntervalsAreTenForRainbowAndFortyForUltra() {
        Configuration config = TestBlocks.shippedConfiguration();

        assertEquals(10, JumprunSettings.rerollTicks(config, JumprunSettings.RAINBOW_REROLL_TICKS_KEY));
        assertEquals(40, JumprunSettings.rerollTicks(config, JumprunSettings.ULTRA_REROLL_TICKS_KEY));
    }

    @Test
    void theKeysCarryTheMode() {
        assertEquals("jumprun.rainbow.rerollTicks", JumprunSettings.RAINBOW_REROLL_TICKS_KEY);
        assertEquals("jumprun.ultra.rerollTicks", JumprunSettings.ULTRA_REROLL_TICKS_KEY);
    }

    @Test
    void aRerollIntervalOfZeroNamesItsKey() {
        for (String key : REROLL_KEYS) {
            IllegalArgumentException refusal = rerollRefusal(key, "0");

            assertNamesKey(key, refusal);
            assertTrue(refusal.getMessage().contains("greater than 0"), "reason: " + refusal.getMessage());
        }
    }

    @Test
    void aNegativeRerollIntervalNamesItsKey() {
        REROLL_KEYS.forEach(key -> assertNamesKey(key, rerollRefusal(key, "-5")));
    }

    @Test
    void aRerollIntervalThatIsNoNumberNamesItsKey() {
        for (String key : REROLL_KEYS) {
            IllegalArgumentException refusal = rerollRefusal(key, "often");

            assertNamesKey(key, refusal);
            assertTrue(refusal.getMessage().contains("whole number"), "reason: " + refusal.getMessage());
        }
    }

    @Test
    void aMissingRerollIntervalNamesItsKey() {
        for (String key : REROLL_KEYS) {
            Configuration config = TestBlocks.shippedConfiguration();
            config.clearProperty(key);

            assertNamesKey(key, assertThrows(IllegalArgumentException.class, () -> JumprunSettings.rerollTicks(config, key)));
        }
    }
}
