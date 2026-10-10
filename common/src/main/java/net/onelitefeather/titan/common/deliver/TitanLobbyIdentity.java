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
package net.onelitefeather.titan.common.deliver;

import java.util.Optional;
import net.onelitefeather.titan.core.lobby.LobbyIdentities;
import net.onelitefeather.titan.core.lobby.LobbyIdentity;

/**
 * Cross-classloader holder for the identity of this lobby. Like {@link TitanPlayerCountLookup} it
 * lives on the shared application classloader and exchanges only JDK types: the CloudNet bridge
 * extension sets the task and service name it reads from CloudNet, the application reads them as
 * a {@link LobbyIdentity}; until then the lobby knows nothing about itself.
 */
public final class TitanLobbyIdentity {

    private static volatile String[] identity;

    private TitanLobbyIdentity() {
    }

    /** Installs the identity; a missing or blank name leaves the lobby without one. */
    public static void set(String task, String serviceName) {
        boolean usable = task != null && !task.isBlank() && serviceName != null && !serviceName.isBlank();
        identity = usable ? new String[]{task, serviceName} : null;
    }

    public static void clear() {
        identity = null;
    }

    public static Optional<LobbyIdentity> self() {
        String[] current = identity;
        return current == null ? Optional.empty() : Optional.of(new LobbyIdentity(current[0], current[1]));
    }

    /** The holder as the application-facing {@link LobbyIdentities}. */
    public static LobbyIdentities identities() {
        return TitanLobbyIdentity::self;
    }
}
