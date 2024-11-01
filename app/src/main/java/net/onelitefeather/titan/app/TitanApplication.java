package net.onelitefeather.titan.app;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.agones.AgonesAPI;

public final class TitanApplication {
    public static void main(String[] args) {
        var minecraftServer = MinecraftServer.init();
        if (TitanFlag.AGONES_SUPPORT) {
            MinecraftServer.getSchedulerManager().buildShutdownTask(AgonesAPI.instance()::shutdown);
        }
        Titan.instance();
        minecraftServer.start("0.0.0.0", 25565);
    }
}
