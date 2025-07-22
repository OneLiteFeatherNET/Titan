/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
