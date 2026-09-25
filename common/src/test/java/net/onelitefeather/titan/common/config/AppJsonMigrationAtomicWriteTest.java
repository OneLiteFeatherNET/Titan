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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Covers {@link AppJsonMigration#migrate(Path)}'s atomic write of {@code application.yaml}: the
 * document is written to a sibling temporary file first and only then moved into place, so a
 * reader never observes a half-written file and a failed migration never leaves a corrupt or
 * partial {@code application.yaml} behind, nor renames {@code app.json} it did not finish writing
 * a replacement for.
 */
class AppJsonMigrationAtomicWriteTest {

    private final AppJsonMigration migration = new AppJsonMigration();

    @Test
    @DisplayName("migrate() leaves a complete application.yaml and no leftover temp file")
    void migrateLeavesCompleteFileAndNoLeftovers(@TempDir Path tempDir) throws IOException {
        Path appJson = tempDir.resolve("app.json");
        Files.writeString(appJson, "{\"tickleDuration\": 4000}");

        migration.migrate(tempDir);

        Path applicationYaml = tempDir.resolve("application.yaml");
        Path migratedAppJson = tempDir.resolve("app.json.migrated");
        assertTrue(Files.exists(applicationYaml), "application.yaml must exist after migrate()");
        List<Path> siblings;
        try (var stream = Files.list(tempDir)) {
            siblings = stream.toList();
        }
        assertEquals(Set.of(applicationYaml, migratedAppJson), Set.copyOf(siblings), "no temporary file must be left behind next to application.yaml");
    }

    @Test
    @DisplayName("A migrate() that cannot write its temp file leaves app.json untouched and no application.yaml behind")
    void failedMigrateLeavesAppJsonUntouchedAndNoLeftovers(@TempDir Path tempDir) throws IOException {
        Path appJson = tempDir.resolve("app.json");
        String content = "{\"tickleDuration\": 4000}";
        Files.writeString(appJson, content);

        Set<PosixFilePermission> writable = PosixFilePermissions.fromString("rwxr-xr-x");
        Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("r-xr-xr-x");
        try {
            Files.setPosixFilePermissions(tempDir, readOnly);
        } catch (UnsupportedOperationException e) {
            assumeTrue(false, "POSIX permissions are not supported on this file system");
        }

        try {
            assertThrows(UncheckedIOException.class, () -> migration.migrate(tempDir), "creating the temp file in a read-only directory must fail");
        } finally {
            Files.setPosixFilePermissions(tempDir, writable);
        }

        assertEquals(content, Files.readString(appJson), "a failed migration must never touch the original app.json");
        assertFalse(Files.exists(tempDir.resolve("application.yaml")), "a failed migration must not leave a partially written application.yaml");
        assertFalse(Files.exists(tempDir.resolve("app.json.migrated")), "a failed migration must not rename app.json when the write itself failed");
        List<Path> siblings;
        try (var stream = Files.list(tempDir)) {
            siblings = stream.toList();
        }
        assertEquals(List.of(appJson), siblings, "a failed migration must not leave a leftover temp file");
    }
}
