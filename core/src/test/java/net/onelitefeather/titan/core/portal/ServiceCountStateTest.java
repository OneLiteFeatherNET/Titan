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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceCountStateTest {

    @DisplayName("A service that has not announced a limit is not judged full")
    @Test
    void noLimitAnnouncedIsNotFull() {
        ServiceCount service = new ServiceCount("Lobby-1", 0, 0);

        assertFalse(service.limitAnnounced(), "max 0 means no limit has been announced");
        assertFalse(service.full(), "without a limit a service cannot be full");
    }

    @DisplayName("A service below its announced limit is open")
    @Test
    void belowLimitIsOpen() {
        ServiceCount service = new ServiceCount("Lobby-1", 19, 20);

        assertTrue(service.limitAnnounced(), "a positive max is an announced limit");
        assertFalse(service.full(), "19 of 20 still has a free slot");
    }

    @DisplayName("A service at its announced limit is full")
    @Test
    void atLimitIsFull() {
        ServiceCount service = new ServiceCount("Lobby-1", 20, 20);

        assertTrue(service.full(), "20 of 20 is full");
    }
}
