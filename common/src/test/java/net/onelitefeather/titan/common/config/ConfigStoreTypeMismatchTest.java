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
 * Covers a JSON type mismatch (e.g. a string where a record component expects a {@code long}):
 * {@link ConfigStore#section(String, Class, Record)} must not let Gson's raw {@link
 * RuntimeException} escape; it must report a {@link ConfigException} naming the file, the section
 * and - whenever the offending field can be determined by checking the section against the
 * record's own components - the field too.
 */
class ConfigStoreTypeMismatchTest {

    @Test
    @DisplayName("A string value for a long field is rejected, naming file, section and field")
    void stringForLongFieldIsRejected(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "tickle": {"cooldownMillis": "oops"}
                }
                """);
        ConfigStore store = ConfigStore.open(file);

        ConfigException exception = assertThrows(ConfigException.class, () -> store.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS));

        assertEquals("app.json", exception.file());
        assertEquals("tickle", exception.section());
        assertEquals("cooldownMillis", exception.field(), "the offending field should be found by checking the record's components");
        assertTrue(exception.reason() != null && !exception.reason().isBlank(), "a type mismatch must carry a reason");
        assertTrue(exception.getMessage().contains("tickle.cooldownMillis"), "message should name section and field: " + exception.getMessage());
    }

    @Test
    @DisplayName("A type mismatch inside a multi-field section still names the offending field")
    void mismatchInMultiFieldSectionNamesTheField(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "spawn": {"minHeight": -64, "maxHeight": "not-a-number", "simulationDistance": 2}
                }
                """);
        ConfigStore store = ConfigStore.open(file);

        ConfigException exception = assertThrows(ConfigException.class, () -> store.section("spawn", SpawnTestConfig.class, SpawnTestConfig.DEFAULTS));

        assertEquals("spawn", exception.section());
        assertEquals("maxHeight", exception.field());
        assertEquals("app.json", exception.file());
    }
}
