/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        if (event.getPacket() instanceof ClientInputPacket(byte flags)) {
            var ridingEntity = event.getPlayer().getVehicle();
            if (flags == 2 && ridingEntity != null) {
                var entityDismountEvent = new EntityDismountEvent(event.getPlayer(), ridingEntity);
                EventDispatcher.call(entityDismountEvent);
            }
        }
    }
}
