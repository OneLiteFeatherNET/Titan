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
package net.onelitefeather.titan.feature.jumprun.space;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class OccupiedProbeTest {

    private static final BlockPos TAKEN = new BlockPos(1, 2, 3);

    private final FakeSpaceProbe world = new FakeSpaceProbe().occupy(5, 5, 5);
    private final OccupiedProbe probe = new OccupiedProbe(world, Set.of(TAKEN));

    @Test
    void anOccupiedPositionIsNotAirEvenIfTheWorldIsEmptyThere() {
        assertFalse(probe.isAir(TAKEN), "taken by a course block");
    }

    @Test
    void aPositionTakenInTheWorldStaysNotAir() {
        assertFalse(probe.isAir(new BlockPos(5, 5, 5)), "solid in the world");
    }

    @Test
    void anyOtherPositionIsAsFreeAsTheWorldSaysIt() {
        assertTrue(probe.isAir(new BlockPos(0, 0, 0)), "free everywhere");
    }

    @Test
    void boundsComeFromTheWorld() {
        assertTrue(probe.inBounds(TAKEN), "inside the fake world");
        assertFalse(probe.inBounds(new BlockPos(500, 0, 0)), "outside the fake world");
    }
}
