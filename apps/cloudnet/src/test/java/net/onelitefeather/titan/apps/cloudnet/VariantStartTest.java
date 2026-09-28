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
import java.util.ArrayList;
import java.util.List;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.runtime.variant.LoadedColumns;
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
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class).mock(FeatureFlags.class).build();

        Assertions.assertDoesNotThrow(scope::close, "closing a fully built scope must not throw");
    }

    @DisplayName("Every column named in variant.properties is loaded")
    @Test
    void everyExpectedColumnIsLoaded() {
        Assertions.assertDoesNotThrow(() -> VariantStartupCheck.verify(getClass().getClassLoader()));
    }

    @DisplayName("An additionally expected but missing column aborts startup, naming it")
    @Test
    void anAdditionallyExpectedButMissingColumnAbortsNamingIt() {
        ClassLoader loader = getClass().getClassLoader();
        VariantDescriptor real = VariantDescriptor.fromClasspath(loader).orElseThrow(() -> new AssertionError("this variant must ship META-INF/titan/variant.properties"));
        List<String> columnsWithAGhost = new ArrayList<>(real.columns());
        columnsWithAGhost.add("ghostColumn");
        VariantDescriptor withAMissingColumn = VariantDescriptor.of(real.name(), columnsWithAGhost);

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> VariantStartupCheck.verify(withAMissingColumn, LoadedColumns.discover(loader)));

        Assertions.assertTrue(thrown.getMessage().contains("ghostColumn"), "the message must name the missing column, was: " + thrown.getMessage());
    }
}
