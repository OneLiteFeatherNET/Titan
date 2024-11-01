package net.onelitefeather.titan.app;

import org.jetbrains.annotations.NotNull;

public final class TitanFlag {

    public static final boolean AGONES_SUPPORT = stringProperty("AGONES_SDK_GRPC_PORT") != null;

    private static String stringProperty(@NotNull String name) {
        return System.getenv(name);
    }

}
