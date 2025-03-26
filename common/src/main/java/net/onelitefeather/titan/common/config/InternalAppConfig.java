package net.onelitefeather.titan.common.config;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;

import java.util.List;

public record InternalAppConfig(
    long tickleDuration,
    Vec sitOffset,
    List<NamespaceID> allowedSitBlocks,
    int simulationDistance,
    int fireworkBoostSlot,
    double elytraBoostMultiplier,
    int maxHeightBeforeTeleport,
    int minHeightBeforeTeleport
) implements AppConfig {

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
        private static final List<NamespaceID> DEFAULT_ALLOWED_SIT_BLOCKS = List.of(Material.SPRUCE_STAIRS.namespace());
        private static final int DEFAULT_SIMULATION_DISTANCE = 2;
        private static final int DEFAULT_FIREWORK_BOOST_SLOT = 45;
        private static final double DEFAULT_ELYTRA_BOOST_MULTIPLIER = 35.0;
        private static final int DEFAULT_MAX_HEIGHT_BEFORE_TELEPORT = 310;
        private static final int DEFAULT_MIN_HEIGHT_BEFORE_TELEPORT = -64;

        private static final InternalAppConfig DEFAULT;

        static {
            DEFAULT = new InternalAppConfig(
                    DEFAULT_TICKLE_DURATION,
                    DEFAULT_SIT_OFFSET,
                    DEFAULT_ALLOWED_SIT_BLOCKS,
                    DEFAULT_SIMULATION_DISTANCE,
                    DEFAULT_FIREWORK_BOOST_SLOT,
                    DEFAULT_ELYTRA_BOOST_MULTIPLIER,
                    DEFAULT_MAX_HEIGHT_BEFORE_TELEPORT,
                    DEFAULT_MIN_HEIGHT_BEFORE_TELEPORT
            );
        }

        private Instances() {
            throw new UnsupportedOperationException("This class cannot be instantiated");
        }
    }
}
