/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.feature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitanFeaturesTest {

    /** NFR-009: the flag list must not grow into one constant per sub-feature. */
    private static final int MAX_FEATURES = 12;

    @Test
    @DisplayName("the feature list stays below the agreed ceiling of twelve")
    void featureCountStaysBelowTheCeiling() {
        assertTrue(TitanFeatures.values().length <= MAX_FEATURES, "TitanFeatures holds " + TitanFeatures.values().length + " constants, at most " + MAX_FEATURES + " are allowed (NFR-009). Release stages and time windows are " + "configuration on an existing flag, not new flags.");
    }

    @Test
    @DisplayName("feature names are unique")
    void featureNamesAreUnique() {
        Set<String> names = new HashSet<>();
        Arrays.stream(TitanFeatures.values()).map(Enum::name).forEach(names::add);
        assertEquals(TitanFeatures.values().length, names.size());
    }
}
