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
package net.onelitefeather.titan.feature.throwing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChargeTest {

    @DisplayName("Charge strength is zero for no charge at all")
    @Test
    void noChargeHasNoStrength() {
        Assertions.assertEquals(0.0, Charge.strength(0), 1e-9, "a charge of 0 ticks must have strength 0");
    }

    @DisplayName("A tap of one tick throws with strength zero")
    @Test
    void aTapThrowsWithZeroStrength() {
        Assertions.assertEquals(0.0, Charge.strength(1), 1e-9, "a charge below two ticks must count as a tap");
    }

    @DisplayName("Half of the full charge gives half the strength")
    @Test
    void halfChargeGivesHalfStrength() {
        Assertions.assertEquals(0.5, Charge.strength(15), 1e-9, "15 of 30 ticks must give strength 0.5");
    }

    @DisplayName("Exactly the full charge gives full strength")
    @Test
    void fullChargeGivesFullStrength() {
        Assertions.assertEquals(1.0, Charge.strength(30), 1e-9, "30 ticks must give strength 1");
    }

    @DisplayName("A charge beyond the cap stays at full strength")
    @Test
    void chargeBeyondTheCapStaysAtFullStrength() {
        Assertions.assertEquals(1.0, Charge.strength(90), 1e-9, "a charge past 30 ticks must not exceed strength 1");
    }

    @DisplayName("Speed runs linearly from 8 to 16 blocks per second")
    @Test
    void speedIsLinearInStrength() {
        Assertions.assertEquals(8.0, Charge.speed(0.0), 1e-9, "strength 0 must throw at 8 blocks per second");
        Assertions.assertEquals(12.0, Charge.speed(0.5), 1e-9, "strength 0.5 must throw at 12 blocks per second");
        Assertions.assertEquals(16.0, Charge.speed(1.0), 1e-9, "strength 1 must throw at 16 blocks per second");
    }
}
