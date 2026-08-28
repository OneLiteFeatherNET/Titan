/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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