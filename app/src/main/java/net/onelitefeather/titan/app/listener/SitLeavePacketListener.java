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
