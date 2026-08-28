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
package net.onelitefeather.titan.common.time.season;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The season enum itself: the identifier configuration and world directories use, and the order.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
class SeasonTest {

    @Test
    @DisplayName("The identifier is the lower-case name and does not depend on the default locale")
    void theIdentifierIsLocaleIndependent() {
        assertEquals("spring", Season.SPRING.id());
        assertEquals("summer", Season.SUMMER.id());
        assertEquals("autumn", Season.AUTUMN.id());
        assertEquals("winter", Season.WINTER.id());
    }

    @Test
    @DisplayName("The seasons follow each other in calendar order and wrap")
    void theSeasonsWrap() {
        assertEquals(Season.SUMMER, Season.SPRING.next());
        assertEquals(Season.AUTUMN, Season.SUMMER.next());
        assertEquals(Season.WINTER, Season.AUTUMN.next());
        assertEquals(Season.SPRING, Season.WINTER.next());
    }
}
