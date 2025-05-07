package net.onelitefeather.titan.common.blockhandler;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNull;

public final class JukeboxHandler implements BlockHandler {

    private static final @NotNull Key NAMESPACE_ID = Key.key("minecraft:jukebox");

    @Override
    public @NotNull Key getKey() {
        return NAMESPACE_ID;
    }
}
