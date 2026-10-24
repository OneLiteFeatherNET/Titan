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
package net.onelitefeather.titan.buildsrc.architecture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Every module with production code must carry a {@code MaintainabilityArchitectureTest} under
 * {@code src/test/java} (openspec {@code maintainability-gates}, design D3). The test has to be
 * written out per module; this guard only checks that it exists, so a new module cannot skip it.
 */
public final class ArchitectureTestGuard {

    /** The file name each module's architecture test must have. */
    public static final String REQUIRED_TEST_FILE = "MaintainabilityArchitectureTest.java";

    private ArchitectureTestGuard() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Returns a message naming the module when it has Java production code but no
     * {@link #REQUIRED_TEST_FILE} under {@code src/test/java}, or empty when the guard is satisfied.
     */
    public static Optional<String> violation(Path moduleDir, String modulePath) {
        if (!hasJavaFiles(moduleDir.resolve("src/main/java")) || hasFileNamed(moduleDir.resolve("src/test/java"), REQUIRED_TEST_FILE)) {
            return Optional.empty();
        }
        return Optional.of(modulePath + " has production code but no " + REQUIRED_TEST_FILE + " under src/test/java");
    }

    private static boolean hasJavaFiles(Path root) {
        return anyFile(root, name -> name.endsWith(".java"));
    }

    private static boolean hasFileNamed(Path root, String fileName) {
        return anyFile(root, fileName::equals);
    }

    private static boolean anyFile(Path root, Predicate<String> nameMatches) {
        if (!Files.isDirectory(root)) {
            return false;
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile).anyMatch(file -> nameMatches.test(file.getFileName().toString()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
