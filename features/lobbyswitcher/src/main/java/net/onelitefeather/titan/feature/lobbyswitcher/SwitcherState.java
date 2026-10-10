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
package net.onelitefeather.titan.feature.lobbyswitcher;

import net.onelitefeather.titan.core.lobby.LobbyIdentity;
import net.onelitefeather.titan.core.portal.ServiceCount;

/**
 * What a listed lobby offers the viewer. A service that no longer runs is not listed, so it has no
 * state.
 */
enum SwitcherState {
    /** This lobby; wins over every other state. */
    CURRENT,
    /** Has a player limit and has reached it. */
    FULL,
    /** Runs but has not announced a player limit yet. */
    NOT_READY, JOINABLE;

    static SwitcherState of(ServiceCount service, LobbyIdentity own) {
        if (service.name().equals(own.serviceName())) {
            return CURRENT;
        }
        if (service.max() == 0) {
            return NOT_READY;
        }
        return service.online() >= service.max() ? FULL : JOINABLE;
    }
}
