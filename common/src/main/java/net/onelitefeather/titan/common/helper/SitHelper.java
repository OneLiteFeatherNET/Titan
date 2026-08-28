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

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.utils.Tags;

import java.util.Optional;

public final class SitHelper {

    private SitHelper() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Sits the player at the given location.
     *
     * @param player
     *                    the player to sit
     * @param sitLocation
     *                    the location to sit at
     * @param config
     *                    the app config
     */
    public static void sitPlayer(Player player, Point sitLocation, AppConfig config) {
        var instance = player.getInstance();
        if (instance == null)
            return;
        if (player.getTag(Tags.SIT_ARROW) != null) {
            removePlayer(player);
        }
        var playerLocation = player.getPosition();
        var arrow = new ArrowEntity();
        arrow.setInstance(instance, sitLocation.add(config.sitOffset()));
        arrow.setInvisible(true);
        arrow.setSilent(true);

        player.setTag(Tags.SIT_PLAYER, playerLocation);
        arrow.addPassenger(player);
        player.setTag(Tags.SIT_ARROW, arrow.getUuid());
    }

    /**
     * Removes the player from the sitting position.
     *
     * @param player
     *               the player to remove
     */
    public static void removePlayer(Player player) {
        Optional.ofNullable(player.getTag(Tags.SIT_ARROW)).map(player.getInstance()::getEntityByUuid).ifPresent(arrow -> {
            player.removeTag(Tags.SIT_ARROW);
            Optional.ofNullable(player.getTag(Tags.SIT_PLAYER)).ifPresent(player::teleport);
            arrow.removePassenger(player);
            if (arrow.getPassengers().isEmpty()) {
                arrow.remove();
            }
        });
    }

    static class ArrowEntity extends Entity {
        public ArrowEntity() {
            super(EntityType.ARROW);
        }

        @Override
        public void update(final long time) {
            super.update(time);
            if (this.getPassengers().isEmpty()) {
                this.remove();
            }
        }
    }

    /**
     * Checks if the player is sitting.
     *
     * @param player
     *               the player to check
     * @return true if the player is sitting, false otherwise
     */
    public static boolean isSitting(Player player) {
        return player.hasTag(Tags.SIT_ARROW);
    }
}
