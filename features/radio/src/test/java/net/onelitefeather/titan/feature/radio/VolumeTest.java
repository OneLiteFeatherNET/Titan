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
package net.onelitefeather.titan.feature.radio;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VolumeTest {

    @DisplayName("The volume steps are 25, 50, 75 and 100 percent")
    @Test
    void stepsAreTheFourLevels() {
        Assertions.assertEquals(List.of(25, 50, 75, 100), Volume.STEPS, "the radio volume must have exactly the four steps");
    }

    @DisplayName("The default volume is 100 percent")
    @Test
    void defaultIsFullVolume() {
        Assertions.assertEquals(100, Volume.DEFAULT, "a player without a stored volume must hear 100 percent");
    }

    @DisplayName("A left click raises the volume by one step")
    @Test
    void leftClickRaisesOneStep() {
        Assertions.assertEquals(75, Volume.up(50), "a left click at 50 percent must go to 75 percent");
    }

    @DisplayName("A right click lowers the volume by one step")
    @Test
    void rightClickLowersOneStep() {
        Assertions.assertEquals(50, Volume.down(75), "a right click at 75 percent must go to 50 percent");
    }

    @DisplayName("The volume stays at 100 percent on a left click")
    @Test
    void upStaysAtTheTop() {
        Assertions.assertEquals(100, Volume.up(100), "the top step must not wrap around");
    }

    @DisplayName("The volume stays at 25 percent on a right click")
    @Test
    void downStaysAtTheBottom() {
        Assertions.assertEquals(25, Volume.down(25), "the bottom step must not wrap around");
    }

    @DisplayName("A value that is not a step is moved to the nearest step in the click direction")
    @Test
    void valueBetweenStepsSnapsToTheStep() {
        Assertions.assertEquals(75, Volume.up(60), "up from 60 percent must reach the next step, 75 percent");
        Assertions.assertEquals(50, Volume.down(60), "down from 60 percent must reach the step below, 50 percent");
    }

    @DisplayName("The factor of a step is its percentage divided by 100")
    @Test
    void factorIsThePercentage() {
        Assertions.assertEquals(0.25f, Volume.factor(25), 0.0001f, "25 percent must scale by 0.25");
        Assertions.assertEquals(1.0f, Volume.factor(100), 0.0001f, "100 percent must scale by 1.0");
    }
}
