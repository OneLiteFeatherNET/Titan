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
package net.onelitefeather.titan.common.config;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;

import java.util.List;

public record InternalAppConfig(long tickleDuration, Vec sitOffset, List<Key> allowedSitBlocks,
                                int simulationDistance,
                                int fireworkBoostSlot, double elytraBoostMultiplier,
                                int maxHeightBeforeTeleport,
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
            DEFAULT = new InternalAppConfig(DEFAULT_TICKLE_DURATION, DEFAULT_SIT_OFFSET, DEFAULT_ALLOWED_SIT_BLOCKS, DEFAULT_SIMULATION_DISTANCE, DEFAULT_FIREWORK_BOOST_SLOT, DEFAULT_ELYTRA_BOOST_MULTIPLIER, DEFAULT_MAX_HEIGHT_BEFORE_TELEPORT, DEFAULT_MIN_HEIGHT_BEFORE_TELEPORT);
        }

        private Instances() {
            throw new UnsupportedOperationException("This class cannot be instantiated");
        }
    }
}
