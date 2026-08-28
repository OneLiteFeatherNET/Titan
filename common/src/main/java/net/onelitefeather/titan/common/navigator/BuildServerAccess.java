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

/**
 * Says which CloudNet destinations count as build servers and which permission they require.
 *
 * <p>This is the rule the <em>guard</em> uses, and it deliberately works on names rather than on
 * the list of servers that happen to be online. A destination is a build server because of the
 * task it belongs to, not because it is currently reachable — otherwise a request naming a stopped
 * build server would slip past the guard as an ordinary destination, and the check in
 * {@link GuardedDeliver} would only hold for as long as the menu was accurate.
 *
 * <p>Which names those are is fixed by CloudNet: {@code ServiceId.name()} returns
 * {@code taskName + nameSplitter + taskServiceId}, where the id is a number and the splitter is
 * <em>configured per task</em> — {@code -} by default, but a task set to {@code _} produces
 * {@code Build_1}. Both halves are therefore configuration here as well, and the numeric tail is
 * required: it is what keeps a different task named {@code Build-Test} from having its services
 * ({@code Build-Test-1}) mistaken for services of {@code Build}.
 *
 * <p>Membership of the <em>menu</em> is a different question and is not answered here. CloudNet
 * answers that one authoritatively through {@code servicesByTask}; see
 * {@link BuildServerDirectory}.
 *
 * @param taskName     the CloudNet task the build servers belong to
 * @param nameSplitter what the task puts between its name and the numeric service id
 * @param permission   the permission required to see and to reach them
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 1.15.0
 */
public record BuildServerAccess(String taskName, String nameSplitter, String permission) {

    /** Permission a team member needs for the build servers (US-5.01). */
    public static final String PERMISSION = "titan.navigator.buildserver";

    /** CloudNet task the build servers run under when nothing else is configured. */
    public static final String DEFAULT_TASK = "Build";

    /** The name splitter CloudNet gives a task that does not configure one. */
    public static final String DEFAULT_NAME_SPLITTER = "-";

    /**
     * System property overriding {@value #DEFAULT_TASK} for a network that names its task
     * differently.
     */
    public static final String TASK_PROPERTY = "titan.buildserver.task";

    /**
     * System property overriding {@value #DEFAULT_NAME_SPLITTER} for a task whose CloudNet
     * {@code nameSplitter} is not the default. The bridge warns when this disagrees with what
     * CloudNet actually reports for the task.
     */
    public static final String SPLITTER_PROPERTY = "titan.buildserver.namesplitter";

    /**
     * Creates a rule for a task that uses CloudNet's default name splitter.
     *
     * @param taskName   the CloudNet task the build servers belong to
     * @param permission the permission required to see and to reach them
     */
    public BuildServerAccess(String taskName, String permission) {
        this(taskName, DEFAULT_NAME_SPLITTER, permission);
    }

    /**
     * Returns the access rule for this deployment: the task from {@value #TASK_PROPERTY} and the
     * splitter from {@value #SPLITTER_PROPERTY}, guarded by {@value #PERMISSION}.
     *
     * @return the configured access rule
     */
    @Contract(value = "-> new", pure = true)
    public static BuildServerAccess defaults() {
        return new BuildServerAccess(property(TASK_PROPERTY, DEFAULT_TASK), property(SPLITTER_PROPERTY, DEFAULT_NAME_SPLITTER), PERMISSION);
    }

    /**
     * Checks whether a name belongs to the build task — either the task name itself or one of its
     * services, {@code <task><splitter><number>}.
     *
     * @param serviceName the CloudNet task or service name, for example {@code Build-1}
     * @return whether the name is the build task or one of its services
     */
    @Contract(pure = true)
    public boolean covers(String serviceName) {
        if (serviceName.equalsIgnoreCase(this.taskName)) {
            return true;
        }
        String prefix = this.taskName + this.nameSplitter;
        if (!serviceName.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return false;
        }
        return isServiceId(serviceName.substring(prefix.length()));
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

    /**
     * Whether the tail behind the splitter is a CloudNet service id, which is a plain number.
     * Anything else — {@code Test-1} behind {@code Build-} — belongs to a different task.
     */
    @Contract(pure = true)
    private static boolean isServiceId(String tail) {
        if (tail.isEmpty()) {
            return false;
        }
        for (int index = 0; index < tail.length(); index++) {
            char character = tail.charAt(index);
            if (character < '0' || character > '9') {
                return false;
            }
        }
        return true;
    }

    private static String property(String key, String fallback) {
        String configured = System.getProperty(key, fallback).trim();
        return configured.isEmpty() ? fallback : configured;
    }
}
