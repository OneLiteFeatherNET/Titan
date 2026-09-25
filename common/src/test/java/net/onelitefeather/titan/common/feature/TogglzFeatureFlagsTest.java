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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link TogglzFeatureFlags#exists(String)} and the short-circuit branch of
 * {@link TogglzFeatureFlags#isActive(String)} that never needs to ask.
 *
 * <p>Deliberately does not assert on {@link TogglzFeatureFlags#isActive(String)} for a name that
 * <em>is</em> a real {@code TitanFeatures} constant: that branch defers to
 * {@code TitanFeatures#isActive()}, which reads the static Togglz {@code FeatureContext} backed by
 * a real {@code flags.properties} file on disk. Exercising that here would violate F.I.R.S.T.
 * (Independent, Repeatable) exactly the way {@code NavigatorModule}'s own tests avoid it - by
 * injecting a fake {@link FeatureFlags} instead. This class only covers the pure, static
 * {@link TitanFeatures#valueOf(String)} lookup that both methods start with.
 */
class TogglzFeatureFlagsTest {

    private final TogglzFeatureFlags flags = new TogglzFeatureFlags();

    @DisplayName("A known TitanFeatures constant exists")
    @Test
    void aKnownFeatureExists() {
        Assertions.assertTrue(this.flags.exists("NAVIGATOR_SLENDER"));
    }

    @DisplayName("An unknown name does not exist")
    @Test
    void anUnknownNameDoesNotExist() {
        Assertions.assertFalse(this.flags.exists("GIBT_ES_NICHT"));
    }

    @DisplayName("An unknown name is never active")
    @Test
    void anUnknownNameIsNeverActive() {
        Assertions.assertFalse(this.flags.isActive("GIBT_ES_NICHT"));
    }
}
