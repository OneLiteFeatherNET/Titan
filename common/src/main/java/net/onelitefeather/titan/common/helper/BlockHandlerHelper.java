/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
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
    BED_HANDLER(BedHandler::new), JUKEBOX_HANDLER(JukeboxHandler::new), BEACON_HANDLER(
            BeaconHandler::new), SIGN_HANDLER(SignHandler::new), BANNER_HANDLER(
                    BannerHandler::new), SKULL_HANDLER(SkullHandler::new), CANDLE_HANDLER(CandleHandler::new);

    private final Supplier<BlockHandler> blockHandler;
    private static final BlockHandlerHelper[] VALUES = values();

    BlockHandlerHelper(final Supplier<BlockHandler> blockHandler) {
        this.blockHandler = blockHandler;
    }

    private void register() {
        final var handler = this.blockHandler.get();
        MinecraftServer.getBlockManager().registerHandler(handler.getKey(), this.blockHandler);
    }

    public static void registerAll() {
        Arrays.stream(VALUES).forEach(BlockHandlerHelper::register);
    }
}
