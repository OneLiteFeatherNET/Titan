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
import net.minestom.server.item.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentMaterialTypeIntegrationTest {

    @Test
    @DisplayName("Test ArgumentMaterialType with different material types")
    void testArgumentMaterialTypeWithDifferentMaterialTypes() throws ArgumentSyntaxException {
        // Create the argument
        ArgumentMaterialType materialArg = new ArgumentMaterialType("material");

        // Test with different valid materials
        String result1 = materialArg.parse(null, "minecraft:stone");
        assertEquals("minecraft:stone", result1);

        String result2 = materialArg.parse(null, "minecraft:oak_planks");
        assertEquals("minecraft:oak_planks", result2);

        String result3 = materialArg.parse(null, "minecraft:diamond_sword");
        assertEquals("minecraft:diamond_sword", result3);

        // Verify that the parsed values match the actual Material keys
        assertEquals(Material.STONE.key().asString(), result1);
        assertEquals(Material.OAK_PLANKS.key().asString(), result2);
        assertEquals(Material.DIAMOND_SWORD.key().asString(), result3);
    }

    @Test
    @DisplayName("Test ArgumentMaterialType with invalid inputs")
    void testArgumentMaterialTypeWithInvalidInputs() {
        // Create the argument
        ArgumentMaterialType materialArg = new ArgumentMaterialType("material");

        // Test with invalid material name
        assertThrows(ArgumentSyntaxException.class, () -> {
            materialArg.parse(null, "minecraft:nonexistent_material");
        });

        // Test with empty input
        assertThrows(ArgumentSyntaxException.class, () -> {
            materialArg.parse(null, "");
        });

        // Test with malformed input
        assertThrows(ArgumentSyntaxException.class, () -> {
            materialArg.parse(null, "malformed:input");
        });
    }

    @Test
    @DisplayName("Test ArgumentMaterialType with Material enum integration")
    void testArgumentMaterialTypeWithMaterialEnumIntegration() throws ArgumentSyntaxException {
        // Create the argument
        ArgumentMaterialType materialArg = new ArgumentMaterialType("material");

        // Test with various materials from the Material enum
        for (Material material : new Material[]{Material.STONE, Material.DIRT, Material.GRASS_BLOCK, Material.OAK_LOG}) {
            String materialKey = material.key().asString();
            String result = materialArg.parse(null, materialKey);

            // The result should match the original material key
            assertEquals(materialKey, result);

            // We should be able to convert back to a Material
            Material parsedMaterial = Material.values().stream().filter(m -> m.key().asString().equals(result)).findFirst().orElse(null);

            assertNotNull(parsedMaterial);
            assertEquals(material, parsedMaterial);
        }
    }
}
