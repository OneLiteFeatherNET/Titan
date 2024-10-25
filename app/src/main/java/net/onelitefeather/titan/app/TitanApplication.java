package net.onelitefeather.titan.app;

import net.minestom.server.MinecraftServer;

public final class TitanApplication {
    public static void main(String[] args) {
        var minecraftServer = MinecraftServer.init();
        Titan.instance();
        minecraftServer.start("0.0.0.0", 25565);
    }
}
