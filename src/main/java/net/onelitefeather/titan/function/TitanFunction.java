package net.onelitefeather.titan.function;

import net.minestom.server.entity.Player;
import org.togglz.core.user.SimpleFeatureUser;

public interface TitanFunction {
    default SimpleFeatureUser toUser(Player player) {
        return new SimpleFeatureUser(player.getUsername());
    }
}
