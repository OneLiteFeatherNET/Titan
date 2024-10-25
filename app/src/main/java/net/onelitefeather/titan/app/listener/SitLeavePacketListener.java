package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket;
import net.onelitefeather.titan.common.event.EntityDismountEvent;

import java.util.function.Consumer;

public final class SitLeavePacketListener implements Consumer<PlayerPacketEvent> {
    @Override
    public void accept(PlayerPacketEvent event) {
        if (event.getPacket() instanceof ClientSteerVehiclePacket packet) {
            var ridingEntity = event.getPlayer().getVehicle();
            if (packet.flags() == 2 && ridingEntity != null) {
                var entityDismountEvent = new EntityDismountEvent(ridingEntity, event.getPlayer());
                EventDispatcher.call(entityDismountEvent);
            }
        }
    }
}
