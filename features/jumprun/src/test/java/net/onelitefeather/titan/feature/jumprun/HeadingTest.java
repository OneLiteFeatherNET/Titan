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

import org.junit.jupiter.api.Test;

class HeadingTest {

    private static final Heading EAST = new Heading(1.0, 0.0);

    @Test
    void aStepAlongTheHeadingKeepsIt() {
        Heading steered = EAST.steered(Direction.EAST);

        assertEquals(1.0, steered.x(), 1e-12, "x");
        assertEquals(0.0, steered.z(), 1e-12, "z");
    }

    @Test
    void aSideStepBendsTheHeadingByTwentyPercentBeforeNormalizing() {
        Heading steered = EAST.steered(Direction.SOUTH);

        double length = Math.hypot(0.8, 0.2);
        assertEquals(0.8 / length, steered.x(), 1e-12, "x is 0.8 of the old heading");
        assertEquals(0.2 / length, steered.z(), 1e-12, "z is 0.2 of the step");
    }

    @Test
    void aDiagonalStepCountsAsAUnitVector() {
        Heading steered = EAST.steered(Direction.SOUTH_EAST);

        double stepPart = 0.2 / Math.sqrt(2.0);
        double length = Math.hypot(0.8 + stepPart, stepPart);
        assertEquals((0.8 + stepPart) / length, steered.x(), 1e-12, "x");
        assertEquals(stepPart / length, steered.z(), 1e-12, "z");
    }

    @Test
    void aSteeredHeadingStaysAUnitVector() {
        Heading heading = EAST;
        for (Direction step : Direction.values()) {
            heading = heading.steered(step);
            assertEquals(1.0, Math.hypot(heading.x(), heading.z()), 1e-12, "length after " + step);
        }
    }

    @Test
    void aStepAgainstTheHeadingNeverCancelsItOut() {
        Heading steered = EAST.steered(Direction.WEST);

        assertEquals(1.0, steered.x(), 1e-12, "the heading only weakens, then is normalized again");
    }
}
