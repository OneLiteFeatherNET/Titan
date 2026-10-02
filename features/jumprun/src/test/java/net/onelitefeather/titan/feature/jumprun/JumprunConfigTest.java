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

import io.avaje.config.Configuration;
import org.junit.jupiter.api.Test;

/** The reroll interval a run reads: the key of its mode, strictly at start and live per run. */
class JumprunConfigTest {

    @Test
    void theShippedDefaultsAreTenForRainbowAndFortyForUltra() {
        JumprunConfig config = TestBlocks.shippedReader();

        assertEquals(10, config.rerollTicks(Mode.RAINBOW));
        assertEquals(40, config.rerollTicks(Mode.ULTRA));
    }

    @Test
    void aModeThatNeverRerollsHasNoInterval() {
        JumprunConfig config = TestBlocks.shippedReader();

        for (Mode mode : new Mode[]{Mode.EASY, Mode.MEDIUM, Mode.HARD}) {
            assertEquals(0, config.rerollTicks(mode), mode + " does not reroll");
        }
    }

    @Test
    void eachModeReadsItsOwnKeyLive() {
        Configuration source = TestBlocks.shippedConfiguration();
        JumprunConfig config = new JumprunConfig(source);
        config.readAtStartup();

        source.setProperty(JumprunSettings.ULTRA_REROLL_TICKS_KEY, "99");

        assertEquals(10, config.rerollTicks(Mode.RAINBOW), "Rainbow is untouched");
        assertEquals(99, config.rerollTicks(Mode.ULTRA), "Ultra reads the edit");
    }

    @Test
    void startAbortsNamingTheInvalidKeyOfEitherMode() {
        for (String key : new String[]{JumprunSettings.RAINBOW_REROLL_TICKS_KEY, JumprunSettings.ULTRA_REROLL_TICKS_KEY}) {
            Configuration source = TestBlocks.shippedConfiguration();
            source.setProperty(key, "0");
            JumprunConfig config = new JumprunConfig(source);

            IllegalArgumentException abort = assertThrows(IllegalArgumentException.class, config::readAtStartup);

            assertTrue(abort.getMessage().startsWith(key), "the abort names the key: " + abort.getMessage());
        }
    }
}
