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
package net.onelitefeather.titan.feature.lobbyswitcher;

import java.util.Set;
import net.onelitefeather.titan.core.feature.FeatureFlags;

/** Every flag it was given is on; every other name does not exist. */
final class FakeFeatureFlags implements FeatureFlags {

    private final Set<String> active;

    FakeFeatureFlags(String... active) {
        this.active = Set.of(active);
    }

    @Override
    public boolean exists(String featureName) {
        return this.active.contains(featureName);
    }

    @Override
    public boolean isActive(String featureName) {
        return this.active.contains(featureName);
    }
}
