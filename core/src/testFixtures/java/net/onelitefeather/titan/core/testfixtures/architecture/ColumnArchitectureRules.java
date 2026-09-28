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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchRule;
import io.avaje.inject.BeanScope;
import io.avaje.inject.PostConstruct;
import jakarta.inject.Singleton;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventNode;

/**
 * The architecture rules every column shares: a small {@code ColumnArchitectureTest} in each
 * column applies these via {@code @ArchTest} to its own package, which a column's own
 * {@code @AnalyzeClasses} already scopes the import universe to - so these rules deliberately carry
 * no package restriction of their own.
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "net.onelitefeather.titan.feature.<column>", importOptions =
 * ImportOption.DoNotIncludeTests.class)
 * class ColumnArchitectureTest {
 * 
 * @ArchTest
 *           static final ArchRule featuresRegisterListenersOnlyThroughFeatureNode =
 *           ColumnArchitectureRules.FEATURES_REGISTER_LISTENERS_ONLY_THROUGH_FEATURE_NODE;
 * @ArchTest
 *           static final ArchRule classesWithPostConstructAreSingleton =
 *           ColumnArchitectureRules.CLASSES_WITH_POST_CONSTRUCT_ARE_SINGLETON;
 * @ArchTest
 *           static final ArchRule featureModulesDoNotUseBeanScope =
 *           ColumnArchitectureRules.FEATURE_MODULES_DO_NOT_USE_BEAN_SCOPE;
 *           }
 *           }</pre>
 */
public final class ColumnArchitectureRules {

    /** True for a call to the raw Minestom event API a feature must never reach for directly. */
    private static final DescribedPredicate<JavaMethodCall> TOUCHES_THE_RAW_EVENT_TREE = DescribedPredicate.describe("calls EventNode#addListener, EventNode#addChild or MinecraftServer#getGlobalEventHandler", ColumnArchitectureRules::touchesTheRawEventTree);

    /** True for a class that declares a method annotated with {@link PostConstruct}. */
    private static final DescribedPredicate<JavaClass> DECLARES_POST_CONSTRUCT = DescribedPredicate.describe("declares a method annotated with @PostConstruct", javaClass -> javaClass.getMethods().stream().anyMatch(method -> method.isAnnotatedWith(PostConstruct.class)));

    /** A feature registers a listener only through {@code core.module.FeatureNode}. */
    public static final ArchRule FEATURES_REGISTER_LISTENERS_ONLY_THROUGH_FEATURE_NODE = noClasses().should().callMethodWhere(TOUCHES_THE_RAW_EVENT_TREE).because("a feature must register listeners through FeatureNode, not the raw event API");

    /** A class with an {@code @PostConstruct} method must also be a {@code @Singleton} bean. */
    public static final ArchRule CLASSES_WITH_POST_CONSTRUCT_ARE_SINGLETON = classes().that(DECLARES_POST_CONSTRUCT).should().beAnnotatedWith(Singleton.class).because("a class with an @PostConstruct method must also be a @Singleton bean, or Avaje Inject never discovers it");

    /** A feature is built through constructor injection only, never via {@link BeanScope}. */
    public static final ArchRule FEATURE_MODULES_DO_NOT_USE_BEAN_SCOPE = noClasses().should().dependOnClassesThat().areAssignableTo(BeanScope.class).because("a feature must get its dependencies through its constructor, never via BeanScope");

    private ColumnArchitectureRules() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    private static boolean touchesTheRawEventTree(JavaMethodCall call) {
        JavaClass owner = call.getTargetOwner();
        String methodName = call.getTarget().getName();
        boolean onEventNode = owner.isAssignableTo(EventNode.class) && ("addListener".equals(methodName) || "addChild".equals(methodName));
        boolean globalHandlerLookup = owner.isEquivalentTo(MinecraftServer.class) && "getGlobalEventHandler".equals(methodName);
        return onEventNode || globalHandlerLookup;
    }
}
