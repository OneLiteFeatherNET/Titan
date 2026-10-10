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
package net.onelitefeather.titan.core.portal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCountsRunningTest {

    private static final PlayerCounts WITHOUT_LISTING = (type, name) -> PlayerCount.NOT_RUNNING;

    @DisplayName("A provider that does not list services returns an empty list")
    @Test
    void defaultListingIsEmpty() {
        assertTrue(WITHOUT_LISTING.running(SourceType.TASK, "Lobby").isEmpty(), "the default has nothing to list");
    }

    @DisplayName("A service count carries name, online and max")
    @Test
    void serviceCountCarriesItsValues() {
        ServiceCount count = new ServiceCount("Lobby-1", 3, 20);

        assertEquals("Lobby-1", count.name(), "name");
        assertEquals(3, count.online(), "online");
        assertEquals(20, count.max(), "max");
    }
}
