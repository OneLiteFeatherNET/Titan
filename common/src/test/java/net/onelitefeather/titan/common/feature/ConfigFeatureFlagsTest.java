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

import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link ConfigFeatureFlags}, built directly from its constructor - never through
 * the static factory that wires it to {@code io.avaje.config.Config} (see design.md, decision 4).
 * That keeps this class Fast, Independent and Repeatable: no test here ever calls
 * {@code io.avaje.config.Config}, so none of them can be affected by a real
 * {@code application.yaml} on the classpath or by another test's system properties.
 */
class ConfigFeatureFlagsTest {

    @DisplayName("A known, active flag is active")
    @Test
    void aKnownActiveFlagIsActive() {
        ConfigFeatureFlags flags = new ConfigFeatureFlags(Set.of("NAVIGATOR_SLENDER"), name -> true);

        Assertions.assertTrue(flags.exists("NAVIGATOR_SLENDER"), "a known flag must exist");
        Assertions.assertTrue(flags.isActive("NAVIGATOR_SLENDER"), "the predicate said this flag is on");
    }

    @DisplayName("A known, inactive flag exists but is not active")
    @Test
    void aKnownInactiveFlagIsNotActive() {
        ConfigFeatureFlags flags = new ConfigFeatureFlags(Set.of("NAVIGATOR_SLENDER"), name -> false);

        Assertions.assertTrue(flags.exists("NAVIGATOR_SLENDER"), "a known flag must exist");
        Assertions.assertFalse(flags.isActive("NAVIGATOR_SLENDER"), "the predicate said this flag is off");
    }

    @DisplayName("An unknown name does not exist, even if the predicate would say yes")
    @Test
    void anUnknownNameDoesNotExist() {
        ConfigFeatureFlags flags = new ConfigFeatureFlags(Set.of("NAVIGATOR_SLENDER"), name -> true);

        Assertions.assertFalse(flags.exists("GIBT_ES_NICHT"), "an unknown name must not exist");
    }

    @DisplayName("An unknown name is never active, even if the predicate would say yes")
    @Test
    void anUnknownNameIsNeverActive() {
        ConfigFeatureFlags flags = new ConfigFeatureFlags(Set.of("NAVIGATOR_SLENDER"), name -> true);

        Assertions.assertFalse(flags.isActive("GIBT_ES_NICHT"), "an unknown name must never be active");
    }

    @DisplayName("A missing value is treated as off")
    @Test
    void aMissingValueIsOff() {
        ConfigFeatureFlags flags = new ConfigFeatureFlags(Set.of("NAVIGATOR_SLENDER"), name -> false);

        Assertions.assertFalse(flags.isActive("NAVIGATOR_SLENDER"), "a missing/unset value must be off");
    }
}
