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

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Plain-Java unit coverage for {@link ModuleBoundaries}: no Gradle project needed (F.I.R.S.T.). */
class ModuleBoundariesTest {

    @DisplayName("A column may not depend on another column")
    @Test
    void aColumnMayNotDependOnAnotherColumn() {
        assertViolation(":features:navigator", ":features:jumprun");
    }

    @DisplayName("A column may not depend on runtime")
    @Test
    void aColumnMayNotDependOnRuntime() {
        assertViolation(":features:navigator", ":runtime");
    }

    @DisplayName("A column may not depend on an app variant")
    @Test
    void aColumnMayNotDependOnAnAppVariant() {
        assertViolation(":features:navigator", ":apps:cloudnet");
    }

    @DisplayName("A permission platform may not depend on a feature column")
    @Test
    void aPlatformMayNotDependOnAFeatureColumn() {
        assertViolation(":platform:cloudnet", ":features:navigator");
    }

    @DisplayName("common may not depend on runtime")
    @Test
    void commonMayNotDependOnRuntime() {
        assertViolation(":common", ":runtime");
    }

    @DisplayName("A column may depend on core")
    @Test
    void aColumnMayDependOnCore() {
        assertAllowed(":features:navigator", ":core");
    }

    @DisplayName("A column may depend on persistence")
    @Test
    void aColumnMayDependOnPersistence() {
        assertAllowed(":features:jumprun", ":persistence");
    }

    @DisplayName("A column may use the test fixtures of core, which count as core")
    @Test
    void aColumnMayUseTheTestFixturesOfCore() {
        assertAllowed(":features:navigator", ":core");
    }

    @DisplayName("A permission platform may depend on common and platform extensions")
    @Test
    void aPlatformMayDependOnCommonAndExtensions() {
        assertAllowed(":platform:cloudnet", ":common");
        assertAllowed(":platform:cloudnet", ":platform:extensions");
    }

    @DisplayName("An app variant may depend on any module")
    @Test
    void anAppVariantMayDependOnAnyModule() {
        assertAllowed(":apps:local", ":features:navigator");
        assertAllowed(":apps:local", ":runtime");
    }

    @DisplayName("Persistence may use the core test fixtures for its architecture test, but nothing else of core is declared")
    @Test
    void persistenceMayUseCoreTestFixtures() {
        assertAllowed(":persistence", ":core");
        assertViolation(":persistence", ":features:navigator");
    }

    @DisplayName("A module may always depend on itself, for example through its own test fixtures")
    @Test
    void aModuleMayDependOnItself() {
        assertAllowed(":persistence", ":persistence");
    }

    @DisplayName("The violation message names the source and the target module")
    @Test
    void theViolationMessageNamesSourceAndTarget() {
        String message = ModuleBoundaries.violation(":features:navigator", ":features:jumprun").orElseThrow();

        Assertions.assertTrue(message.contains(":features:navigator"), "message should name the source: " + message);
        Assertions.assertTrue(message.contains(":features:jumprun"), "message should name the target: " + message);
    }

    @DisplayName("A module that is not classified is rejected instead of silently allowed")
    @Test
    void anUnclassifiedModuleIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ModuleBoundaries.violation(":tools:unknown", ":core"));
    }

    private static void assertViolation(String source, String target) {
        Optional<String> violation = ModuleBoundaries.violation(source, target);

        Assertions.assertTrue(violation.isPresent(), source + " -> " + target + " should be reported as a violation");
    }

    private static void assertAllowed(String source, String target) {
        Optional<String> violation = ModuleBoundaries.violation(source, target);

        Assertions.assertTrue(violation.isEmpty(), source + " -> " + target + " should be allowed but was: " + violation.orElse(""));
    }
}
