package net.onelitefeather.titan.common.map;

import net.onelitefeather.titan.common.config.AppConfig;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

public record MapEntry(@NotNull Path path) {

    public boolean hasMapFile() {
        return Files.exists(path.resolve(AppConfig.MAP_FILE_NAME));
    }

    public @NotNull Path getMapFile() {
        return path.resolve(AppConfig.MAP_FILE_NAME);
    }
}
