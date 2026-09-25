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
 * Covers the {@code lobby-module-config} spec scenario "syntactically broken file": {@link
 * ConfigStore#open(Path)} must name the file and the parser's position, and must never touch the
 * broken file.
 */
class ConfigStoreMalformedJsonTest {

    @Test
    @DisplayName("A syntactically broken file is rejected with file and position, and is left untouched")
    void brokenJsonIsRejectedAndFileIsUntouched(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        String broken = "{ \"tickle\": { \"cooldownMillis\": 4000 \"extra\": 1 } }"; // missing comma
        Files.writeString(file, broken);

        ConfigException exception = assertThrows(ConfigException.class, () -> ConfigStore.open(file));

        assertTrue(exception.getMessage().contains("app.json"), "message should name the file: " + exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase().contains("line") && exception.getMessage().toLowerCase().contains("column"), "message should name the parser position: " + exception.getMessage());

        assertEquals(broken, Files.readString(file), "a broken file must never be overwritten");
    }

    @Test
    @DisplayName("A document whose root is not a JSON object is rejected")
    void nonObjectRootIsRejected(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");
        String content = "[1, 2, 3]";
        Files.writeString(file, content);

        assertThrows(ConfigException.class, () -> ConfigStore.open(file));
        assertEquals(content, Files.readString(file));
    }
}
