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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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
import net.onelitefeather.titan.core.module.LobbyWorldChoice;
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
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.empty()).mock(FeatureFlags.class).mock(PermissionService.class, "luckperms").build();

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

        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.empty()).mock(FeatureFlags.class).build();
        try {
            Assertions.assertEquals("deny-all", scope.get(PermissionService.class).name(), "without a permission platform, the fallback deny-all service must be active");
        } finally {
            scope.close();
        }
    }

    @DisplayName("variant.properties lists jumprunColumn and the jumprun column is loaded")
    @Test
    void jumprunColumnIsExpectedAndLoaded() {
        ClassLoader loader = getClass().getClassLoader();
        VariantDescriptor descriptor = VariantDescriptor.fromClasspath(loader).orElseThrow(() -> new AssertionError("this variant must ship META-INF/titan/variant.properties"));

        Assertions.assertTrue(descriptor.modules().contains("jumprunColumn"), "expected modules must include jumprunColumn, were: " + descriptor.modules());
        Assertions.assertTrue(LoadedModules.discover(loader).contains("jumprunColumn"), "the jumprun column must be on the classpath and load");
    }

    @DisplayName("The season column is neither expected nor on the classpath")
    @Test
    void seasonColumnIsNotPartOfThisVariant() {
        ClassLoader loader = getClass().getClassLoader();
        VariantDescriptor descriptor = VariantDescriptor.fromClasspath(loader).orElseThrow(() -> new AssertionError("this variant must ship META-INF/titan/variant.properties"));

        Assertions.assertFalse(descriptor.modules().contains("seasonColumn"), "expected modules must not include seasonColumn, were: " + descriptor.modules());
        Assertions.assertFalse(LoadedModules.discover(loader).contains("seasonColumn"), "the season column must not load in this variant");
        Assertions.assertThrows(ClassNotFoundException.class, () -> Class.forName("net.onelitefeather.titan.feature.season.SeasonWorldChoice", false, loader), "the season classes must not be on the classpath");
    }

    @DisplayName("No LobbyWorldChoice service entry is on the classpath")
    @Test
    void noLobbyWorldChoiceServiceEntry() throws IOException {
        List<URL> entries = Collections.list(getClass().getClassLoader().getResources("META-INF/services/" + LobbyWorldChoice.class.getName()));

        Assertions.assertTrue(entries.isEmpty(), "this variant must not register a LobbyWorldChoice, found: " + entries);
    }

    @DisplayName("The shipped application.yaml has no seasons defaults")
    @Test
    void applicationYamlHasNoSeasonsDefaults() throws IOException {
        for (URL url : Collections.list(getClass().getClassLoader().getResources("application.yaml"))) {
            try (InputStream in = url.openStream()) {
                String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                Assertions.assertFalse(yaml.contains("seasons:"), "no seasons defaults expected in " + url);
            }
        }
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

        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.with(List.of(portal))).mock(FeatureFlags.class).mock(PermissionService.class, "luckperms").build();
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
