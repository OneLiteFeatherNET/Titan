package net.onelitefeather.titan.function;

import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.IChunkLoader;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.onelitefeather.titan.utils.TitanFeatures;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class WorldFunction {

    private static final Path BASE_PATH = Path.of("worlds");
    private static final Instance LOBBY_INSTANCE = MinecraftServer.getInstanceManager().createInstanceContainer(chunkLoader());

    private WorldFunction() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static Instance lobbyInstance() {
        return LOBBY_INSTANCE;
    }


    private static IChunkLoader chunkLoader() {
        return new AnvilLoader(createWorldPath());
    }

    private static @NotNull Path createWorldPath() {
        if (TitanFeatures.HALLOWEEN.isActive()) {
            return BASE_PATH.resolve("halloween");
        } if (TitanFeatures.WINTER.isActive()) {
            return BASE_PATH.resolve("winter");
        }
        return BASE_PATH.resolve("world");
    }

}
