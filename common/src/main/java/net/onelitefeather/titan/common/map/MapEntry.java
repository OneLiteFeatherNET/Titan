/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
