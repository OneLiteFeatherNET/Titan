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

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import net.onelitefeather.titan.core.testfixtures.architecture.ColumnArchitectureRules;

/**
 * Applies the architecture rules every column shares to this column's own package.
 *
 * <p>{@code featuresRegisterListenersOnlyThroughFeatureNode} is deliberately not applied here:
 * {@link HotbarLobbyItems} is this column's platform-wide item dispatcher, not a
 * {@code FeatureNode}-scoped feature - it registers once on the shared {@code titan} node for the
 * lifetime of the lobby and attributes a failure to the using item's own feature id, not to
 * itself. Wrapping it in a {@code FeatureNode} would attribute every failure to {@code hotbar}
 * instead, which {@code HotbarLobbyItemsIntegrationTest} guards against.
 */
@AnalyzeClasses(packages = "net.onelitefeather.titan.feature.hotbar", importOptions = ImportOption.DoNotIncludeTests.class)
class ColumnArchitectureTest {

    @ArchTest
    static final ArchRule classesWithPostConstructAreSingleton = ColumnArchitectureRules.CLASSES_WITH_POST_CONSTRUCT_ARE_SINGLETON.allowEmptyShould(true);

    @ArchTest
    static final ArchRule featureModulesDoNotUseBeanScope = ColumnArchitectureRules.FEATURE_MODULES_DO_NOT_USE_BEAN_SCOPE;

    ColumnArchitectureTest() {
    }
}
