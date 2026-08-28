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
package net.onelitefeather.titan.common.deliver;

/**
 * Cross-classloader holder for {@link ServiceAvailability}, the twin of
 * {@link TitanServerConnector}.
 *
 * <p>The CloudNet service list lives in the bridge extension classloader; the application cannot
 * reference those classes. This holder lives on the shared application classloader, the bridge
 * extension installs the real source, and the application asks through JDK types only.
 *
 * <p><b>Nothing installed means nothing is reachable.</b> That is not a pessimistic guess: the
 * same missing bridge that leaves this holder empty also leaves {@link TitanServerConnector}
 * empty, so a delivery would be dropped on the floor. Reporting "unreachable" makes a portal say
 * so instead of appearing to work (US-7.03).
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class TitanServiceAvailability {

    private static volatile ServiceAvailability availability;

    private TitanServiceAvailability() {
    }

    /**
     * Installs the availability source. Called by the bridge extension once the bridge is up.
     *
     * @param serviceAvailability the source backed by the CloudNet service list
     */
    public static void setAvailability(ServiceAvailability serviceAvailability) {
        availability = serviceAvailability;
    }

    /**
     * Returns a view on whatever source is installed at the time of each call, so a portal built
     * before the bridge extension loaded still sees the bridge answers afterwards.
     *
     * @return a live view on the installed availability source
     */
    public static ServiceAvailability holder() {
        return new ServiceAvailability() {

            @Override
            public boolean isTaskReachable(String taskName) {
                ServiceAvailability current = availability;
                return current != null && current.isTaskReachable(taskName);
            }

            @Override
            public boolean isServerReachable(String serviceName) {
                ServiceAvailability current = availability;
                return current != null && current.isServerReachable(serviceName);
            }
        };
    }

    /**
     * Returns whether a source has been installed at all. Used for logging: "no portal target is
     * reachable" reads very differently from "the bridge is not there yet".
     *
     * @return whether the bridge installed an availability source
     */
    public static boolean isInstalled() {
        return availability != null;
    }
}
