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
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.bridge.impl.platform.minestom.MinestomPermissionChecker;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
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
 *
 * <p>Driver services are resolved through {@link InjectionLayer#ext()}, the layer CloudNet
 * documents for "all kinds of external component injection (like plugins)" — an extension is such
 * a component. The ext layer is created as a child of the boot layer
 * ({@code InjectionLayerProvider.boot()} ends with {@code ext = child(boot, "ext")}), so every
 * boot binding is visible through it and nothing is lost by not asking boot directly.
 */
public final class TitanBridgePermissionExtension extends Extension {

    private static final Logger LOGGER = LoggerFactory.getLogger(TitanBridgePermissionExtension.class);

    /** Guards the one-off name-splitter check so a mismatch is reported once, not every menu. */
    private static final AtomicBoolean NAME_SPLITTER_CHECKED = new AtomicBoolean();

    @Override
    public void initialize() {
        BuildServerAccess buildServerAccess = BuildServerAccess.defaults();
        TitanBuildServerDirectory.setDirectory(() -> reachableBuildServers(buildServerAccess));

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
     * Lists the services of the build task that are running and connected right now. CloudNet is
     * asked by task name, so it - not a name pattern on this side - decides which services belong
     * to the task; the application offers what comes back unchanged. Anything that cannot be
     * answered - no driver, a failed lookup - is reported as "no build servers" rather than as a
     * stale list, because the navigator promises reachability (US-5.04).
     *
     * @param access the task the build servers run under and how it names its services
     * @return the reachable service names
     */
    private static List<String> reachableBuildServers(BuildServerAccess access) {
        verifyNameSplitter(access);
        try {
            CloudServiceProvider provider = InjectionLayer.ext().instance(CloudServiceProvider.class);
            List<String> names = new ArrayList<>();
            for (ServiceInfoSnapshot snapshot : provider.servicesByTask(access.taskName())) {
                if (snapshot.lifeCycle() == ServiceLifeCycle.RUNNING && snapshot.connected()) {
                    names.add(snapshot.name());
                }
            }
            return List.copyOf(names);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not read the CloudNet service list for task {}; reporting no build servers", access.taskName(), exception);
            return List.of();
        }
    }

    /**
     * Compares the name splitter Titan assumes against the one the task is really configured with,
     * once the driver can answer. The menu does not depend on the splitter any more, but the guard
     * in {@code GuardedDeliver} does: it has to recognise a build server by name, including one
     * that has stopped and is in no service list. A task switched to {@code _} while Titan still
     * assumes {@code -} would leave those destinations unguarded, and nothing else would say so.
     *
     * <p>The result is reported once and then never again - the check is diagnostics, and a line
     * per opened menu would be noise. A lookup that fails is not a result: the flag stays down so
     * the next menu tries again, and the failure itself stays at debug level because the service
     * list below reports the same outage where an operator will look for it.
     *
     * @param access the task and splitter Titan is configured with
     */
    private static void verifyNameSplitter(BuildServerAccess access) {
        if (NAME_SPLITTER_CHECKED.get()) {
            return;
        }
        try {
            ServiceTask task = InjectionLayer.ext().instance(ServiceTaskProvider.class).serviceTask(access.taskName());
            if (!NAME_SPLITTER_CHECKED.compareAndSet(false, true)) {
                return;
            }
            if (task == null) {
                LOGGER.warn("CloudNet knows no task named {}; the navigator will never offer a build server. Set -D{} if the task is named differently.", access.taskName(), BuildServerAccess.TASK_PROPERTY);
            } else if (!task.nameSplitter().equals(access.nameSplitter())) {
                LOGGER.warn("Task {} names its services with the splitter '{}' but Titan assumes '{}', so a build server would not be recognised as one when a switch is requested. Set -D{}={}.", access.taskName(), task.nameSplitter(), access.nameSplitter(), BuildServerAccess.SPLITTER_PROPERTY, task.nameSplitter());
            }
        } catch (RuntimeException exception) {
            LOGGER.debug("Could not read the CloudNet task {} to verify its name splitter", access.taskName(), exception);
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
