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

import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain unit tests for {@link NavigatorEntryKeys}: no {@code Config}, no Minestom server needed -
 * just the pure function that turns relative {@code navigator.entries} keys into entry names.
 */
class NavigatorEntryKeysTest {

    @DisplayName("Keys for two entries collapse to their two distinct names")
    @Test
    void keysForTwoEntriesCollapseToTheirNames() {
        Set<String> names = NavigatorEntryKeys.names(Set.of("survival.slot", "survival.icon", "parkour.slot"));

        Assertions.assertEquals(Set.of("survival", "parkour"), names);
    }

    @DisplayName("Every field of an entry maps to the same single name")
    @Test
    void everyFieldOfAnEntryMapsToTheSameName() {
        Set<String> names = NavigatorEntryKeys.names(Set.of("slender.slot", "slender.icon", "slender.displayName", "slender.destination", "slender.feature"));

        Assertions.assertEquals(Set.of("slender"), names);
    }

    @DisplayName("An empty key set yields no names")
    @Test
    void emptyKeySetYieldsNoNames() {
        Set<String> names = NavigatorEntryKeys.names(Set.of());

        Assertions.assertEquals(Set.of(), names);
    }

    @DisplayName("A key with no dot is its own name")
    @Test
    void keyWithNoDotIsItsOwnName() {
        Set<String> names = NavigatorEntryKeys.names(Set.of("survival"));

        Assertions.assertEquals(Set.of("survival"), names);
    }

    @DisplayName("A null key set is rejected")
    @Test
    void nullKeySetIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> NavigatorEntryKeys.names(null));
    }
}
