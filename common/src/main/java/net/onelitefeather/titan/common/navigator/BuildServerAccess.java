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

import net.onelitefeather.deliver.DeliverComponent;
import org.jetbrains.annotations.Contract;

import java.util.Locale;

/**
 * Says which CloudNet destinations count as build servers and which permission they require.
 *
 * <p>The membership test deliberately works on names rather than on the list of servers that
 * happen to be online. A destination is a build server because of the task it belongs to, not
 * because it is currently reachable — otherwise a request naming a stopped build server would
 * slip past the guard as an ordinary destination, and the check in {@link GuardedDeliver} would
 * only hold for as long as the menu was accurate. CloudNet names a service {@code <task>-<id>},
 * so the task name is recoverable from the service name alone, with JDK types only.
 *
 * @param taskName   the CloudNet task the build servers belong to
 * @param permission the permission required to see and to reach them
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public record BuildServerAccess(String taskName, String permission) {

    /** Permission a team member needs for the build servers (US-5.01). */
    public static final String PERMISSION = "titan.navigator.buildserver";

    /** CloudNet task the build servers run under when nothing else is configured. */
    public static final String DEFAULT_TASK = "Build";

    /**
     * System property overriding {@value #DEFAULT_TASK} for a network that names its task
     * differently.
     */
    public static final String TASK_PROPERTY = "titan.buildserver.task";

    /**
     * Returns the access rule for this deployment: the task from {@value #TASK_PROPERTY} or
     * {@value #DEFAULT_TASK}, guarded by {@value #PERMISSION}.
     *
     * @return the configured access rule
     */
    @Contract(value = "-> new", pure = true)
    public static BuildServerAccess defaults() {
        String configured = System.getProperty(TASK_PROPERTY, DEFAULT_TASK).trim();
        return new BuildServerAccess(configured.isEmpty() ? DEFAULT_TASK : configured, PERMISSION);
    }

    /**
     * Checks whether a service name belongs to the build task.
     *
     * @param serviceName the CloudNet service name, for example {@code Build-1}
     * @return whether the service is a build server
     */
    @Contract(pure = true)
    public boolean covers(String serviceName) {
        String name = serviceName.toLowerCase(Locale.ROOT);
        String task = this.taskName.toLowerCase(Locale.ROOT);
        return name.equals(task) || name.startsWith(task + "-");
    }

    /**
     * Checks whether a requested switch targets a build server, whether it names the task or one
     * of its services.
     *
     * @param component the requested switch
     * @return whether the request needs {@link #permission()}
     */
    @Contract(pure = true)
    public boolean covers(DeliverComponent component) {
        return switch (component) {
            case DeliverComponent.TaskComponent taskComponent -> covers(taskComponent.taskName());
            case DeliverComponent.ServerDeliverComponent serverComponent ->
                covers(serverComponent.gameServer());
            default -> false;
        };
    }
}
