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
package net.onelitefeather.titan.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@code lobby-module-config} spec scenarios "negative duration" and "impossible
 * height bounds": a value that fails a config record's own validation must abort with a message
 * naming the section, the field and the reason - see {@link ConfigException}.
 */
class ConfigStoreValidationTest {

    @Test
    @DisplayName("A negative duration is rejected, naming the section, field and reason")
    void negativeDurationIsRejected(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "tickle": {"cooldownMillis": -5}
                }
                """);
        ConfigStore store = ConfigStore.open(file);

        ConfigException exception = assertThrows(ConfigException.class, () -> store.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS));

        assertEquals("tickle", exception.section());
        assertEquals("cooldownMillis", exception.field());
        assertTrue(exception.reason() != null && exception.reason().contains("negative"), "reason should explain why the value is invalid, was: " + exception.reason());
        assertTrue(exception.getMessage().contains("app.json"), "message should name the file");
        assertTrue(exception.getMessage().contains("tickle.cooldownMillis"), "message should name section and field: " + exception.getMessage());
    }

    @Test
    @DisplayName("Impossible height bounds are rejected, naming both fields")
    void impossibleHeightBoundsAreRejected(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "spawn": {"minHeight": 500, "maxHeight": -64, "simulationDistance": 2}
                }
                """);
        ConfigStore store = ConfigStore.open(file);

        ConfigException exception = assertThrows(ConfigException.class, () -> store.section("spawn", SpawnTestConfig.class, SpawnTestConfig.DEFAULTS));

        assertEquals("spawn", exception.section());
        assertEquals("minHeight", exception.field());
        assertTrue(exception.reason() != null && exception.reason().contains("maxHeight"), "reason should name both fields: " + exception.reason());
    }
}
