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
package net.onelitefeather.titan.app.bootstrap;

import io.avaje.config.Configuration;
import io.avaje.inject.BeanScope;
import io.avaje.inject.BeanScopeBuilder;
import java.util.Map;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.config.ConfigSections;
import net.onelitefeather.titan.common.feature.FeatureFlags;
import net.onelitefeather.titan.common.map.MapProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Proves the exact {@link BeanScope} wiring {@link net.onelitefeather.titan.app.Titan}'s
 * constructor performs - supplying a pre-built {@link Configuration} as a bean and as the backing
 * source of a {@link ConfigurationPropertyPlugin} - resolves {@link ConfigSections} from that same
 * instance, end to end through the real {@link io.avaje.inject.BeanScopeBuilder}, not a mock.
 *
 * <p>This is the regression test for the smoke-test defect {@link ConfigurationPropertyPlugin}'s
 * Javadoc describes: before that class existed, {@code BeanScope.builder().build()} fell back to
 * Avaje Inject's default property plugin, which reads the <em>static</em> {@code
 * io.avaje.config.Config} facade and loads {@code application.yaml} a second time - a broken file
 * then surfaced as an uncaught {@code ExceptionInInitializerError} that hung the process instead of
 * exiting. Supplying {@code configPlugin(new ConfigurationPropertyPlugin(configuration))} here, the
 * same way {@code Titan} does, means the scope never falls back to that default at all.
 *
 * <p>{@link Configuration} is built directly from a {@link Map} (design.md decision 1's spike
 * result), never from the real filesystem, profiles or environment, so this stays hermetic
 * (F.I.R.S.T. - Independent, Repeatable) - unlike {@link ConfigurationPrecedenceTest}, which
 * exercises the real resource-loading pipeline in a child JVM. {@link MapProvider} and {@link
 * FeatureFlags} are mocked via Avaje Inject's {@code forTesting()} escape hatch, the same pattern
 * {@code ModuleWiringTest} uses, since a real {@link MapProvider} would read the filesystem {@code
 * worlds/} and a real {@link FeatureFlags} would read Togglz's process-wide static - neither of
 * which this test is about.
 */
@ExtendWith(MicrotusExtension.class)
class TitanBeanScopeConfigurationWiringTest {

    /**
     * A minimal, self-contained config record used only to probe {@link ConfigSections}'
     * binding mechanism, now that no feature module keeps its own config record around for a
     * bootstrap test to borrow (see {@code openspec/changes/avaje-config-facade/design.md},
     * decision 3).
     */
    private record ProbeSection(int simulationDistance) {

        private static final ProbeSection DEFAULTS = new ProbeSection(0);
    }

    @DisplayName("ConfigSections resolves a value from the Configuration instance supplied to the scope, not a rebuilt one")
    @Test
    void configSectionsResolvesFromTheSuppliedConfigurationInstance(Env env) {
        Configuration configuration = Configuration.builder().putAll(Map.of("probe.simulationDistance", "7")).build();

        BeanScopeBuilder.ForTesting builder = BeanScope.builder().forTesting().mock(MapProvider.class).mock(FeatureFlags.class);
        builder.bean(Configuration.class, configuration);
        // configPlugin(...) returns void, not the builder, so it cannot be chained above.
        builder.configPlugin(new ConfigurationPropertyPlugin(configuration));
        BeanScope scope = builder.build();
        try {
            ConfigSections configSections = scope.get(ConfigSections.class);
            ProbeSection probe = configSections.section("probe", ProbeSection.class, ProbeSection.DEFAULTS);

            Assertions.assertEquals(7, probe.simulationDistance(), "the scope must resolve ConfigSections from exactly the Configuration instance it was given");
        } finally {
            scope.close();
        }
    }
}
