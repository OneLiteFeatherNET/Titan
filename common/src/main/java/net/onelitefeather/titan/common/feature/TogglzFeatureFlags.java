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
package net.onelitefeather.titan.common.feature;

import net.onelitefeather.titan.common.utils.TitanFeatures;
import org.jetbrains.annotations.Nullable;

/**
 * The production {@link FeatureFlags}: looks a name up against the {@link TitanFeatures} enum and,
 * if found, defers to {@link TitanFeatures#isActive()} - which already goes through the static
 * Togglz {@code FeatureContext} and {@code SingletonFeatureManagerProvider}
 * ({@code FileBasedStateRepository} on {@code flags.properties}), wrapped in the existing
 * {@code ThreadHelper#syncThreadForServiceLoader} classloader handling.
 *
 * <p>A name {@link TitanFeatures} does not declare is treated as "does not exist" by
 * {@link #exists(String)} and as "not active" by {@link #isActive(String)} - the composition root
 * never gets this far for a config-supplied name, because a feature module validates every
 * configured feature name against {@link #exists(String)} on enable and aborts startup otherwise
 * (see {@code openspec/changes/lobby-feature-modules/design.md}, decision 13).
 */
public final class TogglzFeatureFlags implements FeatureFlags {

    @Override
    public boolean exists(String featureName) {
        return findFeature(featureName) != null;
    }

    @Override
    public boolean isActive(String featureName) {
        TitanFeatures feature = findFeature(featureName);
        return feature != null && feature.isActive();
    }

    private static @Nullable TitanFeatures findFeature(String featureName) {
        try {
            return TitanFeatures.valueOf(featureName);
        } catch (IllegalArgumentException notAKnownFeature) {
            return null;
        }
    }
}
