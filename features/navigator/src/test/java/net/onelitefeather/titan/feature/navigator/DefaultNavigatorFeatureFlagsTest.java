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
package net.onelitefeather.titan.feature.navigator;

import io.avaje.config.Configuration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the shipped {@code titan/defaults/features.yaml} against a {@link Destination}
 * that gates itself behind a feature flag the {@code features} section does not list.
 *
 * <p>{@link Destination#feature()} is fixed in code, so this is a plain unit test rather than a
 * start-up registry validation. Loads {@code titan/defaults/features.yaml} as its own,
 * independent {@link Configuration} instance, rather than through the static facade, so this test
 * stays Independent and Repeatable regardless of load order.
 */
class DefaultNavigatorFeatureFlagsTest {

    private static final String FEATURES_SECTION = "features";
    private static final String DEFAULTS_RESOURCE = "titan/defaults/features.yaml";

    @DisplayName("Every destination's feature flag is listed in the shipped features section")
    @Test
    void everyDestinationFeatureIsKnown() {
        Configuration classpathOnly = Configuration.builder().resourceLoader(getClass().getClassLoader()::getResourceAsStream).load(DEFAULTS_RESOURCE).build();
        Set<String> knownFlags = classpathOnly.forPath(FEATURES_SECTION).keys();

        for (Destination destination : Destination.values()) {
            String feature = destination.feature();
            if (feature != null) {
                Assertions.assertTrue(knownFlags.contains(feature), () -> "destination '" + destination + "' uses feature '" + feature + "', which is missing from the 'features' section of " + DEFAULTS_RESOURCE);
            }
        }
    }

    @DisplayName("The shipped features section lists exactly the flags that a destination uses")
    @Test
    void shippedFeaturesAreExactlyTheDestinationFlags() {
        Configuration classpathOnly = Configuration.builder().resourceLoader(getClass().getClassLoader()::getResourceAsStream).load(DEFAULTS_RESOURCE).build();
        Set<String> knownFlags = classpathOnly.forPath(FEATURES_SECTION).keys();
        Set<String> destinationFlags = Arrays.stream(Destination.values()).map(Destination::feature).filter(feature -> feature != null).collect(Collectors.toSet());

        Assertions.assertEquals(destinationFlags, knownFlags, "the 'features' section of " + DEFAULTS_RESOURCE + " must list exactly the flags a destination is gated by");
    }
}
