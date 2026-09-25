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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.app.module.ModuleRegistry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wires a production {@link ConfigReloader} - the real avaje-config-backed
 * {@link ConfigSnapshotSource}/{@link LiveConfig}, a {@link ModuleRestarterAdapter} over the
 * lobby's {@link ModuleRegistry}, a virtual-thread worker executor and the Minestom scheduler as
 * the tick executor - and, unless polling is turned off, a repeating {@link ConfigFileWatcher}
 * task that triggers it. See {@code openspec/changes/config-reload-feature-flags/design.md},
 * decisions 1 and 2.
 *
 * <p>Deliberately a plain static factory called from {@code Titan}, not an Avaje Inject bean:
 * {@code ModuleRegistry} itself is built the same way (see {@code PlatformBeans}'s own javadoc),
 * and wiring this here rather than as a bean keeps a test that only builds the
 * {@code BeanScope} (e.g. {@code ModuleWiringTest}) from also starting a background polling task
 * or touching {@link MinecraftServer#getSchedulerManager()}.
 */
public final class ConfigReloadBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigReloadBootstrap.class);

    private static final String INTERVAL_SECONDS_KEY = "titan.config.reload.intervalSeconds";
    private static final int DEFAULT_INTERVAL_SECONDS = 10;
    private static final String ACTIVE_PROFILES_KEY = "avaje.profiles";
    private static final String CONFIG_FILE_PROPERTY = "config.file";
    private static final String CONFIG_FILE_ENV = "CONFIG_FILE";

    private ConfigReloadBootstrap() {
    }

    /**
     * @param moduleRegistry the lobby's module registry - {@link ModuleRestarterAdapter} delegates
     *                       every restart to it
     * @return a fully wired {@link ConfigReloader}, ready to be triggered by the reload command;
     *         if polling is enabled ({@code titan.config.reload.intervalSeconds} &gt; 0), a
     *         repeating scheduler task that polls the configuration files and triggers it has
     *         already been scheduled
     */
    public static ConfigReloader install(ModuleRegistry moduleRegistry) {
        Objects.requireNonNull(moduleRegistry, "moduleRegistry");

        ConfigSnapshotSource source = new AvajeConfigSnapshotSource();
        LiveConfig liveConfig = new AvajeLiveConfig();
        ModuleRestarter restarter = new ModuleRestarterAdapter(moduleRegistry);
        ExecutorService workerExecutor = Executors.newVirtualThreadPerTaskExecutor();
        // Scheduler extends Executor and its own execute(Runnable) proxies to
        // scheduleNextTick(Runnable) - so the scheduler manager itself is the tick executor, with
        // no adapter of our own needed.
        Executor tickExecutor = MinecraftServer.getSchedulerManager();

        ConfigReloader reloader = new ConfigReloader(source, liveConfig, restarter, workerExecutor, tickExecutor);
        MinecraftServer.getSchedulerManager().buildShutdownTask(workerExecutor::shutdown);

        scheduleFileWatcher(reloader);
        return reloader;
    }

    private static void scheduleFileWatcher(ConfigReloader reloader) {
        int intervalSeconds = Config.getInt(INTERVAL_SECONDS_KEY, DEFAULT_INTERVAL_SECONDS);
        if (!ConfigFileWatcher.shouldSchedule(intervalSeconds)) {
            LOGGER.debug("Configuration file polling is disabled ({} <= 0)", intervalSeconds);
            return;
        }

        List<String> activeProfiles = Config.asConfiguration().list().of(ACTIVE_PROFILES_KEY);
        List<Path> paths = ConfigFileWatcher.pathsFor(Path.of(""), activeProfiles, configFilePath());
        ConfigFileWatcher watcher = new ConfigFileWatcher(paths, ConfigReloadBootstrap::stamp, reloader::reload);
        MinecraftServer.getSchedulerManager().buildTask(watcher::check).repeat(TaskSchedule.seconds(intervalSeconds)).schedule();
    }

    /**
     * @return the file named by {@code CONFIG_FILE}/{@code config.file} - the same override avaje-
     *         config's own {@code InitialLoader} resolves for the initial load (system property
     *         wins over the environment variable) - or {@code null} if neither is set
     */
    private static @Nullable Path configFilePath() {
        String configFile = System.getProperty(CONFIG_FILE_PROPERTY, System.getenv(CONFIG_FILE_ENV));
        return configFile != null ? Path.of(configFile) : null;
    }

    private static FileStamp stamp(Path path) {
        try {
            if (!Files.exists(path)) {
                return FileStamp.MISSING;
            }
            return new FileStamp(true, Files.getLastModifiedTime(path).toMillis(), Files.size(path));
        } catch (IOException exception) {
            LOGGER.debug("Could not stamp configuration file {}, treating it as missing", path, exception);
            return FileStamp.MISSING;
        }
    }
}
