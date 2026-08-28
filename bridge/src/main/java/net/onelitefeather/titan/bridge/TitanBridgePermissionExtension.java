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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minestom.server.extensions.Extension;
import net.onelitefeather.titan.common.deliver.ServerConnector;
import net.onelitefeather.titan.common.deliver.TitanServerConnector;
import net.onelitefeather.titan.common.navigator.BuildServerAccess;
import net.onelitefeather.titan.common.navigator.TitanBuildServerDirectory;
import net.onelitefeather.titan.common.permission.TitanPermissionBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <li><b>Build servers:</b> installs the directory of reachable build servers the navigator
 * reads (US-5.04). The CloudNet service list is only available here; the application receives
 * nothing but a {@code List<String>} of service names.
 * </ul>
 */
public final class TitanBridgePermissionExtension extends Extension {

    private static final Logger LOGGER = LoggerFactory.getLogger(TitanBridgePermissionExtension.class);

    @Override
    public void initialize() {
        String buildServerTask = BuildServerAccess.defaults().taskName();
        TitanBuildServerDirectory.setDirectory(() -> reachableBuildServers(buildServerTask));

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
    }

    /**
     * Lists the services of the build task that are running and connected right now. Anything
     * that cannot be answered - no driver, a failed lookup - is reported as "no build servers"
     * rather than as a stale list, because the navigator promises reachability (US-5.04).
     *
     * @param taskName the CloudNet task the build servers run under
     * @return the reachable service names
     */
    private static List<String> reachableBuildServers(String taskName) {
        try {
            CloudServiceProvider provider = InjectionLayer.boot().instance(CloudServiceProvider.class);
            List<String> names = new ArrayList<>();
            for (ServiceInfoSnapshot snapshot : provider.servicesByTask(taskName)) {
                if (snapshot.lifeCycle() == ServiceLifeCycle.RUNNING && snapshot.connected()) {
                    names.add(snapshot.name());
                }
            }
            return List.copyOf(names);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not read the CloudNet service list for task {}; reporting no build servers", taskName, exception);
            return List.of();
        }
    }

    private static PlayerExecutor playerExecutor(UUID playerId) {
        var registration = ServiceRegistry.registry().registration(PlayerManager.class, "PlayerManager");
        return registration == null ? null : registration.serviceInstance().playerExecutor(playerId);
    }

    @Override
    public void terminate() {
    }
}
