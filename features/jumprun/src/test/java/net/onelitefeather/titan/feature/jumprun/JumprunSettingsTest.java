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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import net.minestom.server.instance.block.Block;
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
    void aWeightOfZeroNamesItsKey() {
        IllegalArgumentException refusal = refusal(Surface.FULL, Map.of("lime_wool", "0"));

        assertNamesKey("jumprun.palettes.full.lime_wool", refusal);
        assertTrue(refusal.getMessage().contains("greater than 0"), "reason: " + refusal.getMessage());
    }

    @Test
    void aNegativeWeightNamesItsKey() {
        assertNamesKey("jumprun.palettes.pane.iron_bars", refusal(Surface.PANE, Map.of("iron_bars", "-2")));
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
}
