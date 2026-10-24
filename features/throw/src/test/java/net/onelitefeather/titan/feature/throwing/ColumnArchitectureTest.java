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
package net.onelitefeather.titan.feature.throwing;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import net.onelitefeather.titan.core.testfixtures.architecture.ColumnArchitectureRules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Applies the architecture rules every column shares to this column's own package. */
@AnalyzeClasses(packages = "net.onelitefeather.titan.feature.throwing", importOptions = ImportOption.DoNotIncludeTests.class)
class ColumnArchitectureTest {

    private static final String OWN_PACKAGE = "net.onelitefeather.titan.feature.throwing..";

    @ArchTest
    static final ArchRule featuresRegisterListenersOnlyThroughFeatureNode = ColumnArchitectureRules.FEATURES_REGISTER_LISTENERS_ONLY_THROUGH_FEATURE_NODE;

    @ArchTest
    static final ArchRule classesWithPostConstructAreSingleton = ColumnArchitectureRules.CLASSES_WITH_POST_CONSTRUCT_ARE_SINGLETON;

    @ArchTest
    static final ArchRule featureModulesDoNotUseBeanScope = ColumnArchitectureRules.FEATURE_MODULES_DO_NOT_USE_BEAN_SCOPE;

    // A column depends on core only: no other column may be reached from here.
    @ArchTest
    static final ArchRule dependsOnNoOtherFeature = noClasses().that().resideInAPackage(OWN_PACKAGE).should().dependOnClassesThat(JavaClass.Predicates.resideInAPackage("net.onelitefeather.titan.feature..").and(JavaClass.Predicates.resideOutsideOfPackage(OWN_PACKAGE))).because("a column may depend only on core; features must not reach into each other");

    ColumnArchitectureTest() {
    }
}
