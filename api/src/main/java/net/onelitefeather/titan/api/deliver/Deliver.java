package net.onelitefeather.titan.api.deliver;

import net.minestom.server.entity.Player;
import net.onelitefeather.deliver.DeliverComponent;

/**
 * Represents the delivery interface to send a player to another server
 * @since 2.0.0
 * @version 1.0.0
 */
public interface Deliver {
    void sendPlayer(Player player, DeliverComponent component);
}
