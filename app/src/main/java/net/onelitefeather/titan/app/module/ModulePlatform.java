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

import net.minestom.server.command.CommandManager;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.common.config.ConfigStore;
import org.jetbrains.annotations.Nullable;

/**
 * The platform-wide services every {@link ModuleContext} is built from: one instance per
 * {@link ModuleRegistry}, shared by every module it starts.
 *
 * <p>Deliberately package-private and never handed to a module directly - it is not a service bag
 * in the public API. A module only ever sees the narrow, per-module views {@link ModuleContext}
 * builds from it ({@link ModuleContext#tasks()}, {@link ModuleContext#commands()},
 * {@link ModuleContext#items()}, {@link ModuleContext#navigator()}, {@link ModuleContext#config}).
 * Keeping every platform service in one record here, instead of one {@link ModuleContext}
 * constructor overload per service, is what lets a later wave add another platform service without
 * adding another constructor.
 *
 * @param scheduler      the scheduler modules' tasks run on
 * @param commandManager the command manager modules register commands on
 * @param config         the {@link ConfigStore} modules read their own section from, or
 *                       {@code null} if none is configured
 * @param items          the platform-wide item registry modules register {@link
 *                       net.onelitefeather.titan.app.module.item.LobbyItem}s through
 * @param navigator      the platform-wide registry modules contribute navigator entries to
 */
record ModulePlatform(Scheduler scheduler, CommandManager commandManager,
                      @Nullable ConfigStore config, ItemRegistry items,
                      NavigatorEntries navigator) {
}
