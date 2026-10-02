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

import org.junit.jupiter.api.Test;

class SpawnZoneTest {

    private static final SpawnZone SPAWN = new SpawnZone(0.5, 0.5);

    @Test
    void sixteenBlocksAwayIsFarEnough() {
        assertTrue(SPAWN.isFarEnough(new BlockPos(16, 40, 0)), "exactly 16 blocks east");
    }

    @Test
    void fifteenBlocksAwayIsTooClose() {
        assertFalse(SPAWN.isFarEnough(new BlockPos(15, 40, 0)), "15 blocks east");
    }

    @Test
    void theDistanceIsHorizontalOnly() {
        assertFalse(SPAWN.isFarEnough(new BlockPos(0, 400, 0)), "far above the spawn is still on top of it");
    }

    @Test
    void theDistanceIsMeasuredAlongTheDiagonalToo() {
        assertFalse(SPAWN.isFarEnough(new BlockPos(11, 40, 11)), "about 15.6 blocks away");
        assertTrue(SPAWN.isFarEnough(new BlockPos(12, 40, 12)), "about 16.2 blocks away");
    }
}
