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

import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

public record MapEntry(@NotNull Path path) {

    /** The file name a lobby map is persisted under, inside a map's own directory. */
    public static final String MAP_FILE_NAME = "map.json";

    public boolean hasMapFile() {
        return Files.exists(path.resolve(MAP_FILE_NAME));
    }

    public @NotNull Path getMapFile() {
        return path.resolve(MAP_FILE_NAME);
    }
}
