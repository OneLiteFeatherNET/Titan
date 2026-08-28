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
        Assertions.assertFalse(this.access.covers("Build-"), "A name with no service id behind the splitter is no service");
        Assertions.assertFalse(this.access.covers(""));
    }

    @DisplayName("A different task whose name starts with the build task is not the build task")
    @Test
    void testTaskWhoseNameStartsWithTheBuildTask() {
        // CloudNet names a service <task><splitter><number>, so the tail behind the splitter is
        // the deciding evidence: 'Test-1' is not a number, therefore 'Build-Test-1' belongs to a
        // task called Build-Test and must not be guarded as if it were a build server.
        Assertions.assertFalse(this.access.covers("Build-Test"));
        Assertions.assertFalse(this.access.covers("Build-Test-1"));
        Assertions.assertFalse(this.access.covers(server("Build-Test-1")));
    }

    @DisplayName("A task that splits its service names differently is recognised once it is configured")
    @Test
    void testConfiguredNameSplitter() {
        // The splitter is a per-task CloudNet setting (ServiceTask.nameSplitter, default "-"), so
        // a Build task set to "_" produces Build_1 and the rule has to be told about it.
        BuildServerAccess underscore = new BuildServerAccess("Build", "_", BuildServerAccess.PERMISSION);

        Assertions.assertTrue(underscore.covers("Build_1"));
        Assertions.assertTrue(underscore.covers(server("Build_9")), "A stopped one is guarded just the same");
        Assertions.assertFalse(underscore.covers("Build-1"), "A splitter the task does not use names no service of it");
        Assertions.assertFalse(this.access.covers("Build_1"), "The default rule expects CloudNet's default splitter");
    }

    @DisplayName("The name splitter can be overridden per deployment")
    @Test
    void testNameSplitterOverride() {
        String previous = System.getProperty(BuildServerAccess.SPLITTER_PROPERTY);
        try {
            System.setProperty(BuildServerAccess.SPLITTER_PROPERTY, "_");
            BuildServerAccess overridden = BuildServerAccess.defaults();

            Assertions.assertEquals("_", overridden.nameSplitter());
            Assertions.assertTrue(overridden.covers("Build_1"));
        } finally {
            if (previous == null) {
                System.clearProperty(BuildServerAccess.SPLITTER_PROPERTY);
            } else {
                System.setProperty(BuildServerAccess.SPLITTER_PROPERTY, previous);
            }
        }
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
        Assertions.assertEquals(BuildServerAccess.DEFAULT_NAME_SPLITTER, defaults.nameSplitter());
        Assertions.assertEquals("titan.navigator.buildserver", defaults.permission());
    }

    private DeliverComponent task(String taskName) {
        return DeliverComponent.taskBuilder().playerId(this.player).taskName(taskName).build();
    }

    private DeliverComponent server(String serviceName) {
        return DeliverComponent.serverBuilder().playerId(this.player).serverName(serviceName).build();
    }
}
