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
package net.onelitefeather.titan.core.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import net.onelitefeather.titan.core.architecture.fixture.acyclic.one.One;
import net.onelitefeather.titan.core.architecture.fixture.acyclic.two.Two;
import net.onelitefeather.titan.core.architecture.fixture.clock.ReadsSystemClock;
import net.onelitefeather.titan.core.architecture.fixture.compliant.CompliantProductionClass;
import net.onelitefeather.titan.core.architecture.fixture.cycle.first.First;
import net.onelitefeather.titan.core.architecture.fixture.cycle.second.Second;
import net.onelitefeather.titan.core.architecture.fixture.logger.InstanceLogger;
import net.onelitefeather.titan.core.architecture.fixture.logger.SystemLogger;
import net.onelitefeather.titan.core.architecture.fixture.reflection.BypassesAccessModifier;
import net.onelitefeather.titan.core.architecture.fixture.sleep.SleepsInTest;
import net.onelitefeather.titan.core.testfixtures.EventListenerCounter;
import net.onelitefeather.titan.core.testfixtures.architecture.MaintainabilityRules;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Each maintainability rule is shown to report a class that breaks it and to accept a class that
 * follows it. The fixtures live in {@code architecture.fixture}; the rules are applied to them with
 * an all-accepting scope, because the fixtures are test code and the real scopes exclude them.
 */
class MaintainabilityRulesTest {

    private static final DescribedPredicate<JavaClass> ANY_CLASS = DescribedPredicate.alwaysTrue();

    @DisplayName("Reflection that bypasses access modifiers is reported")
    @Test
    void reflectionBypassIsReported() {
        assertViolated(MaintainabilityRules.noReflectionBypass(ANY_CLASS), BypassesAccessModifier.class);
    }

    @DisplayName("Code without reflection is accepted by the reflection rule")
    @Test
    void codeWithoutReflectionIsAccepted() {
        assertSatisfied(MaintainabilityRules.noReflectionBypass(ANY_CLASS), CompliantProductionClass.class);
    }

    @DisplayName("EventListenerCounter, the named exception, is accepted by the reflection rule")
    @Test
    void theNamedReflectionExceptionIsAccepted() {
        // A second class keeps the rule from failing on an empty scope once the exception is filtered out.
        assertSatisfied(MaintainabilityRules.noReflectionBypass(ANY_CLASS), EventListenerCounter.class, CompliantProductionClass.class);
    }

    @DisplayName("Reading the system clock is reported")
    @Test
    void systemClockIsReported() {
        assertViolated(MaintainabilityRules.noSystemClock(ANY_CLASS), ReadsSystemClock.class);
    }

    @DisplayName("Reading an injected clock is accepted by the system clock rule")
    @Test
    void injectedClockIsAccepted() {
        assertSatisfied(MaintainabilityRules.noSystemClock(ANY_CLASS), CompliantProductionClass.class);
    }

    @DisplayName("A test that calls Thread.sleep is reported")
    @Test
    void threadSleepInTestIsReported() {
        assertViolated(MaintainabilityRules.noThreadSleep(ANY_CLASS), SleepsInTest.class);
    }

    @DisplayName("A test that does not sleep is accepted by the sleep rule")
    @Test
    void testWithoutSleepIsAccepted() {
        assertSatisfied(MaintainabilityRules.noThreadSleep(ANY_CLASS), CompliantProductionClass.class);
    }

    @DisplayName("An instance logger is reported")
    @Test
    void instanceLoggerIsReported() {
        assertViolated(MaintainabilityRules.loggersArePrivateStaticFinal(ANY_CLASS), InstanceLogger.class);
    }

    @DisplayName("A private static final logger is accepted by the logger rule")
    @Test
    void staticFinalLoggerIsAccepted() {
        assertSatisfied(MaintainabilityRules.loggersArePrivateStaticFinal(ANY_CLASS), CompliantProductionClass.class);
    }

    @DisplayName("System.getLogger is reported")
    @Test
    void systemGetLoggerIsReported() {
        assertViolated(MaintainabilityRules.noSystemGetLogger(ANY_CLASS), SystemLogger.class);
    }

    @DisplayName("Packages that depend on each other in a cycle are reported")
    @Test
    void packageCycleIsReported() {
        assertViolated(MaintainabilityRules.packagesAreFreeOfCycles("net.onelitefeather.titan.core.architecture.fixture.cycle", ANY_CLASS), First.class, Second.class);
    }

    @DisplayName("Packages that depend on each other in one direction only are accepted")
    @Test
    void packageDependencyWithoutCycleIsAccepted() {
        assertSatisfied(MaintainabilityRules.packagesAreFreeOfCycles("net.onelitefeather.titan.core.architecture.fixture.acyclic", ANY_CLASS), One.class, Two.class);
    }

    private static void assertViolated(ArchRule rule, Class<?>... fixtures) {
        Assertions.assertThrows(AssertionError.class, () -> rule.check(new ClassFileImporter().importClasses(fixtures)), "the rule should report " + fixtures[0].getSimpleName());
    }

    private static void assertSatisfied(ArchRule rule, Class<?>... fixtures) {
        Assertions.assertDoesNotThrow(() -> rule.check(new ClassFileImporter().importClasses(fixtures)), "the rule should accept " + fixtures[0].getSimpleName());
    }
}
