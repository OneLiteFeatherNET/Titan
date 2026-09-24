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
package net.onelitefeather.titan.app.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import java.util.Optional;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventNode;
import net.onelitefeather.titan.app.Titan;
import net.onelitefeather.titan.app.TitanApplication;

/**
 * Enforces the module boundaries decided in
 * {@code openspec/changes/lobby-feature-modules/design.md}, decision 10, and the
 * {@code lobby-modules} spec requirement "Module sind voneinander unabhängig": feature modules
 * (protection, spawn, respawn, navigator, sit, tickle, elytra) neither depend on each other nor
 * leak internals, the platform and shared libraries never depend on a feature, and only the
 * platform touches the raw Minestom event tree.
 *
 * <p><strong>Scope of {@link #onlyThePlatformRegistersListenersDirectly}:</strong> this class
 * analyzes the whole {@code net.onelitefeather.titan} codebase (see {@link AnalyzeClasses}
 * above), which also covers {@code common} and the separate {@code setup} artifact. Direct calls
 * to {@code EventNode#addListener}/{@code #addChild} or
 * {@code MinecraftServer#getGlobalEventHandler()} exist there too, outside
 * {@code app.module} and outside this module system entirely:
 * {@code common.map.MapProvider} registers a chunk-load relight listener directly on a world
 * instance's own event node, and {@code setup.Titan} is the {@code setup} artifact's own,
 * unrelated composition root with its own small, hand-wired listener set. Both are legitimate,
 * already-reviewed direct uses of the Minestom event API that predate and sit outside the lobby
 * module system this rule protects. Rather than silently widening the rule to allow direct
 * registration anywhere, the rule below is deliberately scoped to
 * {@code net.onelitefeather.titan.app..} only, so it keeps guarding the one place - the lobby
 * app - where {@link net.onelitefeather.titan.app.module.ModuleContext#listen} must be the only
 * door to the event tree.
 */
@AnalyzeClasses(packages = "net.onelitefeather.titan", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String FEATURE_PACKAGE = "net.onelitefeather.titan.app.feature..";
    private static final String MODULE_PACKAGE = "net.onelitefeather.titan.app.module..";
    private static final String COMMON_PACKAGE = "net.onelitefeather.titan.common..";
    private static final String APP_PACKAGE = "net.onelitefeather.titan.app..";

    /**
     * True for a class named {@code *Module}, a class named {@code *Config}, or a class nested
     * (directly or transitively) inside a class named {@code *Config}.
     */
    private static final DescribedPredicate<JavaClass> PUBLIC_FEATURE_API = DescribedPredicate.describe("named '*Module' or '*Config', or nested in a class named '*Config'", ArchitectureTest::isModuleOrConfigOrNestedInConfig);

    /** True for {@link Titan} or {@link TitanApplication}, the lobby's composition root. */
    private static final DescribedPredicate<JavaClass> IS_COMPOSITION_ROOT = DescribedPredicate.describe("is the composition root (Titan or TitanApplication)", javaClass -> javaClass.isEquivalentTo(Titan.class) || javaClass.isEquivalentTo(TitanApplication.class));

    /**
     * True for a class residing in {@code net.onelitefeather.titan.app..} that is neither part of
     * the module platform ({@code app.module..}) nor the composition root ({@link Titan} or
     * {@link TitanApplication}).
     */
    private static final DescribedPredicate<JavaClass> MUST_NOT_REGISTER_LISTENERS_DIRECTLY = JavaClass.Predicates.resideInAPackage(APP_PACKAGE).and(DescribedPredicate.not(JavaClass.Predicates.resideInAPackage(MODULE_PACKAGE))).and(DescribedPredicate.not(IS_COMPOSITION_ROOT));

    /**
     * True for a method call to {@code EventNode#addListener}, {@code EventNode#addChild} (which
     * also covers {@code GlobalEventHandler}, since it extends {@code EventNode}), or
     * {@code MinecraftServer#getGlobalEventHandler()}.
     */
    private static final DescribedPredicate<JavaMethodCall> REGISTERS_LISTENERS_OR_LOOKS_UP_GLOBAL_HANDLER = DescribedPredicate.describe("calls EventNode#addListener, EventNode#addChild or MinecraftServer#getGlobalEventHandler", ArchitectureTest::registersListenersOrLooksUpGlobalHandler);

    /**
     * Rule 1 (design.md decision 10.1): a feature must never reach into another feature's
     * package. Anything two features both need belongs behind a {@code ModuleContext} dock point
     * or in a shared library outside {@code app.feature}.
     */
    @ArchTest
    static final ArchRule featureModulesDoNotDependOnEachOther = slices().matching("net.onelitefeather.titan.app.feature.(*)..").should().notDependOnEachOther().because("each feature module (protection, spawn, respawn, navigator, sit, tickle, elytra) must stand on its own; a feature needing something from another one has to go through the module context's dock points (listen/config/items/navigator/commands/tasks) or a shared library outside app.feature - see design.md decision 10.1 and the lobby-modules spec requirement \"Module sind voneinander unabhängig\"");

    /**
     * Rule 2 (design.md decision 10.2): the platform ({@code app.module}) and the shared
     * libraries ({@code titan.common}) are what feature modules depend on, never the reverse.
     */
    @ArchTest
    static final ArchRule platformAndCommonDoNotDependOnFeatures = noClasses().that().resideInAnyPackage(MODULE_PACKAGE, COMMON_PACKAGE).should().dependOnClassesThat().resideInAPackage(FEATURE_PACKAGE).because("the platform (app.module) and the shared libraries (titan.common) must stay usable without any particular feature present; a platform or common class referencing a feature type would make the platform's behaviour depend on which features happen to be registered - see design.md decision 10.2 and the lobby-modules spec requirement \"Gemeinsame Bibliotheken DÜRFEN NICHT von Feature-Modulen abhängen\"");

    /**
     * Rule 3 (design.md decision 10.3): a feature's only declared surface is its {@code *Module}
     * entry point and its {@code *Config} record(s) (plus any type nested inside a
     * {@code *Config}, such as a nested record describing one config entry). Everything else -
     * listeners, helpers, internal state - stays package-private so no other feature or platform
     * class can reach into it.
     */
    @ArchTest
    static final ArchRule onlyModuleAndConfigTypesArePublicInFeatures = classes().that().resideInAPackage(FEATURE_PACKAGE).and().haveModifier(JavaModifier.PUBLIC).should(ArchConditions.be(PUBLIC_FEATURE_API)).because("a feature module's declared surface is its *Module entry point and its *Config record(s); everything else must be package-private so it cannot be depended on from outside the feature - see design.md decision 10.3");

    /**
     * Rule 4 (design.md decision 10.4): only the module platform is allowed to touch the raw
     * Minestom event tree. A feature module registers exclusively through
     * {@link net.onelitefeather.titan.app.module.ModuleContext#listen}, which guarantees cleanup
     * and error attribution on disable; registering directly would bypass both. See also this
     * class's Javadoc for why the rule is scoped to {@code net.onelitefeather.titan.app..} rather
     * than the whole analyzed codebase.
     */
    @ArchTest
    static final ArchRule onlyThePlatformRegistersListenersDirectly = noClasses().that(MUST_NOT_REGISTER_LISTENERS_DIRECTLY).should().callMethodWhere(REGISTERS_LISTENERS_OR_LOOKS_UP_GLOBAL_HANDLER).because("no class outside net.onelitefeather.titan.app.module.. (and outside the composition root Titan/TitanApplication) may call EventNode#addListener, EventNode#addChild or MinecraftServer#getGlobalEventHandler; a feature module registers through ModuleContext#listen instead, which guarantees cleanup and error attribution on disable - see design.md decision 10.4 and the lobby-modules spec requirement \"Keine Listener-Registrierung zur Laufzeit\". Scoped to net.onelitefeather.titan.app.. only; see this class's Javadoc for why common.map.MapProvider and the separate setup.Titan composition root are out of scope rather than silently exempted everywhere");

    private ArchitectureTest() {
    }

    private static boolean registersListenersOrLooksUpGlobalHandler(JavaMethodCall call) {
        JavaClass owner = call.getTargetOwner();
        String methodName = call.getTarget().getName();
        boolean onEventNode = owner.isAssignableTo(EventNode.class) && ("addListener".equals(methodName) || "addChild".equals(methodName));
        boolean globalHandlerLookup = owner.isEquivalentTo(MinecraftServer.class) && "getGlobalEventHandler".equals(methodName);
        return onEventNode || globalHandlerLookup;
    }

    private static boolean isModuleOrConfigOrNestedInConfig(JavaClass javaClass) {
        if (isModuleType(javaClass) || isConfigType(javaClass)) {
            return true;
        }
        Optional<JavaClass> enclosing = javaClass.getEnclosingClass();
        while (enclosing.isPresent()) {
            if (isConfigType(enclosing.get())) {
                return true;
            }
            enclosing = enclosing.get().getEnclosingClass();
        }
        return false;
    }

    private static boolean isModuleType(JavaClass javaClass) {
        return javaClass.getSimpleName().endsWith("Module");
    }

    private static boolean isConfigType(JavaClass javaClass) {
        return javaClass.getSimpleName().endsWith("Config");
    }
}
