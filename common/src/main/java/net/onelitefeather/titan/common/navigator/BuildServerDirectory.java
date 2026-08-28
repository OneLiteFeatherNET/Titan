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

import java.util.List;

/**
 * Lists the build servers that are reachable right now (US-5.04).
 *
 * <p>CloudNet is the source of that list, and CloudNet classes are not on the application
 * classpath: the bridge runs in its own extension classloader. The production implementation
 * therefore lives in the {@code :bridge} extension and is installed through
 * {@link TitanBuildServerDirectory}; only a {@code List<String>} crosses the boundary. This is
 * the same rule {@code net.onelitefeather.titan.common.deliver.ServerConnector} and
 * {@code net.onelitefeather.titan.common.permission.TitanPermissionBridge} follow.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
@FunctionalInterface
public interface BuildServerDirectory {

    /**
     * Returns a directory that never reports a build server. Used standalone — local runs, tests
     * and AOT training — where there is no CloudNet to ask.
     *
     * @return a directory reporting nothing
     */
    static BuildServerDirectory empty() {
        return List::of;
    }

    /**
     * Returns the names of the build-task services that are running and connected at this moment.
     *
     * @return the reachable service names; empty when CloudNet is absent or cannot be reached
     */
    List<String> reachableServices();
}
