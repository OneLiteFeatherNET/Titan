package net.onelitefeather.titan.common.utils;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.tag.Tag;

import java.util.UUID;

/**
 * The {@link Tags} class contains all tags used in the project. It is a utility class and should not be instantiated.
 *
 * @author themeinerlp
 * @version 2.0.0
 * @since 2.0.0
 **/
public final class Tags {

    public static final Tag<Long> TICKLE_COOLDOWN = Tag.Long("tickle_cooldown");
    public static final Tag<UUID> SIT_ARROW = Tag.UUID("SIT_ARROW");
    public static final Tag<Pos> SIT_PLAYER = Tag.Structure("SIT_PLAYER", Pos.class);

    private Tags() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
