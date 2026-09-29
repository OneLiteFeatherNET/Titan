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
package net.onelitefeather.titan.feature.season;

import net.onelitefeather.titan.common.map.MapEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lives in the column's package name because {@code SeasonSettings} is package-private; a column
 * sees only core, so this is the one place that can compare it with {@code common}.
 */
class SeasonMapFileNameTest {

    @DisplayName("The column's map file name equals MapEntry.MAP_FILE_NAME")
    @Test
    void mapFileNameMatchesMapEntry() {
        Assertions.assertEquals(MapEntry.MAP_FILE_NAME, SeasonSettings.MAP_FILE_NAME, "the season column would validate a file the map loader never reads");
    }
}
