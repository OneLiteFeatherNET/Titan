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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Covers {@link ConfigStore#save()} writing atomically: the document is written to a sibling
 * temporary file first and only then moved into place, so a reader never observes a half-written
 * {@code app.json} and a failed save never corrupts the previous, still-valid file.
 */
class ConfigStoreAtomicSaveTest {

    @Test
    @DisplayName("save() leaves a complete file and no leftover temp file")
    void saveLeavesCompleteFileAndNoLeftovers(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");

        ConfigStore store = ConfigStore.open(file);
        store.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);
        store.save();

        assertTrue(Files.exists(file), "the target file must exist after save()");
        List<Path> siblings;
        try (var stream = Files.list(tempDir)) {
            siblings = stream.toList();
        }
        assertEquals(List.of(file), siblings, "no temporary file must be left behind next to app.json");

        // The written file must be complete, valid JSON.
        var document = com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertTrue(document.has("configVersion"));
        assertTrue(document.has("tickle"));
    }

    @Test
    @DisplayName("A save() that cannot write its temp file leaves the previous file content intact")
    void failedSaveLeavesPreviousContentIntact(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("app.json");

        ConfigStore first = ConfigStore.open(file);
        first.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);
        first.save();
        String originalContent = Files.readString(file);

        ConfigStore second = ConfigStore.open(file);
        second.section("tickle", TickleTestConfig.class, TickleTestConfig.DEFAULTS);
        second.set("tickle", "cooldownMillis", new com.google.gson.JsonPrimitive(9999L));

        Set<PosixFilePermission> writable = PosixFilePermissions.fromString("rwxr-xr-x");
        Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("r-xr-xr-x");
        try {
            Files.setPosixFilePermissions(tempDir, readOnly);
        } catch (UnsupportedOperationException e) {
            assumeTrue(false, "POSIX permissions are not supported on this file system");
        }

        try {
            assertThrows(UncheckedIOException.class, second::save, "creating the temp file in a read-only directory must fail");
        } finally {
            Files.setPosixFilePermissions(tempDir, writable);
        }

        assertEquals(originalContent, Files.readString(file), "a failed save must never touch the previous, valid file content");
        List<Path> siblings;
        try (var stream = Files.list(tempDir)) {
            siblings = stream.toList();
        }
        assertEquals(List.of(file), siblings, "a failed save must not leave a leftover temp file");
    }
}
