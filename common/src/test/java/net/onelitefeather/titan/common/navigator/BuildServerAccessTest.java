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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class BuildServerAccessTest {

    private final BuildServerAccess access = new BuildServerAccess("Build", BuildServerAccess.PERMISSION);
    private final UUID player = UUID.randomUUID();

    @DisplayName("A service of the build task is recognised by its name")
    @Test
    void testServiceNamesOfTheBuildTask() {
        Assertions.assertTrue(this.access.covers("Build-1"));
        Assertions.assertTrue(this.access.covers("Build-42"));
        Assertions.assertTrue(this.access.covers("Build"));
        Assertions.assertTrue(this.access.covers("build-1"), "CloudNet task names are not case sensitive");
    }

    @DisplayName("A service that only starts like the build task is not one")
    @Test
    void testForeignServiceNames() {
        Assertions.assertFalse(this.access.covers("Lobby-1"));
        Assertions.assertFalse(this.access.covers("BuildBattle-1"), "A different task with the same prefix is not the build task");
        Assertions.assertFalse(this.access.covers(""));
    }

    @DisplayName("A stopped build server is still a build server")
    @Test
    void testOfflineBuildServerIsStillGuarded() {
        // The guard must not depend on the service being in the reachable list: otherwise a
        // request naming a stopped build server would look like an ordinary destination.
        Assertions.assertTrue(this.access.covers(server("Build-9")), "Membership follows the task name, not the service list");
    }

    @DisplayName("Both a task request and a service request are guarded")
    @Test
    void testComponentsAreCovered() {
        Assertions.assertTrue(this.access.covers(task("Build")));
        Assertions.assertTrue(this.access.covers(server("Build-1")));
        Assertions.assertFalse(this.access.covers(task("Survival")));
        Assertions.assertFalse(this.access.covers(server("Lobby-1")));
    }

    @DisplayName("The task name can be overridden per deployment")
    @Test
    void testTaskNameOverride() {
        String previous = System.getProperty(BuildServerAccess.TASK_PROPERTY);
        try {
            System.setProperty(BuildServerAccess.TASK_PROPERTY, "Bauserver");
            BuildServerAccess overridden = BuildServerAccess.defaults();

            Assertions.assertEquals("Bauserver", overridden.taskName());
            Assertions.assertTrue(overridden.covers("Bauserver-1"));
            Assertions.assertFalse(overridden.covers("Build-1"));
        } finally {
            if (previous == null) {
                System.clearProperty(BuildServerAccess.TASK_PROPERTY);
            } else {
                System.setProperty(BuildServerAccess.TASK_PROPERTY, previous);
            }
        }
    }

    @DisplayName("Without an override the defaults name the Build task and the navigator permission")
    @Test
    void testDefaults() {
        BuildServerAccess defaults = BuildServerAccess.defaults();

        Assertions.assertEquals(BuildServerAccess.DEFAULT_TASK, defaults.taskName());
        Assertions.assertEquals("titan.navigator.buildserver", defaults.permission());
    }

    private DeliverComponent task(String taskName) {
        return DeliverComponent.taskBuilder().playerId(this.player).taskName(taskName).build();
    }

    private DeliverComponent server(String serviceName) {
        return DeliverComponent.serverBuilder().playerId(this.player).serverName(serviceName).build();
    }
}
