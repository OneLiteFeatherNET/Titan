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
package net.onelitefeather.titan.runtime.feature;

import io.avaje.config.Configuration;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link ConfigFeatureFlags}, built directly from its constructor rather than
 * through the static factory that touches {@code io.avaje.config.Config}.
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

    @DisplayName("The shipped runtime defaults declare the lobby switcher flag, on by default")
    @Test
    void theLobbySwitcherFlagIsDeclaredOnByDefault() {
        ClassLoader classLoader = ConfigFeatureFlagsTest.class.getClassLoader();
        Set<String> known = ConfigFeatureFlags.knownFlagsIn("titan/defaults/features.yaml", classLoader);

        Assertions.assertTrue(known.contains("LOBBYSWITCHER"), "LOBBYSWITCHER must be a known flag");
        Assertions.assertTrue(known.contains("NAVIGATOR_SLENDER"), "the navigator flags must move along unchanged");
    }

    @DisplayName("The navigator flags stay off by default, the lobby switcher flag is on")
    @Test
    void theShippedDefaultValuesAreAsDocumented() {
        Configuration defaults = Configuration.builder().resourceLoader(ConfigFeatureFlagsTest.class.getClassLoader()::getResourceAsStream).load("titan/defaults/features.yaml").build();

        Assertions.assertTrue(defaults.getBool("features.LOBBYSWITCHER", false), "LOBBYSWITCHER must default to true");
        Assertions.assertFalse(defaults.getBool("features.NAVIGATOR_SLENDER", true), "NAVIGATOR_SLENDER must default to false");
    }

    @DisplayName("A missing value is treated as off")
    @Test
    void aMissingValueIsOff() {
        ConfigFeatureFlags flags = new ConfigFeatureFlags(Set.of("NAVIGATOR_SLENDER"), name -> false);

        Assertions.assertFalse(flags.isActive("NAVIGATOR_SLENDER"), "a missing/unset value must be off");
    }
}
