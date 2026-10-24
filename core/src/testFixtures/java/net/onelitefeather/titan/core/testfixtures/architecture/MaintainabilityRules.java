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
package net.onelitefeather.titan.core.testfixtures.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import net.onelitefeather.titan.core.testfixtures.EventListenerCounter;

/**
 * The maintainability rules Titan enforces with ArchUnit (openspec {@code maintainability-gates},
 * design D3). Every rule takes the scope it applies to, so a module test uses the real scope while
 * a fixture test applies the same rule to a class that breaks it. The shared constants use the
 * scope a rule is meant for.
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "net.onelitefeather.titan.feature.navigator")
 * class MaintainabilityArchitectureTest {
 *     @ArchTest
 *     static final ArchRule noSystemClock = MaintainabilityRules.NO_SYSTEM_CLOCK;
 * }
 * }</pre>
 */
public final class MaintainabilityRules {

    private static final String FIXTURE_PACKAGE = "net.onelitefeather.titan.core.architecture.fixture";

    /**
     * Classes compiled from {@code src/test}, without the rule fixtures of this module. Gradle puts
     * a module's own classes on a test classpath either as directories or as jars, so both forms
     * are matched.
     */
    public static final DescribedPredicate<JavaClass> TEST_CODE = locatedIn("test code (src/test)", "/classes/java/test/")
            .and(DescribedPredicate.describe("outside " + FIXTURE_PACKAGE, javaClass -> !javaClass.getPackageName().startsWith(FIXTURE_PACKAGE)));

    /** Every class compiled from {@code src/test}, including the rule fixtures; the scopes below exclude all of them. */
    private static final DescribedPredicate<JavaClass> TEST_CLASSES = locatedIn("test classes (src/test)", "/classes/java/test/");

    /** Classes compiled from {@code src/testFixtures}, as a directory or as the test-fixtures jar. */
    private static final DescribedPredicate<JavaClass> TEST_FIXTURE_CODE = locatedIn("test-fixture code (src/testFixtures)", "/classes/java/testFixtures/")
            .or(locatedIn("test-fixture jar", "-test-fixtures.jar"));

    /** Classes compiled from {@code src/main}: the code that ships. */
    public static final DescribedPredicate<JavaClass> MAIN_CODE = TEST_CLASSES.negate().and(TEST_FIXTURE_CODE.negate())
            .as("production code (src/main)");

    /** Classes compiled from {@code src/main} or {@code src/testFixtures}: code other tests build on. */
    public static final DescribedPredicate<JavaClass> SHARED_CODE = TEST_CLASSES.negate().as("production or test-fixture code");

    /** Production code must not bypass access modifiers. The fixture test names {@link EventListenerCounter} as its exception. */
    public static final ArchRule NO_REFLECTION_BYPASS = noReflectionBypass(SHARED_CODE);

    /** Production code must not read the system clock; time comes from an injected {@code Clock}. */
    public static final ArchRule NO_SYSTEM_CLOCK = noSystemClock(MAIN_CODE);

    /** Tests must not wait on the wall clock; they drive time explicitly. */
    public static final ArchRule NO_THREAD_SLEEP_IN_TESTS = noThreadSleep(TEST_CODE);

    /** Loggers are {@code private static final}, so every class has exactly one logger. */
    public static final ArchRule LOGGERS_ARE_PRIVATE_STATIC_FINAL = loggersArePrivateStaticFinal(MAIN_CODE);

    /** Production code logs through SLF4J; {@code System.getLogger} is not used. */
    public static final ArchRule NO_SYSTEM_GET_LOGGER = noSystemGetLogger(MAIN_CODE);

    private MaintainabilityRules() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static ArchRule noReflectionBypass(DescribedPredicate<JavaClass> scope) {
        return noClasses().that(scope)
                .and().doNotHaveFullyQualifiedName(EventListenerCounter.class.getName())
                .should().callMethod(Field.class, "setAccessible", boolean.class)
                .orShould().callMethod(Method.class, "setAccessible", boolean.class)
                .orShould().callMethod(Constructor.class, "setAccessible", boolean.class)
                .orShould().callMethod(AccessibleObject.class, "setAccessible", boolean.class)
                .because("setAccessible bypasses the access modifiers; EventListenerCounter is the one named exception, for a test-only read of a private field").allowEmptyShould(true);
    }

    public static ArchRule noSystemClock(DescribedPredicate<JavaClass> scope) {
        return noClasses().that(scope)
                .should().callMethod(Instant.class, "now")
                .orShould().callMethod(LocalDate.class, "now")
                .orShould().callMethod(LocalDateTime.class, "now")
                .orShould().callMethod(LocalTime.class, "now")
                .orShould().callMethod(ZonedDateTime.class, "now")
                .orShould().callMethod(OffsetDateTime.class, "now")
                .orShould().callMethod(System.class, "currentTimeMillis")
                .because("the time comes from an injected java.time.Clock, so tests can fix it; System.nanoTime for durations stays allowed").allowEmptyShould(true);
    }

    public static ArchRule noThreadSleep(DescribedPredicate<JavaClass> scope) {
        return noClasses().that(scope)
                .should().callMethod(Thread.class, "sleep", long.class)
                .orShould().callMethod(Thread.class, "sleep", long.class, int.class)
                .orShould().callMethod(Thread.class, "sleep", Duration.class)
                .because("a test that sleeps is slow and depends on the machine; drive time with a Clock and explicit ticks").allowEmptyShould(true);
    }

    public static ArchRule loggersArePrivateStaticFinal(DescribedPredicate<JavaClass> scope) {
        // Matched by name for SLF4J: consumers of this rule need not have SLF4J on their compile classpath.
        DescribedPredicate<JavaField> loggerField = DescribedPredicate.describe("of a logger type",
                (JavaField field) -> field.getRawType().getFullName().equals("org.slf4j.Logger")
                        || field.getRawType().isEquivalentTo(System.Logger.class));
        return fields().that(loggerField).and().areDeclaredInClassesThat(scope)
                .should().bePrivate()
                .andShould().beStatic()
                .andShould().beFinal()
                .because("a logger is one static constant per class; an instance logger is created per object and is easy to leak into the wrong class").allowEmptyShould(true);
    }

    public static ArchRule noSystemGetLogger(DescribedPredicate<JavaClass> scope) {
        return noClasses().that(scope)
                .should().callMethod(System.class, "getLogger", String.class)
                .because("the project logs through SLF4J; System.getLogger would add a second logging API").allowEmptyShould(true);
    }

    /**
     * No cycle between the production packages below {@code basePackage}; the freezing rule keeps the
     * existing ones. Test code is left out: a test that uses its own fixtures is not a design cycle.
     */
    public static ArchRule packagesAreFreeOfCycles(String basePackage) {
        return packagesAreFreeOfCycles(basePackage, MAIN_CODE);
    }

    public static ArchRule packagesAreFreeOfCycles(String basePackage, DescribedPredicate<JavaClass> scope) {
        return slices().assignedFrom(productionSlices(basePackage, scope))
                .should().beFreeOfCycles()
                .because("packages that depend on each other cannot be understood, changed or tested apart").allowEmptyShould(true);
    }

    /** A slice is the first package segment below {@code basePackage}; classes outside the scope and the base package itself are ignored. */
    private static SliceAssignment productionSlices(String basePackage, DescribedPredicate<JavaClass> scope) {
        return new SliceAssignment() {
            @Override
            public String getDescription() {
                return "packages below " + basePackage;
            }

            @Override
            public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
                String packageName = javaClass.getPackageName();
                if (!scope.test(javaClass) || !packageName.startsWith(basePackage + ".")) {
                    return SliceIdentifier.ignore();
                }
                String firstSegment = packageName.substring(basePackage.length() + 1).split("\\.")[0];
                return SliceIdentifier.of(basePackage + "." + firstSegment);
            }
        };
    }

    private static DescribedPredicate<JavaClass> locatedIn(String description, String classesDirectory) {
        return DescribedPredicate.describe(description, javaClass -> javaClass.getSource()
                .map(source -> String.valueOf(source.getUri()).contains(classesDirectory))
                .orElse(false));
    }
}
