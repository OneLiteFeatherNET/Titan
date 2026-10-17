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
package net.onelitefeather.titan.feature.spawn;

import java.util.function.Consumer;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;

/**
 * Removes every server-pushed resource pack from a joining player.
 *
 * <p>Game servers (e.g. Cygnus) push a required pack. When a player leaves one - kicked, or by
 * switching servers themselves - the pack stays applied on the client, and the game server cannot
 * reliably remove it (a player-initiated proxy switch closes its connection without a chance to
 * send anything). The lobby is where those players end up, so it cleans up. Titan pushes no pack of
 * its own.
 *
 * <p>Runs in the configuration phase: the client reloads its resources there anyway, and
 * {@code ResourcePackPopPacket} is a valid configuration-state packet.
 */
final class ResourcePackClearListener implements Consumer<AsyncPlayerConfigurationEvent> {

    @Override
    public void accept(AsyncPlayerConfigurationEvent event) {
        event.getPlayer().clearResourcePacks();
    }
}
