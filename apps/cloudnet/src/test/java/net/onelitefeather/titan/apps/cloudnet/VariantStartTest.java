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
package net.onelitefeather.titan.apps.cloudnet;

import io.avaje.inject.BeanScope;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minestom.server.coordinate.Vec;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.feature.portal.PortalModule;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.platform.luckperms.LuckPermsPermissionService;
import net.onelitefeather.titan.runtime.variant.LoadedModules;
import net.onelitefeather.titan.runtime.variant.VariantDescriptor;
import net.onelitefeather.titan.runtime.variant.VariantStartupCheck;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Start coverage for the {@code cloudnet} variant (spec {@code app-variants}, "Eine Variante
 * startet nur mit allen erwarteten Columns"): the full scope builds with every column
 * {@code META-INF/titan/variant.properties} names, and an additionally expected but missing
 * column aborts, naming it -
 * {@link net.onelitefeather.titan.runtime.bootstrap.WiringTest WiringTest} already covers the
 * exact feature/bean count, so this only exercises the variant-level check itself.
 */
@ExtendWith(MicrotusExtension.class)
class VariantStartTest {

    @DisplayName("The full scope builds with no exception")
    @Test
    @Timeout(30)
    void theFullScopeBuildsWithNoException(Env env) {
        // Named mock, not the plain mock(Type) overload - see docs/lobby-modules.md,
        // "Permission-Plattform".
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.empty()).mock(FeatureFlags.class).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).build();

        Assertions.assertDoesNotThrow(scope::close, "closing a fully built scope must not throw");
    }

    @DisplayName("Every column named in variant.properties is loaded")
    @Test
    void everyExpectedColumnIsLoaded() {
        Assertions.assertDoesNotThrow(() -> VariantStartupCheck.verify(getClass().getClassLoader()));
    }

    @DisplayName("variant.properties lists luckpermsPlatform among this variant's expected modules")
    @Test
    void variantPropertiesListsLuckpermsPlatform() {
        ClassLoader loader = getClass().getClassLoader();
        VariantDescriptor descriptor = VariantDescriptor.fromClasspath(loader).orElseThrow(() -> new AssertionError("this variant must ship META-INF/titan/variant.properties"));

        Assertions.assertTrue(descriptor.modules().contains("luckpermsPlatform"), "expected modules must include luckpermsPlatform, were: " + descriptor.modules());
    }

    @DisplayName("variant.properties lists seasonColumn and the season column is loaded")
    @Test
    void seasonColumnIsExpectedAndLoaded() {
        ClassLoader loader = getClass().getClassLoader();
        VariantDescriptor descriptor = VariantDescriptor.fromClasspath(loader).orElseThrow(() -> new AssertionError("this variant must ship META-INF/titan/variant.properties"));

        Assertions.assertTrue(descriptor.modules().contains("seasonColumn"), "expected modules must include seasonColumn, were: " + descriptor.modules());
        Assertions.assertTrue(LoadedModules.discover(loader).contains("seasonColumn"), "the season column must be on the classpath and load");
    }

    @DisplayName("variant.properties lists jumprunColumn and the jumprun column is loaded")
    @Test
    void jumprunColumnIsExpectedAndLoaded() {
        ClassLoader loader = getClass().getClassLoader();
        VariantDescriptor descriptor = VariantDescriptor.fromClasspath(loader).orElseThrow(() -> new AssertionError("this variant must ship META-INF/titan/variant.properties"));

        Assertions.assertTrue(descriptor.modules().contains("jumprunColumn"), "expected modules must include jumprunColumn, were: " + descriptor.modules());
        Assertions.assertTrue(LoadedModules.discover(loader).contains("jumprunColumn"), "the jumprun column must be on the classpath and load");
    }

    @DisplayName("The shipped application.yaml carries the seasons defaults")
    @Test
    void applicationYamlCarriesTheSeasonsDefaults() throws IOException {
        String yaml = new String(getClass().getClassLoader().getResourceAsStream("application.yaml").readAllBytes(), StandardCharsets.UTF_8);

        Assertions.assertTrue(yaml.contains("seasons:"), "the merged application.yaml must contain the seasons defaults");
    }

    @DisplayName("variant.properties lists portalColumn, the portal column is loaded and its module starts against the real LobbyPortals bean")
    @Test
    @Timeout(30)
    void portalColumnIsExpectedLoadedAndWiredToTheLobbyPortalsBean(Env env) {
        ClassLoader loader = getClass().getClassLoader();
        VariantDescriptor descriptor = VariantDescriptor.fromClasspath(loader).orElseThrow(() -> new AssertionError("this variant must ship META-INF/titan/variant.properties"));
        Portal portal = new Portal("survival", new Box(new Vec(0, 64, 0), new Vec(1, 65, 1)), "Survival", null);

        Assertions.assertTrue(descriptor.modules().contains("portalColumn"), "expected modules must include portalColumn, were: " + descriptor.modules());
        Assertions.assertTrue(LoadedModules.discover(loader).contains("portalColumn"), "the portal column must be on the classpath and load");

        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.with(List.of(portal))).mock(FeatureFlags.class).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).build();
        try {
            Assertions.assertNotNull(scope.get(PortalModule.class), "the portal module must be a bean");
            Assertions.assertEquals(List.of(portal), scope.get(LobbyPortals.class).portals(), "LobbyPortals must serve the portals of the active map");
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
