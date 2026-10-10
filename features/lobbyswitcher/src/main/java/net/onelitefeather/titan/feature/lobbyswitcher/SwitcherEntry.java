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

import java.util.Comparator;
import java.util.List;
import net.onelitefeather.titan.core.lobby.LobbyIdentity;
import net.onelitefeather.titan.core.portal.ServiceCount;

/**
 * One row of the lobby list.
 *
 * @param name   the service name
 * @param online players currently on the service
 * @param max    the player limit; {@code 0} while the service has not announced one
 * @param state  what the viewer can do with this row
 */
record SwitcherEntry(String name, int online, int max, SwitcherState state) {

    static SwitcherEntry of(ServiceCount service, LobbyIdentity own) {
        return new SwitcherEntry(service.name(), service.online(), service.max(), SwitcherState.of(service, own));
    }

    /** The entries by service name, ascending, whatever their state. */
    static List<SwitcherEntry> sorted(List<ServiceCount> services, LobbyIdentity own) {
        return services.stream().map(service -> of(service, own)).sorted(Comparator.comparing(SwitcherEntry::name)).toList();
    }
}
