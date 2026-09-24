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
package net.onelitefeather.titan.app.feature.navigator;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.testing.Env;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleRegistry;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.common.config.ConfigStore;
import org.jetbrains.annotations.Nullable;

/**
 * Test-only support for starting a {@link NavigatorModule} against a real {@code Env}, built
 * directly on {@link ModuleRegistry} instead of
 * {@code net.onelitefeather.titan.app.module.testing.ModuleHarness}.
 *
 * <p>{@code ModuleHarness} always builds and owns its own, internal {@link NavigatorEntries} (and
 * {@link ItemRegistry}) - there is no way to hand it one from the outside.
 * {@link NavigatorModule}'s
 * constructor, however, needs that exact {@link NavigatorEntries} instance up front, before
 * {@code enable()} runs, so it can read every module's entries back at open time (see
 * {@code design.md}, decision 8) - not just the narrow, add-only view
 * {@code ModuleContext#navigator()} would give it. Constructing the module and then starting
 * {@code ModuleHarness} with it is therefore impossible: the harness would build its own, different
 * {@link NavigatorEntries} that the already-constructed module never sees.
 *
 * <p>This class builds the same {@link ModuleRegistry} {@code ModuleHarness} would, directly, with
 * the {@link NavigatorEntries} (and {@link ItemRegistry}) the caller already constructed and handed
 * to the module - without changing {@code ModuleHarness} itself, per task 6.4's rule that wave C
 * only adds files under {@code app/feature/navigator} and its test package.
 */
final class NavigatorModuleTestSupport {

    private NavigatorModuleTestSupport() {
    }

    /**
     * What {@link #start} returns: the running registry and the item registry it was built with.
     */
    record Started(ModuleRegistry registry, ItemRegistry items) {
    }

    static Started start(Env env, EventNode<Event> parent, NavigatorEntries entries, LobbyModule... modules) {
        return start(env, parent, entries, null, modules);
    }

    static Started start(Env env, EventNode<Event> parent, NavigatorEntries entries, @Nullable ConfigStore configStore, LobbyModule... modules) {
        ItemRegistry items = new ItemRegistry(parent);
        ModuleRegistry.Builder builder = ModuleRegistry.builder().parent(parent).scheduler(env.process().scheduler()).commandManager(env.process().command()).navigator(entries).items(items).modules(modules);
        if (configStore != null) {
            builder.config(configStore);
        }
        ModuleRegistry registry = builder.build();
        registry.enableAll();
        return new Started(registry, items);
    }
}
