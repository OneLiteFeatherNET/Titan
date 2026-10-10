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
package net.onelitefeather.titan.feature.sit;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.network.packet.client.play.ClientInputPacket;

/** The events a sit test feeds into {@link SitModule}, built the way the server delivers them. */
final class SitTestEvents {

    private SitTestEvents() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    static PlayerBlockInteractEvent clickBlock(Player player, Instance instance, Block block, BlockVec position) {
        return new PlayerBlockInteractEvent(player, PlayerHand.MAIN, instance, block, position, new Vec(0.5, 1, 0.5), BlockFace.TOP);
    }

    static PlayerPacketEvent sneak(Player player) {
        return new PlayerPacketEvent(player, new ClientInputPacket(false, false, false, false, false, true, false));
    }

    static PlayerDisconnectEvent disconnect(Player player) {
        return new PlayerDisconnectEvent(player);
    }
}
