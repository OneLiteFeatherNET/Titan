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

/** What a click on a lobby does, decided from a fresh reading taken at click time. */
enum SwitcherClickDecision {
    SEND("sent"), CURRENT("current"), FULL("full"), NOT_READY("not_ready"),
    /** The target no longer runs; it cannot appear in the list, only here. */
    GONE("gone"),
    /** The check itself failed, so nothing is sent. */
    ERROR("error");

    private final String result;

    SwitcherClickDecision(String result) {
        this.result = result;
    }

    /** The value of the {@code lobbyswitcher.result} attribute and of the selection counter. */
    String result() {
        return this.result;
    }

    static SwitcherClickDecision decide(String target, LobbyIdentity own, List<ServiceCount> fresh) {
        if (target.equals(own.serviceName())) {
            return CURRENT;
        }
        return fresh.stream().filter(service -> service.name().equals(target)).findFirst().map(service -> fromState(SwitcherState.of(service, own))).orElse(GONE);
    }

    /** Takes the fresh reading itself, so that a failing reading becomes {@link #ERROR}. */
    static SwitcherClickDecision decide(String target, LobbyIdentity own, Supplier<List<ServiceCount>> check) {
        List<ServiceCount> fresh;
        try {
            fresh = check.get();
        } catch (RuntimeException e) {
            return ERROR;
        }
        return decide(target, own, fresh);
    }

    private static SwitcherClickDecision fromState(SwitcherState state) {
        return switch (state) {
            case CURRENT -> CURRENT;
            case FULL -> FULL;
            case NOT_READY -> NOT_READY;
            case JOINABLE -> SEND;
        };
    }
}
