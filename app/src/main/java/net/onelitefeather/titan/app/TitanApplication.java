package net.onelitefeather.titan.app;

import net.minestom.server.MinecraftServer;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.onelitefeather.agones.AgonesAPI;

public final class TitanApplication {
    public static void main(String[] args) {
        var minecraftServer = MinecraftServer.init();
        TitanFlag.VELOCITY_SECRET.ifPresent(VelocityProxy::enable);
        if (TitanFlag.AGONES_SUPPORT.isPresent()) {
            MinecraftServer.getSchedulerManager().buildShutdownTask(AgonesAPI.instance()::shutdown);
        }
        Titan.instance();
        minecraftServer.start("0.0.0.0", 25565);
    }
}
