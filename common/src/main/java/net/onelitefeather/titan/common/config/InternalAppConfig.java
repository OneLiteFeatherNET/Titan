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
package net.onelitefeather.titan.common.config;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;

import java.util.List;

public record InternalAppConfig(long tickleDuration, Vec sitOffset, List<Key> allowedSitBlocks, int simulationDistance,
		int fireworkBoostSlot, double elytraBoostMultiplier, int maxHeightBeforeTeleport,
		int minHeightBeforeTeleport) implements AppConfig {

	public static AppConfig defaultConfig() {
		return Instances.DEFAULT;
	}

	@Override
	public Component displayConfig() {
		return Component.empty();
	}

	static final class Instances {

		private static final long DEFAULT_TICKLE_DURATION = 4000;
		private static final Vec DEFAULT_SIT_OFFSET = new Vec(0.5, 0.25, 0.5);
		private static final List<Key> DEFAULT_ALLOWED_SIT_BLOCKS = List.of(Material.SPRUCE_STAIRS.key());
		private static final int DEFAULT_SIMULATION_DISTANCE = 2;
		private static final int DEFAULT_FIREWORK_BOOST_SLOT = 45;
		private static final double DEFAULT_ELYTRA_BOOST_MULTIPLIER = 35.0;
		private static final int DEFAULT_MAX_HEIGHT_BEFORE_TELEPORT = 310;
		private static final int DEFAULT_MIN_HEIGHT_BEFORE_TELEPORT = -64;

		private static final InternalAppConfig DEFAULT;

		static {
			DEFAULT = new InternalAppConfig(DEFAULT_TICKLE_DURATION, DEFAULT_SIT_OFFSET, DEFAULT_ALLOWED_SIT_BLOCKS,
					DEFAULT_SIMULATION_DISTANCE, DEFAULT_FIREWORK_BOOST_SLOT, DEFAULT_ELYTRA_BOOST_MULTIPLIER,
					DEFAULT_MAX_HEIGHT_BEFORE_TELEPORT, DEFAULT_MIN_HEIGHT_BEFORE_TELEPORT);
		}

		private Instances() {
			throw new UnsupportedOperationException("This class cannot be instantiated");
		}
	}
}
