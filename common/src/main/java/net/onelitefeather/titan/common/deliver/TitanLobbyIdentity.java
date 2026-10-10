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
 * lives on the shared application classloader: the CloudNet bridge extension sets the identity it
 * reads from CloudNet, the application reads it; until then the lobby knows nothing about itself.
 */
public final class TitanLobbyIdentity {

    private static volatile LobbyIdentity identity;

    private TitanLobbyIdentity() {
    }

    /** Installs the identity; {@code null} clears it. */
    public static void set(LobbyIdentity lobbyIdentity) {
        identity = lobbyIdentity;
    }

    public static Optional<LobbyIdentity> self() {
        return Optional.ofNullable(identity);
    }

    /** The holder as the application-facing {@link LobbyIdentities}. */
    public static LobbyIdentities identities() {
        return TitanLobbyIdentity::self;
    }
}
