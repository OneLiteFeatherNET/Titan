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

import java.util.HashMap;
import java.util.Map;
import net.onelitefeather.titan.core.feature.FeatureFlags;

/**
 * A test-only {@link FeatureFlags}: a name exists once {@link #declare(String, boolean)} is
 * called for it, and its state is whatever {@link #set(String, boolean)} last set.
 *
 * <p>Exists so {@code NavigatorModule} and {@code Destination} tests never need a real classpath
 * {@code application.yaml} or the static {@code Config} facade.
 */
final class FakeFeatureFlags implements FeatureFlags {

    private final Map<String, Boolean> states = new HashMap<>();

    FakeFeatureFlags declare(String featureName, boolean active) {
        this.states.put(featureName, active);
        return this;
    }

    void set(String featureName, boolean active) {
        if (!this.states.containsKey(featureName)) {
            throw new IllegalArgumentException("'" + featureName + "' was never declared; call declare(...) first");
        }
        this.states.put(featureName, active);
    }

    @Override
    public boolean exists(String featureName) {
        return this.states.containsKey(featureName);
    }

    @Override
    public boolean isActive(String featureName) {
        return this.states.getOrDefault(featureName, false);
    }
}
