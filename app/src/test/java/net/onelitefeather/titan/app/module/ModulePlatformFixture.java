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
package net.onelitefeather.titan.app.module;

import java.util.UUID;
import net.minestom.server.command.CommandManager;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.common.config.ConfigSections;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a fresh {@link ModulePlatform} for tests that need one directly, without going through
 * {@link ModuleRegistry}. Every call returns its own, unshared {@link ItemRegistry} and
 * {@link NavigatorEntries}, backed by their own event node, so tests using this fixture stay
 * independent of each other (F.I.R.S.T) instead of sharing platform state through a static field.
 *
 * <p>Production code never uses this - {@link ModuleRegistry} builds its one {@link ModulePlatform}
 * from its {@link ModuleRegistry.Builder} instead, with the platform's own defaults.
 */
final class ModulePlatformFixture {

    private ModulePlatformFixture() {
    }

    /**
     * @param scheduler      the scheduler to back the platform with
     * @param commandManager the command manager to back the platform with
     * @return a fresh platform with no {@link ConfigSections} configured
     */
    static ModulePlatform create(Scheduler scheduler, CommandManager commandManager) {
        return create(scheduler, commandManager, null);
    }

    /**
     * @param scheduler      the scheduler to back the platform with
     * @param commandManager the command manager to back the platform with
     * @param configSections the {@link ConfigSections} to back the platform with, or {@code null}
     *                       for none
     * @return a fresh platform, with its own, unshared item registry and navigator entries
     */
    static ModulePlatform create(Scheduler scheduler, CommandManager commandManager, @Nullable ConfigSections configSections) {
        EventNode<Event> platformNode = EventNode.all("test-module-platform-fixture/" + UUID.randomUUID());
        return new ModulePlatform(scheduler, commandManager, configSections, new ItemRegistry(platformNode), new NavigatorEntries());
    }
}
