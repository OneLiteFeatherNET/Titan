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
package net.onelitefeather.titan.buildsrc.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Plain-Java unit coverage for {@link DefaultsMerger}: no Gradle project needed (F.I.R.S.T.). */
class DefaultsMergerTest {

    private static File writeFile(Path dir, String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, content);
        return file.toFile();
    }

    @DisplayName("Two files with distinct keys are concatenated in order, content unchanged")
    @Test
    void distinctFilesAreConcatenatedInOrder(@TempDir Path dir) throws IOException {
        File spawn = writeFile(dir, "spawn.yaml", "# spawn comment\nspawn:\n  minHeight: -64\n");
        File sit = writeFile(dir, "sit.yaml", "# sit comment\nsit:\n  offset:\n    x: 0.5\n");

        String merged = DefaultsMerger.merge(List.of(spawn, sit));

        Assertions.assertEquals("# spawn comment\nspawn:\n  minHeight: -64\n\n# sit comment\nsit:\n  offset:\n    x: 0.5\n", merged, "both files' content, including comments, must appear in order, separated by a blank line");
    }

    @DisplayName("A single file's own keys, at every level of nesting, are all found")
    @Test
    void flattenedKeysOfFindsEveryLevelOfNesting(@TempDir Path dir) throws IOException {
        File sit = writeFile(dir, "sit.yaml", "sit:\n  offset:\n    x: 0.5\n    y: 0.25\n  allowedBlocks:\n    - a\n");

        var keys = DefaultsMerger.flattenedKeysOf(Files.readString(sit.toPath()), sit);

        Assertions.assertEquals(java.util.Set.of("sit", "sit.offset", "sit.offset.x", "sit.offset.y", "sit.allowedBlocks"), keys);
    }

    @DisplayName("Two files claiming the same top-level section fail, naming both files and the key")
    @Test
    void twoFilesClaimingTheSameTopLevelSectionFail(@TempDir Path dir) throws IOException {
        File first = writeFile(dir, "navigator.yaml", "features:\n  NAVIGATOR_SLENDER: false\n");
        File second = writeFile(dir, "other.yaml", "features:\n  OTHER_FLAG: true\n");

        DefaultsConflictException thrown = Assertions.assertThrows(DefaultsConflictException.class, () -> DefaultsMerger.merge(List.of(first, second)));

        Assertions.assertTrue(thrown.getMessage().contains("navigator.yaml"), "message must name the first file: " + thrown.getMessage());
        Assertions.assertTrue(thrown.getMessage().contains("other.yaml"), "message must name the second file: " + thrown.getMessage());
        Assertions.assertTrue(thrown.getMessage().contains("features"), "message must name the contested key: " + thrown.getMessage());
    }

    @DisplayName("Two files claiming the same nested key fail, naming both files and the dotted key")
    @Test
    void twoFilesClaimingTheSameNestedKeyFail(@TempDir Path dir) throws IOException {
        File first = writeFile(dir, "a.yaml", "elytra:\n  extra:\n    minHeight: -64\n");
        File second = writeFile(dir, "b.yaml", "elytra:\n  other:\n    minHeight: -32\n");

        DefaultsConflictException thrown = Assertions.assertThrows(DefaultsConflictException.class, () -> DefaultsMerger.merge(List.of(first, second)));

        Assertions.assertTrue(thrown.getMessage().contains("a.yaml"));
        Assertions.assertTrue(thrown.getMessage().contains("b.yaml"));
        Assertions.assertTrue(thrown.getMessage().contains("elytra"), "message must name the contested top-level section, even though the two files' own nested keys differ: " + thrown.getMessage());
    }

    @DisplayName("A single file with a duplicate key at the same level fails, naming that file")
    @Test
    void aSingleFileWithADuplicateKeyFails(@TempDir Path dir) throws IOException {
        File brokenFile = writeFile(dir, "broken.yaml", "spawn:\n  minHeight: -64\n  minHeight: -32\n");

        DefaultsConflictException thrown = Assertions.assertThrows(DefaultsConflictException.class, () -> DefaultsMerger.merge(List.of(brokenFile)));

        Assertions.assertTrue(thrown.getMessage().contains("broken.yaml"), "message must name the offending file: " + thrown.getMessage());
    }

    @DisplayName("Merging a single file returns exactly its own content")
    @Test
    void mergingASingleFileReturnsItsOwnContent(@TempDir Path dir) throws IOException {
        File onlyFile = writeFile(dir, "runtime.yaml", "config.watch.enabled: false\n");

        String merged = DefaultsMerger.merge(List.of(onlyFile));

        Assertions.assertEquals("config.watch.enabled: false\n", merged);
    }
}
