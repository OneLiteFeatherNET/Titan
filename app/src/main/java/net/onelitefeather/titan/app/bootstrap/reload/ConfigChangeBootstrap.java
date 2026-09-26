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
package net.onelitefeather.titan.app.bootstrap.reload;

import io.avaje.config.Config;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minestom.server.MinecraftServer;
import net.onelitefeather.titan.app.module.ModuleRegistry;

/**
 * Wires a {@link ConfigChangeHandler} to avaje-config's built-in file watcher
 * ({@code config.watch.enabled}, see {@code app/src/main/resources/application.yaml}): takes the
 * current configuration as the handler's starting snapshot, then registers it with
 * {@code Config.onChange(...)}. See
 * {@code openspec/changes/config-reload-feature-flags/design.md}, decision 1.
 *
 * <p>Deliberately a plain static factory called from {@code Titan}, not an Avaje Inject bean:
 * {@code ModuleRegistry} itself is built the same way (see {@code PlatformBeans}'s own javadoc),
 * and wiring this here rather than as a bean keeps a test that only builds the {@code BeanScope}
 * (e.g. {@code ModuleWiringTest}) from also touching
 * {@link MinecraftServer#getSchedulerManager()} or registering a listener on the real, static
 * {@code Config} facade.
 */
public final class ConfigChangeBootstrap {

    private ConfigChangeBootstrap() {
    }

    /**
     * @param moduleRegistry the lobby's module registry - {@link ConfigChangeHandler} restarts run
     *                       through it via a {@link ModuleRestarterAdapter}
     * @return the {@link ConfigChangeHandler} that was registered with {@code Config.onChange(...)}
     *         - its own starting snapshot is the configuration's current flat values at the moment
     *         this method runs
     */
    public static ConfigChangeHandler install(ModuleRegistry moduleRegistry) {
        Objects.requireNonNull(moduleRegistry, "moduleRegistry");

        Map<String, String> initialSnapshot = FlatConfigValues.of(Config.asConfiguration().asProperties());
        ModuleRestarter restarter = new ModuleRestarterAdapter(moduleRegistry);
        // Scheduler extends Executor and its own execute(Runnable) proxies to
        // scheduleNextTick(Runnable) - so the scheduler manager itself is the tick executor, with
        // no adapter of our own needed.
        ConfigChangeHandler handler = new ConfigChangeHandler(restarter, ConfigChangeBootstrap::revert, MinecraftServer.getSchedulerManager(), initialSnapshot);

        Config.onChange(handler);
        return handler;
    }

    /**
     * The production {@link ConfigRevertWriter}: publishes a single
     * {@code Config.eventBuilder("reload-revert")} - every key in {@code puts} back to its old
     * value, every key in {@code removals} removed - then publishes it.
     * {@link ConfigChangeHandler} ignores any event with this name (see its own javadoc), so this
     * revert never triggers another run of the very handler that called it.
     */
    private static void revert(Map<String, String> puts, Set<String> removals) {
        var builder = Config.eventBuilder("reload-revert").putAll(puts);
        for (String key : removals) {
            builder.remove(key);
        }
        builder.publish();
    }
}
