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

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DirectionTest {

    @Test
    void thereAreFourAxisAndFourDiagonalDirections() {
        long diagonal = Arrays.stream(Direction.values()).filter(Direction::isDiagonal).count();

        assertEquals(4, diagonal, "diagonal directions");
        assertEquals(8, Direction.values().length, "all directions");
    }

    @Test
    void anAxisDirectionIsNotDiagonal() {
        assertFalse(Direction.EAST.isDiagonal(), "east");
        assertTrue(Direction.NORTH_WEST.isDiagonal(), "north west");
    }

    @Test
    void aDiagonalStepIsLongerThanAnAxisStep() {
        assertEquals(1.0, Direction.SOUTH.length(), 1e-9, "axis");
        assertEquals(Math.sqrt(2.0), Direction.SOUTH_EAST.length(), 1e-9, "diagonal");
    }

    @Test
    void towardUsesTheSignsOfTheOffset() {
        assertEquals(Direction.WEST, Direction.toward(-4, 0), "west");
        assertEquals(Direction.NORTH_EAST, Direction.toward(3, -3), "north east");
    }

    @Test
    void towardRejectsNoStep() {
        assertThrows(IllegalArgumentException.class, () -> Direction.toward(0, 0), "standing still is no direction");
    }
}
