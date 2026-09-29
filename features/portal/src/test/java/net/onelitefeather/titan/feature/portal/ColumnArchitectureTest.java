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
package net.onelitefeather.titan.feature.portal;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import net.onelitefeather.titan.core.testfixtures.architecture.ColumnArchitectureRules;

/**
 * Applies the architecture rules every column shares to this column's own package, plus the
 * portal's own rule: portals are map data, the column reads no configuration.
 */
@AnalyzeClasses(packages = "net.onelitefeather.titan.feature.portal", importOptions = ImportOption.DoNotIncludeTests.class)
class ColumnArchitectureTest {

    @ArchTest
    static final ArchRule featuresRegisterListenersOnlyThroughFeatureNode = ColumnArchitectureRules.FEATURES_REGISTER_LISTENERS_ONLY_THROUGH_FEATURE_NODE;

    @ArchTest
    static final ArchRule classesWithPostConstructAreSingleton = ColumnArchitectureRules.CLASSES_WITH_POST_CONSTRUCT_ARE_SINGLETON;

    @ArchTest
    static final ArchRule featureModulesDoNotUseBeanScope = ColumnArchitectureRules.FEATURE_MODULES_DO_NOT_USE_BEAN_SCOPE;

    @ArchTest
    static final ArchRule portalDoesNotDependOnAvajeConfig = noClasses().should().dependOnClassesThat().resideInAPackage("io.avaje.config..").because("portals are map data, not configuration keys");

    ColumnArchitectureTest() {
    }
}
