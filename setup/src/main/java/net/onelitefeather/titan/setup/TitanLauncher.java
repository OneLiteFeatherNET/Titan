package net.onelitefeather.titan.setup;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.agones.AgonesAPI;

public class TitanLauncher {
    public static void main(String[] args) {
        var minecraftServer = MinecraftServer.init();
        MinecraftServer.getSchedulerManager().buildShutdownTask(AgonesAPI.instance()::shutdown);
        Titan.instance();
        minecraftServer.start("0.0.0.0", 25565);
    }
}
