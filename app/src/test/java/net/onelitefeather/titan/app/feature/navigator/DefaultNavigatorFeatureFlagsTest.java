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
package net.onelitefeather.titan.app.feature.navigator;

import io.avaje.config.Configuration;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the shipped classpath {@code application.yaml} against a {@link Destination} that gates
 * itself behind a feature flag the {@code features} section does not list.
 *
 * <p>Unlike before {@code openspec/changes/navigator-entries-in-code},
 * {@link Destination#feature()}
 * is fixed in code rather than read from configuration, so this can no longer fail at start-up via
 * a registry validation - it is a plain unit test instead, guarding the shipped defaults directly.
 * This test would have failed the moment {@code features.NAVIGATOR_SLENDER} was missing while
 * {@link Destination#SLENDER} still names it.
 *
 * <p>Loads {@code application.yaml} as its own, independent {@link Configuration} instance -
 * exactly like {@link net.onelitefeather.titan.common.feature.ConfigFeatureFlags} does in
 * production - rather than through the static {@code io.avaje.config.Config} facade, so this test
 * stays Independent of any other test's system properties or profiles and Repeatable regardless of
 * load order.
 */
class DefaultNavigatorFeatureFlagsTest {

    private static final String FEATURES_SECTION = "features";

    @DisplayName("Every destination's feature flag is listed in the shipped features section")
    @Test
    void everyDestinationFeatureIsKnown() {
        Configuration classpathOnly = Configuration.builder().resourceLoader(getClass().getClassLoader()::getResourceAsStream).load("application.yaml").build();
        Set<String> knownFlags = classpathOnly.forPath(FEATURES_SECTION).keys();

        for (Destination destination : Destination.values()) {
            String feature = destination.feature();
            if (feature != null) {
                Assertions.assertTrue(knownFlags.contains(feature), () -> "destination '" + destination + "' uses feature '" + feature + "', which is missing from the 'features' section of application.yaml");
            }
        }
    }
}
