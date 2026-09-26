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
package net.onelitefeather.titan.app.bootstrap.reload;

import net.onelitefeather.titan.common.feature.ConfigFeatureFlags;
import net.onelitefeather.titan.common.feature.FeatureFlags;

/**
 * The child process entry point {@code ConfigChangeFileWatchIntegrationTest} launches for its
 * feature-flag scenarios: builds the production {@link FeatureFlags} exactly like
 * {@code PlatformBeans#featureFlags()} does - {@link ConfigFeatureFlags#fromClasspathDefaults()} -
 * and prints whether {@code NAVIGATOR_SLENDER} is active, then exits. Proves an environment
 * variable ({@code FEATURES_NAVIGATOR_SLENDER}) turns the flag on, and that a leftover
 * {@code flags.properties} in the working directory (the pre-{@code config-reload-feature-flags}
 * file, no longer read at all) has no effect - see
 * {@code openspec/changes/config-reload-feature-flags/tasks.md}, task 5.1.
 */
public final class FeatureFlagChildMain {

    private static final String FLAG_NAME = "NAVIGATOR_SLENDER";

    private FeatureFlagChildMain() {
    }

    public static void main(String[] args) {
        FeatureFlags featureFlags = ConfigFeatureFlags.fromClasspathDefaults();
        System.out.println(FLAG_NAME + "=" + featureFlags.isActive(FLAG_NAME));
    }
}
