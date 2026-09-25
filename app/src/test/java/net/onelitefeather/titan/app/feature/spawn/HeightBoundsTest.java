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
package net.onelitefeather.titan.app.feature.spawn;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain unit coverage for {@link HeightBounds} - a pure rule with no dependency on Minestom or a
 * running server.
 */
class HeightBoundsTest {

    private final HeightBounds bounds = new HeightBounds(-64, 310);

    @DisplayName("A y coordinate below minHeight is out of bounds")
    @Test
    void belowMinHeightIsOutOfBounds() {
        Assertions.assertTrue(this.bounds.isOutOfBounds(-65));
    }

    @DisplayName("A y coordinate above maxHeight is out of bounds")
    @Test
    void aboveMaxHeightIsOutOfBounds() {
        Assertions.assertTrue(this.bounds.isOutOfBounds(311));
    }

    @DisplayName("A y coordinate strictly between minHeight and maxHeight is in bounds")
    @Test
    void betweenBoundsIsInBounds() {
        Assertions.assertFalse(this.bounds.isOutOfBounds(0));
    }

    @DisplayName("minHeight itself is still in bounds")
    @Test
    void exactlyAtMinHeightIsInBounds() {
        Assertions.assertFalse(this.bounds.isOutOfBounds(-64));
    }

    @DisplayName("maxHeight itself is still in bounds")
    @Test
    void exactlyAtMaxHeightIsInBounds() {
        Assertions.assertFalse(this.bounds.isOutOfBounds(310));
    }
}
