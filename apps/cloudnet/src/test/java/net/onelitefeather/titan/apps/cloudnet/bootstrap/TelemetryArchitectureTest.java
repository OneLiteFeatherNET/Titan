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
package net.onelitefeather.titan.apps.cloudnet.bootstrap;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import net.onelitefeather.titan.core.testfixtures.architecture.ColumnArchitectureRules;

/**
 * The production variant has every module on its class path, so this is where the telemetry rules
 * see all of them at once.
 */
@AnalyzeClasses(packages = "net.onelitefeather.titan", importOptions = ImportOption.DoNotIncludeTests.class)
class TelemetryArchitectureTest {

    @ArchTest
    static final ArchRule onlyRuntimeCallsGlobalOpenTelemetry = ColumnArchitectureRules.ONLY_RUNTIME_CALLS_GLOBAL_OPEN_TELEMETRY;

    @ArchTest
    static final ArchRule featuresDoNotUseTheOpenTelemetrySdk = ColumnArchitectureRules.FEATURES_DO_NOT_USE_THE_OPEN_TELEMETRY_SDK;
}
