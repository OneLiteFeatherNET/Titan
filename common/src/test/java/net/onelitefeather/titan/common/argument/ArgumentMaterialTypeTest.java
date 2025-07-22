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
package net.onelitefeather.titan.common.argument;

import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentMaterialTypeTest {

    @Test
    @DisplayName("Test parse with valid material name")
    void testParseWithValidMaterialName() throws ArgumentSyntaxException {
        // Arrange
        ArgumentMaterialType argument = new ArgumentMaterialType("material");
        String input = "minecraft:stone";

        // Act
        String result = argument.parse(null, input);

        // Assert
        assertEquals("minecraft:stone", result);
    }

    @Test
    @DisplayName("Test parse with valid material name in different case")
    void testParseWithValidMaterialNameDifferentCase() throws ArgumentSyntaxException {
        // Arrange
        ArgumentMaterialType argument = new ArgumentMaterialType("material");
        String input = "MINECRAFT:STONE";

        // Act
        String result = argument.parse(null, input);

        // Assert
        assertEquals("minecraft:stone", result);
    }

    @Test
    @DisplayName("Test parse with invalid material name throws exception")
    void testParseWithInvalidMaterialNameThrowsException() {
        // Arrange
        ArgumentMaterialType argument = new ArgumentMaterialType("material");
        String input = "minecraft:nonexistent_material";

        // Act & Assert
        assertThrows(ArgumentSyntaxException.class, () -> {
            argument.parse(null, input);
        });
    }

    @Test
    @DisplayName("Test parse with empty input throws exception")
    void testParseWithEmptyInputThrowsException() {
        // Arrange
        ArgumentMaterialType argument = new ArgumentMaterialType("material");
        String input = "";

        // Act & Assert
        assertThrows(ArgumentSyntaxException.class, () -> {
            argument.parse(null, input);
        });
    }

    @Test
    @DisplayName("Test constructor sets ID correctly")
    void testConstructorSetsIdCorrectly() {
        // Arrange
        String id = "testMaterial";

        // Act
        ArgumentMaterialType argument = new ArgumentMaterialType(id);

        // Assert
        assertEquals(id, argument.getId());
    }
}