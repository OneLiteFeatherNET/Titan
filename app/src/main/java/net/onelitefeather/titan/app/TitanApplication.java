package net.onelitefeather.titan.app;

import net.minestom.server.MinecraftServer;

public class TitanApplication {

    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();

        Titan titan = new Titan();
        titan.initialize();

        minecraftServer.start("localhost", 25565);
    }
}
