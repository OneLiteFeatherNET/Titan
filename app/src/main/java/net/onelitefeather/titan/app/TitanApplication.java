package net.onelitefeather.titan.app;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.modules.bridge.impl.platform.minestom.MinestomBridgeExtension;
import net.minestom.server.MinecraftServer;

public class TitanApplication {

    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();

        Titan titan = new Titan();
        titan.initialize();
        InjectionLayer.ext().instance(MinestomBridgeExtension.class).onLoad();

        minecraftServer.start("localhost", 25565);
    }
}
