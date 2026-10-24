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
package net.onelitefeather.titan.feature.hotbar;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import net.onelitefeather.titan.core.testfixtures.architecture.MaintainabilityRules;

/**
 * Applies the shared maintainability rules to this module (openspec maintainability-gates). The
 * rules are written out per module so the gates are visible here; their definitions live in
 * {@link MaintainabilityRules}.
 */
@AnalyzeClasses(packages = "net.onelitefeather.titan.feature.hotbar")
class MaintainabilityArchitectureTest {

    @ArchTest
    static final ArchRule noReflectionBypass = MaintainabilityRules.NO_REFLECTION_BYPASS;

    @ArchTest
    static final ArchRule noSystemClock = MaintainabilityRules.NO_SYSTEM_CLOCK;

    @ArchTest
    static final ArchRule noThreadSleepInTests = MaintainabilityRules.NO_THREAD_SLEEP_IN_TESTS;

    @ArchTest
    static final ArchRule loggersArePrivateStaticFinal = MaintainabilityRules.LOGGERS_ARE_PRIVATE_STATIC_FINAL;

    @ArchTest
    static final ArchRule noSystemGetLogger = MaintainabilityRules.NO_SYSTEM_GET_LOGGER;

    @ArchTest
    static final ArchRule packagesAreFreeOfCycles = FreezingArchRule.freeze(MaintainabilityRules.packagesAreFreeOfCycles("net.onelitefeather.titan.feature.hotbar"));
}
