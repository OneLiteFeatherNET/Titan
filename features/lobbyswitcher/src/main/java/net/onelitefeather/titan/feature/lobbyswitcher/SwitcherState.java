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

import java.util.List;
import java.util.function.Supplier;
import net.onelitefeather.titan.core.lobby.LobbyIdentity;
import net.onelitefeather.titan.core.portal.ServiceCount;

/**
 * What a lobby offers the viewer: the state of a listed row, and the verdict of the fresh check
 * taken when a row is clicked. A service that no longer runs is not listed, so {@link #GONE} and
 * {@link #ERROR} only exist as click verdicts.
 */
enum SwitcherState {
    /** This lobby; wins over every other state. */
    CURRENT("current"),
    /** Has a player limit and has reached it. */
    FULL("full"),
    /** Runs but has not announced a player limit yet. */
    NOT_READY("not_ready"),
    /** Can be joined; as a click verdict, the player is sent. */
    JOINABLE("sent"),
    /** Click verdict: the target no longer runs. */
    GONE("gone"),
    /** Click verdict: the check itself failed, so nobody is sent. */
    ERROR("error");

    private final String result;

    SwitcherState(String result) {
        this.result = result;
    }

    /** The value of the {@code lobbyswitcher.result} attribute and of the selection counter. */
    String result() {
        return this.result;
    }

    static SwitcherState of(ServiceCount service, LobbyIdentity own) {
        if (service.name().equals(own.serviceName())) {
            return CURRENT;
        }
        if (service.max() == 0) {
            return NOT_READY;
        }
        return service.online() >= service.max() ? FULL : JOINABLE;
    }

    /**
     * What clicking {@code target} does, from a fresh reading; a failing reading is {@link #ERROR}.
     */
    static SwitcherState ofClick(String target, LobbyIdentity own, Supplier<List<ServiceCount>> check) {
        List<ServiceCount> fresh;
        try {
            fresh = check.get();
        } catch (RuntimeException e) {
            return ERROR;
        }
        if (target.equals(own.serviceName())) {
            return CURRENT;
        }
        return fresh.stream().filter(service -> service.name().equals(target)).findFirst().map(service -> of(service, own)).orElse(GONE);
    }
}
