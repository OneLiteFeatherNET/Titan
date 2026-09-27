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

import java.util.HashMap;
import java.util.Map;
import net.onelitefeather.titan.common.feature.FeatureFlags;

/**
 * A test-only {@link FeatureFlags}: every name {@link #declare(String, boolean)} was called for
 * "exists"; its current state is whatever {@link #set(String, boolean)} last set it to. A name
 * never
 * declared does not exist and is never active - mirroring an unknown name in
 * {@code net.onelitefeather.titan.common.feature.ConfigFeatureFlags}.
 *
 * <p>Exists so {@code NavigatorModule} and {@code NavigatorInventory} tests never need a real
 * classpath {@code application.yaml} file or the static {@code io.avaje.config.Config} facade -
 * see {@code openspec/changes/lobby-feature-modules/design.md}, decision 13, and this codebase's
 * F.I.R.S.T. rule against a real file or a static singleton in a test.
 */
final class FakeFeatureFlags implements FeatureFlags {

    private final Map<String, Boolean> states = new HashMap<>();

    /**
     * Declares {@code featureName} as known, initially in state {@code active}.
     *
     * @param featureName the feature's name
     * @param active      its initial state
     * @return this instance, for chaining
     */
    FakeFeatureFlags declare(String featureName, boolean active) {
        this.states.put(featureName, active);
        return this;
    }

    /**
     * Changes an already-{@link #declare(String, boolean) declared} feature's state.
     *
     * @param featureName the feature's name
     * @param active      its new state
     */
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
