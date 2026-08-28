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
 * Answers whether a delivery target can be reached right now. This is the question
 * {@link net.onelitefeather.titan.api.deliver.Deliver} cannot answer: it hands a switch request
 * to the CloudNet bridge and returns, so a request for a task nobody is running is
 * indistinguishable
 * from a successful one. A portal has to know beforehand, because a failed switch leaves the player
 * standing in the portal (US-7.03).
 *
 * <p>Implemented in the CloudNet bridge extension realm (where the service list is visible) and
 * invoked from the application through {@link TitanServiceAvailability}, exactly like
 * {@link ServerConnector}. Only JDK types cross that classloader boundary.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public interface ServiceAvailability {

    /**
     * Returns an availability that reports everything as reachable. Only for tests and for
     * deployments that deliberately want a portal to try regardless.
     *
     * @return an availability that never refuses
     */
    static ServiceAvailability alwaysReachable() {
        return new ServiceAvailability() {

            @Override
            public boolean isTaskReachable(String taskName) {
                return true;
            }

            @Override
            public boolean isServerReachable(String serviceName) {
                return true;
            }
        };
    }

    /**
     * Returns an availability that reports nothing as reachable.
     *
     * @return an availability that always refuses
     */
    static ServiceAvailability neverReachable() {
        return new ServiceAvailability() {

            @Override
            public boolean isTaskReachable(String taskName) {
                return false;
            }

            @Override
            public boolean isServerReachable(String serviceName) {
                return false;
            }
        };
    }

    /**
     * Checks whether at least one running service of the given task can take a player.
     *
     * @param taskName the CloudNet task a portal points at
     * @return whether the task currently has a reachable service
     */
    boolean isTaskReachable(String taskName);

    /**
     * Checks whether the named service is running and connected.
     *
     * @param serviceName the CloudNet service a portal points at
     * @return whether that service is currently reachable
     */
    boolean isServerReachable(String serviceName);
}
