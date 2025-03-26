package net.onelitefeather.titan.setup;

import net.minestom.server.MinecraftServer;

public class TitanLauncher {
    public static void main(String[] args) {
        var minecraftServer = MinecraftServer.init();
        Titan.instance();
        minecraftServer.start("0.0.0.0", 25565);
    }
}
