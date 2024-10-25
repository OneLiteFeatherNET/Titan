package net.onelitefeather.titan.common.config;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.NamespaceID;

import java.util.List;

record AppConfigImpl(long tickleDuration,
                     Vec sitOffset,
                     List<NamespaceID> allowedSitBlocks,
                     int simulationDistance,
                     int fireworkBoostSlot,
                     double elytraBoostMultiplier,
                     long updateRateAgones) implements AppConfig {

}
