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
package net.onelitefeather.titan.common.navigator;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Cross-classloader holder for the reachable build servers.
 *
 * <p>Lives on the shared application classloader so the CloudNet bridge extension can install a
 * {@link BuildServerDirectory} that the application can read without ever naming a CloudNet
 * class. Until the extension installs one — standalone runs, tests, the window before the bridge
 * has started — the holder reports no build servers, which hides them rather than guessing.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class TitanBuildServerDirectory {

    private static volatile @Nullable BuildServerDirectory directory;

    private TitanBuildServerDirectory() {
    }

    /**
     * Installs the directory. Called by the bridge extension once the CloudNet driver is up.
     *
     * @param buildServerDirectory the directory backed by the CloudNet service list
     */
    public static void setDirectory(BuildServerDirectory buildServerDirectory) {
        directory = buildServerDirectory;
    }

    /**
     * Returns the currently reachable build servers, or an empty list when no directory has been
     * installed.
     *
     * @return the reachable service names
     */
    public static List<String> reachableServices() {
        BuildServerDirectory current = directory;
        return current == null ? List.of() : current.reachableServices();
    }
}
