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
package net.onelitefeather.titan.bridge;

import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.modules.bridge.impl.platform.minestom.MinestomPermissionChecker;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minestom.server.extensions.Extension;
import net.onelitefeather.minestom.extensions.processor.ExtensionInfo;
import net.onelitefeather.titan.bridge.ServiceTotals.ServiceReading;
import net.onelitefeather.titan.common.deliver.PlayerCountLookup;
import net.onelitefeather.titan.common.deliver.ServerConnector;
import net.onelitefeather.titan.common.deliver.TitanPlayerCountLookup;
import net.onelitefeather.titan.common.deliver.TitanServerConnector;
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
 * <li><b>Player counts:</b> installs a {@link PlayerCountLookup} (used by
 * {@code HolderPlayerCounts})
 * that sums the bridge's player counts of the running services of a task or group, or reads one
 * service by name.
 * </ul>
 *
 * <p>{@link ExtensionInfo} generates {@code extension.json} at compile time; the version is
 * supplied by the build through {@code -Aminestom.extension.version}.
 */
@ExtensionInfo(
        name = "TitanCloudNetPermissions", authors = "OneLiteFeather", dependencies = "CloudNet_Bridge"
)
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

        TitanPlayerCountLookup.setLookup(new PlayerCountLookup() {
            @Override
            public boolean supports(String type) {
                return type.equals("task") || type.equals("group") || type.equals("service");
            }

            @Override
            public int[] lookup(String type, String name) {
                return ServiceTotals.totalOrNotRunning(() -> services(type, name).stream().map(TitanBridgePermissionExtension::reading).toList());
            }
        });
    }

    private static Collection<ServiceInfoSnapshot> services(String type, String name) {
        CloudServiceProvider provider = ServiceRegistry.registry().defaultInstance(CloudServiceProvider.class);
        if (provider == null) {
            return List.of();
        }
        return switch (type) {
            case "task" -> provider.servicesByTask(name);
            case "group" -> provider.servicesByGroup(name);
            case "service" -> {
                ServiceInfoSnapshot service = provider.serviceByName(name);
                yield service == null ? List.of() : List.of(service);
            }
            default -> List.of();
        };
    }

    private static ServiceReading reading(ServiceInfoSnapshot service) {
        return new ServiceReading(service.lifeCycle() == ServiceLifeCycle.RUNNING, count(service.readProperty(BridgeDocProperties.ONLINE_COUNT)), count(service.readProperty(BridgeDocProperties.MAX_PLAYERS)));
    }

    private static int count(Integer value) {
        return value == null ? 0 : value;
    }

    private static PlayerExecutor playerExecutor(UUID playerId) {
        var registration = ServiceRegistry.registry().registration(PlayerManager.class, "PlayerManager");
        return registration == null ? null : registration.serviceInstance().playerExecutor(playerId);
    }

    @Override
    public void terminate() {
    }
}
