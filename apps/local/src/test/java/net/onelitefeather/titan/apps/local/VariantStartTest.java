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
package net.onelitefeather.titan.apps.local;

import io.avaje.inject.BeanScope;
import java.util.ArrayList;
import java.util.List;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.runtime.variant.LoadedModules;
import net.onelitefeather.titan.runtime.variant.VariantDescriptor;
import net.onelitefeather.titan.runtime.variant.VariantStartupCheck;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Start coverage for the {@code local} variant (spec {@code app-variants}, "Eine Variante startet
 * nur mit allen erwarteten Columns" and "Entwicklungsvariante startet"): the full scope builds
 * with every column {@code META-INF/titan/variant.properties} names, exactly like {@code
 * cloudnet}'s own {@code VariantStartTest}, and an additionally expected but missing column
 * aborts, naming it.
 */
@ExtendWith(MicrotusExtension.class)
class VariantStartTest {

    @DisplayName("The full scope builds with no exception")
    @Test
    @Timeout(30)
    void theFullScopeBuildsWithNoException(Env env) {
        // Named mock matching LuckPermsPermissionService.QUALIFIER - see docs/lobby-modules.md,
        // "Permission-Plattform" (platform/luckperms is not always on this module's classpath, so
        // the name is a literal here rather than the constant).
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class).mock(FeatureFlags.class).mock(PermissionService.class, "luckperms").build();

        Assertions.assertDoesNotThrow(scope::close, "closing a fully built scope must not throw");
    }

    @DisplayName("Every column named in variant.properties is loaded")
    @Test
    void everyExpectedColumnIsLoaded() {
        Assertions.assertDoesNotThrow(() -> VariantStartupCheck.verify(getClass().getClassLoader()));
    }

    @DisplayName("Without -Ptitan.luckperms, the active permission service is deny-all")
    @Test
    @Timeout(30)
    void theActiveServiceIsDenyAllWithoutTheSwitch(Env env) {
        // Only meaningful for the default build: with -Ptitan.luckperms, luckpermsPlatform is on
        // the classpath and would be the active service instead - and building an unmocked scope
        // would try to start real LuckPerms, which titan.app-variant keeps off this module's own
        // test runtime classpath (see platform/luckperms's Gson exclude).
        Assumptions.assumeFalse(LoadedModules.discover(getClass().getClassLoader()).contains("luckpermsPlatform"), "only meaningful without -Ptitan.luckperms");

        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class).mock(FeatureFlags.class).build();
        try {
            Assertions.assertEquals("deny-all", scope.get(PermissionService.class).name(), "without a permission platform, the fallback deny-all service must be active");
        } finally {
            scope.close();
        }
    }

    @DisplayName("An additionally expected but missing column aborts startup, naming it")
    @Test
    void anAdditionallyExpectedButMissingColumnAbortsNamingIt() {
        ClassLoader loader = getClass().getClassLoader();
        VariantDescriptor real = VariantDescriptor.fromClasspath(loader).orElseThrow(() -> new AssertionError("this variant must ship META-INF/titan/variant.properties"));
        List<String> modulesWithAGhost = new ArrayList<>(real.modules());
        modulesWithAGhost.add("ghostColumn");
        VariantDescriptor withAMissingModule = VariantDescriptor.of(real.name(), modulesWithAGhost);

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> VariantStartupCheck.verify(withAMissingModule, LoadedModules.discover(loader)));

        Assertions.assertTrue(thrown.getMessage().contains("ghostColumn"), "the message must name the missing column, was: " + thrown.getMessage());
    }
}
