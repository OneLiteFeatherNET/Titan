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
package net.onelitefeather.titan.bridge;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.modules.bridge.impl.platform.minestom.MinestomPermissionChecker;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import java.util.UUID;
import net.minestom.server.extensions.Extension;
import net.onelitefeather.titan.common.deliver.ServerConnector;
import net.onelitefeather.titan.common.deliver.ServiceAvailability;
import net.onelitefeather.titan.common.deliver.TitanServerConnector;
import net.onelitefeather.titan.common.deliver.TitanServiceAvailability;
import net.onelitefeather.titan.common.permission.TitanPermissionBridge;

/**
 * Minestom extension that wires the CloudNet bridge to Titan across the classloader boundary.
 *
 * <p>Both pieces of glue need bridge classes ({@link MinestomPermissionChecker},
 * {@link PlayerManager}) that are only visible inside the bridge's extension classloader, so they
 * cannot live in the application. By declaring a dependency on the {@code CloudNet_Bridge}
 * extension (see {@code extension.json}), this extension loads after the bridge and shares its
 * classloader hierarchy. It exchanges only JDK types with the application through the holders in
 * {@code common}.
 *
 * <ul>
 * <li><b>Permissions:</b> the bridge ships a default checker that only inspects {@code
 *       player.getPermissionLevel()}. This registers a LuckPerms-backed checker and marks it the
 * registry default; the lookup is delegated back to the application via
 * {@link TitanPermissionBridge}.
 * <li><b>Server switching:</b> installs a {@link ServerConnector} (used by
 * {@code MessageChannelDeliver}) that connects players through the bridge
 * {@link PlayerManager} / {@link PlayerExecutor}.
 * <li><b>Reachability:</b> installs a {@link ServiceAvailability} over the CloudNet service
 * list. Portals ask it before switching a player, because the switch itself reports nothing
 * back (US-7.03).
 * </ul>
 */
public final class TitanBridgePermissionExtension extends Extension {

    @Override
    public void initialize() {
        MinestomPermissionChecker checker = (player, permission) -> TitanPermissionBridge.hasPermission(player.getUuid(), permission);
        ServiceRegistry.registry().registerProvider(MinestomPermissionChecker.class, "titan-luckperms", checker).markAsDefaultService();

        TitanServerConnector.setConnector(new ServerConnector() {
            @Override
            public void connectToTask(UUID playerId, String taskName) {
                PlayerExecutor executor = playerExecutor(playerId);
                if (executor != null) {
                    executor.connectToTask(taskName, ServerSelectorType.LOWEST_PLAYERS);
                }
            }

            @Override
            public void connectToServer(UUID playerId, String serviceName) {
                PlayerExecutor executor = playerExecutor(playerId);
                if (executor != null) {
                    executor.connect(serviceName);
                }
            }
        });
        installServiceAvailability();
    }

    private static void installServiceAvailability() {
        TitanServiceAvailability.setAvailability(new ServiceAvailability() {

            @Override
            public boolean isTaskReachable(String taskName) {
                CloudServiceProvider provider = serviceProvider();
                if (provider == null) {
                    return false;
                }
                return provider.servicesByTask(taskName).stream().anyMatch(TitanBridgePermissionExtension::joinable);
            }

            @Override
            public boolean isServerReachable(String serviceName) {
                CloudServiceProvider provider = serviceProvider();
                if (provider == null) {
                    return false;
                }
                return joinable(provider.serviceByName(serviceName));
            }
        });
    }

    /**
     * Resolves the service list lazily and never lets a resolution failure escape: this runs on a
     * player walking into a portal, and an exception there would leave them with neither a switch
     * nor a message.
     */
    private static CloudServiceProvider serviceProvider() {
        try {
            return InjectionLayer.ext().instance(CloudServiceProvider.class);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean joinable(ServiceInfoSnapshot snapshot) {
        return snapshot != null && snapshot.lifeCycle() == ServiceLifeCycle.RUNNING && snapshot.connected();
    }

    private static PlayerExecutor playerExecutor(UUID playerId) {
        var registration = ServiceRegistry.registry().registration(PlayerManager.class, "PlayerManager");
        return registration == null ? null : registration.serviceInstance().playerExecutor(playerId);
    }

    @Override
    public void terminate() {
    }
}
