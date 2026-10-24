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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit coverage for {@link ArchitectureTestGuard} on a module directory in a {@code @TempDir} (F.I.R.S.T.). */
class ArchitectureTestGuardTest {

    private static final String MODULE = ":features:navigator";

    @TempDir
    Path moduleDir;

    @DisplayName("A module with production code and no MaintainabilityArchitectureTest is reported")
    @Test
    void aModuleWithProductionCodeNeedsItsArchitectureTest() throws IOException {
        write("src/main/java/net/onelitefeather/titan/feature/navigator/Navigator.java");

        Optional<String> violation = ArchitectureTestGuard.violation(moduleDir, MODULE);

        Assertions.assertTrue(violation.isPresent(), "a module without the architecture test should be reported");
        Assertions.assertTrue(violation.get().contains(MODULE), "the message should name the module: " + violation.get());
    }

    @DisplayName("A module with production code and a MaintainabilityArchitectureTest is accepted")
    @Test
    void aModuleWithItsArchitectureTestIsAccepted() throws IOException {
        write("src/main/java/net/onelitefeather/titan/feature/navigator/Navigator.java");
        write("src/test/java/net/onelitefeather/titan/feature/navigator/MaintainabilityArchitectureTest.java");

        Assertions.assertTrue(ArchitectureTestGuard.violation(moduleDir, MODULE).isEmpty(), "the architecture test is present");
    }

    @DisplayName("A column's ColumnArchitectureTest does not replace the MaintainabilityArchitectureTest")
    @Test
    void aColumnArchitectureTestDoesNotCountAsTheMaintainabilityTest() throws IOException {
        write("src/main/java/net/onelitefeather/titan/feature/navigator/Navigator.java");
        write("src/test/java/net/onelitefeather/titan/feature/navigator/ColumnArchitectureTest.java");

        Assertions.assertTrue(ArchitectureTestGuard.violation(moduleDir, MODULE).isPresent(), "a different architecture test is not enough");
    }

    @DisplayName("A module without production code needs no architecture test")
    @Test
    void aModuleWithoutProductionCodeIsAccepted() throws IOException {
        write("build.gradle.kts");

        Assertions.assertTrue(ArchitectureTestGuard.violation(moduleDir, ":apps:local").isEmpty(), "an app without Java sources is not checked");
    }

    private void write(String relativePath) throws IOException {
        Path file = moduleDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class Placeholder {}\n");
    }
}
