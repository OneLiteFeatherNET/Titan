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
package net.onelitefeather.titan.core.telemetry;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.opentelemetry.api.GlobalOpenTelemetry;
import net.onelitefeather.titan.feature.fixture.UsesTheOpenTelemetrySdk;
import net.onelitefeather.titan.core.testfixtures.architecture.ColumnArchitectureRules;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves the telemetry architecture rules reject a violating class, so they cannot pass vacuously.
 */
class TelemetryArchitectureRulesTest {

    /** A class outside {@code runtime} that reads the global instance. */
    static final class ReadsTheGlobalInstance {

        Object read() {
            return GlobalOpenTelemetry.get();
        }
    }

    @DisplayName("A class outside runtime that uses GlobalOpenTelemetry violates the rule")
    @Test
    void globalOpenTelemetryOutsideRuntimeIsRejected() {
        JavaClasses classes = new ClassFileImporter().importClasses(ReadsTheGlobalInstance.class);

        Assertions.assertThrows(AssertionError.class, () -> ColumnArchitectureRules.ONLY_RUNTIME_CALLS_GLOBAL_OPEN_TELEMETRY.check(classes));
    }

    @DisplayName("A class that does not touch GlobalOpenTelemetry satisfies the rule")
    @Test
    void aCleanClassSatisfiesTheGlobalRule() {
        JavaClasses classes = new ClassFileImporter().importClasses(Telemetry.class);

        Assertions.assertDoesNotThrow(() -> ColumnArchitectureRules.ONLY_RUNTIME_CALLS_GLOBAL_OPEN_TELEMETRY.check(classes));
    }

    @DisplayName("A feature class that uses the OpenTelemetry SDK violates the rule")
    @Test
    void sdkUseInAFeatureIsRejected() {
        JavaClasses classes = new ClassFileImporter().importClasses(UsesTheOpenTelemetrySdk.class);

        Assertions.assertThrows(AssertionError.class, () -> ColumnArchitectureRules.FEATURES_DO_NOT_USE_THE_OPEN_TELEMETRY_SDK.check(classes));
    }
}
