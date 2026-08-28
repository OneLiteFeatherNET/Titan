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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePackSettingsProviderTest {

    private static final String HASH = "0123456789abcdef0123456789abcdef01234567";

    @Test
    @DisplayName("Without a configuration file the feature stays disabled and nothing is written")
    void testMissingFile(@TempDir Path directory) throws IOException {
        ResourcePackSettings settings = ResourcePackSettingsProvider.load(directory);

        assertFalse(settings.enabled());
        try (var entries = Files.list(directory)) {
            assertEquals(List.of(), entries.toList(), "An unconfigured lobby must not create files of its own");
        }
    }

    @Test
    @DisplayName("A configured base and season pack are read with their identifiers and hashes")
    void testConfiguredPacks(@TempDir Path directory) throws IOException {
        UUID base = UUID.randomUUID();
        UUID season = UUID.randomUUID();
        Files.writeString(directory.resolve(ResourcePackSettingsProvider.FILE_NAME), """
                {
                  "base": {
                    "id": "%s",
                    "url": "https://packs.example/pack-%s.zip",
                    "hash": "%s",
                    "required": true
                  },
                  "season": {
                    "id": "%s",
                    "url": "https://packs.example/season-halloween-2026-%s.zip",
                    "hash": "%s",
                    "required": false,
                    "prompt": "<gold>Halloween"
                  },
                  "responseTimeoutMillis": 4000,
                  "sendToBedrockPlayers": false,
                  "bedrockNamePrefix": "."
                }
                """.formatted(base, HASH, HASH, season, HASH, HASH));

        ResourcePackSettings settings = ResourcePackSettingsProvider.load(directory);

        assertTrue(settings.enabled());
        assertEquals(base, settings.packFor(PackSlot.BASE).id());
        assertTrue(settings.packFor(PackSlot.BASE).required());
        assertEquals(season, settings.packFor(PackSlot.SEASON).id());
        assertFalse(settings.packFor(PackSlot.SEASON).required());
        assertEquals(4000L, settings.responseTimeoutMillis());
        assertFalse(settings.sendToBedrockPlayers());
    }

    @Test
    @DisplayName("A file without packs stays disabled")
    void testEmptyConfiguration(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve(ResourcePackSettingsProvider.FILE_NAME), "{}");

        ResourcePackSettings settings = ResourcePackSettingsProvider.load(directory);

        assertFalse(settings.enabled());
        assertEquals(ResourcePackSettings.DEFAULT_RESPONSE_TIMEOUT, settings.responseTimeout());
    }

    @Test
    @DisplayName("A pack with a broken hash disables the feature instead of keeping players out")
    void testInvalidPackIsRejected(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve(ResourcePackSettingsProvider.FILE_NAME), """
                {
                  "base": {
                    "id": "%s",
                    "url": "https://packs.example/base.zip",
                    "hash": "nope"
                  }
                }
                """.formatted(UUID.randomUUID()));

        ResourcePackSettings settings = ResourcePackSettingsProvider.load(directory);

        assertFalse(settings.enabled());
        assertNull(settings.packFor(PackSlot.BASE));
    }

    @Test
    @DisplayName("Malformed json disables the feature instead of keeping players out")
    void testMalformedJson(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve(ResourcePackSettingsProvider.FILE_NAME), "{ this is not json");

        assertFalse(ResourcePackSettingsProvider.load(directory).enabled());
    }
}
