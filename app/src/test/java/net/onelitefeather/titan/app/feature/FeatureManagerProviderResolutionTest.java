/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.app.feature;

import net.onelitefeather.titan.common.feature.SingletonFeatureManagerProvider;
import net.onelitefeather.titan.common.feature.TitanFeatures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.togglz.core.Feature;
import org.togglz.core.context.FeatureContext;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.spi.FeatureManagerProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards which {@link FeatureManagerProvider} wins the ambient {@link FeatureContext}.
 *
 * <p>Butterfly ships a provider of its own that reads the same {@code flags.properties} but builds
 * its manager from {@code ButterflyFeatures}. Both providers used to declare priority {@code 30},
 * so the winner was decided by whichever entry the merged {@code META-INF/services} file listed
 * first - shadow's classpath walk order. If Butterfly won, {@code FeatureGate.statuses()},
 * {@code FeatureGate.pollStageTransitions()} and {@code /season status} would enumerate the wrong
 * enum while {@code getFeatureState} kept resolving Titan's flags by name, which is a partial and
 * confusing failure rather than a loud one.
 *
 * <p>This test runs in {@code :app}, the module where both providers are on one classpath.
 */
class FeatureManagerProviderResolutionTest {

    private static final String BUTTERFLY_PROVIDER = "net.onelitefeather.butterfly.minestom.feature.SingletonFeatureManagerProvider";

    @BeforeEach
    void clearAmbientManager() {
        FeatureContext.clearCache();
    }

    @Test
    @DisplayName("the ambient feature manager enumerates Titan's features, not Butterfly's")
    void ambientManagerEnumeratesTitanFeatures() {
        FeatureManager manager = FeatureContext.getFeatureManager();

        Set<Feature> features = manager.getFeatures();
        assertEquals(Set.of(TitanFeatures.values()), features, "The ambient Togglz manager enumerates " + features + " instead of Titan's features. A rival FeatureManagerProvider won the " + "ServiceLoader lookup - check SingletonFeatureManagerProvider.PRIORITY against the " + "providers listed by providerPriorities().");
    }

    @Test
    @DisplayName("Butterfly's rival provider is on the classpath, so the test above is not vacuous")
    void butterflyProviderIsPresent() {
        List<String> names = providers().stream().map(provider -> provider.getClass().getName()).toList();

        assertTrue(names.contains(BUTTERFLY_PROVIDER), "Butterfly's provider is no longer on the :app classpath (" + names + "). The tie this " + "test guards is gone - either Butterfly stopped shipping one, or the dependency was " + "dropped. Re-check before deleting this test.");
    }

    @Test
    @DisplayName("Titan's provider outranks every other provider on the classpath")
    void titanProviderHasTheLowestPriority() {
        List<FeatureManagerProvider> providers = providers();
        FeatureManagerProvider titan = providers.stream().filter(SingletonFeatureManagerProvider.class::isInstance).findFirst().orElse(null);
        assertNotNull(titan, "Titan's provider is not registered in META-INF/services at all.");

        assertFalse(providers.stream().filter(provider -> provider != titan).anyMatch(provider -> provider.priority() <= titan.priority()), () -> "Titan's provider must win by priority, never by service-file order. Titan declares " + titan.priority() + ", the others declare " + providers.stream().filter(provider -> provider != titan).map(provider -> provider.getClass().getName() + '=' + provider.priority()).toList());
    }

    private static List<FeatureManagerProvider> providers() {
        List<FeatureManagerProvider> providers = new ArrayList<>();
        ServiceLoader.load(FeatureManagerProvider.class, FeatureManagerProviderResolutionTest.class.getClassLoader()).forEach(providers::add);
        return providers;
    }
}
