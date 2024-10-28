package net.onelitefeather.titan.common.helper;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.utils.Tags;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public final class SitHelper {

    private SitHelper() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Sits the player at the given location.
     *
     * @param player the player to sit
     * @param sitLocation the location to sit at
     * @param config the app config
     */
    public static void sitPlayer(Player player, Point sitLocation, AppConfig config) {
        var instance = player.getInstance();
        if (instance == null) return;
        var playerLocation = player.getPosition();
        var arrow = new Entity(EntityType.ARROW);
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
     * @param player the player to remove
     */
    public static void removePlayerFromArrow(Entity arrow) {
        arrow.getPassengers().forEach(passenger -> {
            if (passenger instanceof Player player) {
                player.removeTag(Tags.SIT_ARROW);
                Optional.ofNullable(player.getTag(Tags.SIT_PLAYER)).ifPresent(player::teleport);
            }
        });
        arrow.remove();

    }

    /**
     * Removes the player from the sitting position.
     *
     * @param player the player to remove
     */
    public static void removePlayer(Player player) {
        Optional.ofNullable(player.getTag(Tags.SIT_ARROW))
                .map(player.getInstance()::getEntityByUuid)
                .ifPresent(arrow -> {
            arrow.removePassenger(player);
            arrow.scheduleRemove(Duration.of(1, ChronoUnit.SECONDS));
        });
        Optional.ofNullable(player.getTag(Tags.SIT_PLAYER)).ifPresent(player::teleport);

    }

    /**
     * Checks if the player is sitting.
     *
     * @param player the player to check
     * @return true if the player is sitting, false otherwise
     */
    public static boolean isSitting(Player player) {
        return player.hasTag(Tags.SIT_ARROW);
    }
}
