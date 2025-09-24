package net.onelitefeather.titan.app.player;

import net.kyori.adventure.util.TriState;
import net.luckperms.api.util.Tristate;

final class CompatibilityUtil {
    private  CompatibilityUtil() {
    }

    static TriState convertTriState(Tristate tristate) {
        return switch (tristate) {
            case TRUE -> TriState.TRUE;
            case FALSE -> TriState.FALSE;
            case UNDEFINED -> TriState.NOT_SET;
        };
    }
}
