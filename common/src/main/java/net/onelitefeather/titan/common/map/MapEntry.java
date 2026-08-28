/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.map;

import net.onelitefeather.titan.common.config.AppConfig;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The {@link MapEntry} record points at one world directory below {@code worlds} and answers
 * whether that directory carries map data.
 *
 * @param path the root directory of the world
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public record MapEntry(Path path) {

    /**
     * Checks whether the world directory carries the map data file.
     *
     * @return true if the file exists, otherwise false
     */
    public boolean hasMapFile() {
        return Files.exists(path.resolve(AppConfig.MAP_FILE_NAME));
    }

    /**
     * Gets the map data file of this world, whether it exists or not.
     *
     * @return the path of the map data file
     */
    public Path getMapFile() {
        return path.resolve(AppConfig.MAP_FILE_NAME);
    }
}
