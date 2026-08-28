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
package net.onelitefeather.titan.common.resourcepack;

import net.kyori.adventure.resource.ResourcePackRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePackDefinitionTest {

    private static final String HASH = "0123456789abcdef0123456789abcdef01234567";
    private static final UUID ID = UUID.fromString("3ac1b0d9-4c8e-4d4a-9a05-cb2f0d5f9c7e");

    @Test
    @DisplayName("A definition keeps id, url and hash")
    void testValidDefinition() {
        ResourcePackDefinition definition = new ResourcePackDefinition(ID, "https://packs.example/pack-" + HASH + ".zip", HASH, true, null);

        assertEquals(ID, definition.id());
        assertEquals(HASH, definition.hash());
        assertEquals("https://packs.example/pack-" + HASH + ".zip", definition.uri().toString());
        assertTrue(definition.required());
    }

    @Test
    @DisplayName("An uppercase hash is normalised, because the protocol field is compared lowercase")
    void testHashIsNormalised() {
        ResourcePackDefinition definition = new ResourcePackDefinition(ID, "https://packs.example/base.zip", "  " + HASH.toUpperCase(java.util.Locale.ROOT) + " ", false, null);

        assertEquals(HASH, definition.hash());
    }

    @Test
    @DisplayName("A missing or malformed hash is rejected, the client caches by url and needs it")
    void testHashIsMandatory() {
        assertThrows(NullPointerException.class, () -> new ResourcePackDefinition(ID, "https://packs.example/base.zip", null, false, null));
        assertThrows(IllegalArgumentException.class, () -> new ResourcePackDefinition(ID, "https://packs.example/base.zip", "not-a-hash", false, null));
        assertThrows(IllegalArgumentException.class, () -> new ResourcePackDefinition(ID, "https://packs.example/base.zip", HASH + "00", false, null));
    }

    @Test
    @DisplayName("A blank or malformed url is rejected")
    void testUrlIsValidated() {
        assertThrows(IllegalArgumentException.class, () -> new ResourcePackDefinition(ID, "  ", HASH, false, null));
        assertThrows(IllegalArgumentException.class, () -> new ResourcePackDefinition(ID, "https://packs.example/a pack.zip", HASH, false, null));
    }

    @Test
    @DisplayName("Only a url carrying the hash survives the client's url keyed cache")
    void testContentAddressedDetection() {
        ResourcePackDefinition addressed = new ResourcePackDefinition(ID, "https://packs.example/pack-" + HASH + ".zip", HASH, false, null);
        ResourcePackDefinition fixedName = new ResourcePackDefinition(ID, "https://packs.example/season.zip", HASH, false, null);

        assertTrue(addressed.contentAddressed());
        assertFalse(fixedName.contentAddressed());
    }

    @Test
    @DisplayName("A request pushes a single pack and never replaces the stack")
    void testRequestNeverReplaces() {
        ResourcePackDefinition definition = new ResourcePackDefinition(ID, "https://packs.example/pack-" + HASH + ".zip", HASH, true, null);

        ResourcePackRequest request = definition.toRequest(true);

        assertFalse(request.replace(), "replace(true) is implemented as pop-all-then-push and must never be used");
        assertTrue(request.required());
        assertEquals(1, request.packs().size());
        assertEquals(ID, request.packs().getFirst().id());
        assertEquals(HASH, request.packs().getFirst().hash());
    }

    @Test
    @DisplayName("The required flag can be softened per push")
    void testRequirementCanBeSoftened() {
        ResourcePackDefinition definition = new ResourcePackDefinition(ID, "https://packs.example/pack-" + HASH + ".zip", HASH, true, null);

        assertFalse(definition.toRequest(false).required());
    }

    @Test
    @DisplayName("A configured prompt is parsed as MiniMessage, an absent one stays absent")
    void testPrompt() {
        ResourcePackDefinition withPrompt = new ResourcePackDefinition(ID, "https://packs.example/base.zip", HASH, false, "<red>Please accept");
        ResourcePackDefinition withoutPrompt = new ResourcePackDefinition(ID, "https://packs.example/base.zip", HASH, false, "  ");

        assertNotNull(withPrompt.promptComponent());
        assertNull(withoutPrompt.promptComponent());
    }
}
