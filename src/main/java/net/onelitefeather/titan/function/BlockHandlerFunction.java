package net.onelitefeather.titan.function;

import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.block.BlockHandler;
import net.onelitefeather.titan.blockhandler.BannerHandler;
import net.onelitefeather.titan.blockhandler.BeaconHandler;
import net.onelitefeather.titan.blockhandler.BedHandler;
import net.onelitefeather.titan.blockhandler.CandleHandler;
import net.onelitefeather.titan.blockhandler.JukeboxHandler;
import net.onelitefeather.titan.blockhandler.SignHandler;
import net.onelitefeather.titan.blockhandler.SkullHandler;

import java.util.Arrays;
import java.util.function.Supplier;

public enum BlockHandlerFunction {
    BED_HANDLER(BedHandler::new),
    JUKEBOX_HANDLER(JukeboxHandler::new),
    BEACON_HANDLER(BeaconHandler::new),
    SIGN_HANDLER(SignHandler::new),
    BANNER_HANDLER(BannerHandler::new),
    SKULL_HANDLER(SkullHandler::new),
    CANDLE_HANDLER(CandleHandler::new)
    ;

    private final Supplier<BlockHandler> blockHandler;
    private static final BlockHandlerFunction[] VALUES = values();

    BlockHandlerFunction(final Supplier<BlockHandler> blockHandler) {
        this.blockHandler = blockHandler;
    }

    private void register() {
        final var handler = this.blockHandler.get();
        MinecraftServer.getBlockManager().registerHandler(handler.getNamespaceId(), this.blockHandler);
    }

    public static void registerAll() {
        Arrays.stream(VALUES).forEach(BlockHandlerFunction::register);
    }
}
