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
package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.network.packet.client.play.ClientInputPacket;
import net.onelitefeather.titan.common.event.EntityDismountEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class SitLeavePacketListener implements Consumer<PlayerPacketEvent> {

    @Override
    public void accept(@NotNull PlayerPacketEvent event) {
        // Stand up (dismount) when the sitting player presses sneak. Use the shift() accessor
        // rather than testing the raw flags: the sneak bit is 0x20, not 0x02 (that is backward),
        // and shift() also matches when other movement keys are held at the same time.
        if (event.getPacket() instanceof ClientInputPacket input && input.shift()) {
            var ridingEntity = event.getPlayer().getVehicle();
            if (ridingEntity != null) {
                var entityDismountEvent = new EntityDismountEvent(event.getPlayer(), ridingEntity);
                EventDispatcher.call(entityDismountEvent);
            }
        }
    }
}
