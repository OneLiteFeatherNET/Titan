package net.onelitefeather.titan.app;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class TitanFlag {

    public static final Optional<String> AGONES_SUPPORT = Optional.ofNullable(stringEnvironment("AGONES_SDK_GRPC_PORT"));

    public static final Optional<String> VELOCITY_SECRET = Optional.ofNullable(stringEnvironment("VELOCITY_SECRET"));

    private static String stringEnvironment(@NotNull String name) {
        return System.getenv(name);
    }

}
