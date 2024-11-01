package net.onelitefeather.titan.common.helper;

import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.block.BlockHandler;
import net.onelitefeather.titan.common.blockhandler.BannerHandler;
import net.onelitefeather.titan.common.blockhandler.BeaconHandler;
import net.onelitefeather.titan.common.blockhandler.BedHandler;
import net.onelitefeather.titan.common.blockhandler.CandleHandler;
import net.onelitefeather.titan.common.blockhandler.JukeboxHandler;
import net.onelitefeather.titan.common.blockhandler.SignHandler;
import net.onelitefeather.titan.common.blockhandler.SkullHandler;

import java.util.Arrays;
import java.util.function.Supplier;

public enum BlockHandlerHelper {
    BED_HANDLER(BedHandler::new),
    JUKEBOX_HANDLER(JukeboxHandler::new),
    BEACON_HANDLER(BeaconHandler::new),
    SIGN_HANDLER(SignHandler::new),
    BANNER_HANDLER(BannerHandler::new),
    SKULL_HANDLER(SkullHandler::new),
    CANDLE_HANDLER(CandleHandler::new)
    ;

    private final Supplier<BlockHandler> blockHandler;
    private static final BlockHandlerHelper[] VALUES = values();

    BlockHandlerHelper(final Supplier<BlockHandler> blockHandler) {
        this.blockHandler = blockHandler;
    }

    private void register() {
        final var handler = this.blockHandler.get();
        MinecraftServer.getBlockManager().registerHandler(handler.getNamespaceId(), this.blockHandler);
    }

    public static void registerAll() {
        Arrays.stream(VALUES).forEach(BlockHandlerHelper::register);
    }
}
