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
package net.onelitefeather.titan.app.bootstrap;

import io.avaje.config.Config;
import java.util.List;
import net.onelitefeather.titan.app.Titan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the single lifecycle line {@link Titan#Titan()} emits once the
 * {@code io.avaje.config.Config}
 * facade has been initialised.
 *
 * <p>Pulled out on its own, mirroring {@link ModuleStartupLog}, so the log line can be
 * unit-tested with a captured appender.
 */
public final class ConfigurationStartupLog {

    /**
     * The {@code avaje-config} key holding the currently active configuration profiles - the same
     * key {@code avaje-config} itself populates from {@code AVAJE_PROFILES}/
     * {@code -Davaje.profiles} (see
     * {@code openspec/changes/standardized-config-profiles/design.md}, decision 6). Shared with
     * {@code ConfigReloadBootstrap}, which reads it for the same reason ({@code
     * ConfigFileWatcher.pathsFor}'s profile-specific files), so the literal exists exactly once.
     */
    public static final String ACTIVE_PROFILES_KEY = "avaje.profiles";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationStartupLog.class);

    private ConfigurationStartupLog() {
    }

    /**
     * Logs the currently active configuration profiles - {@link #ACTIVE_PROFILES_KEY} - as a
     * single parameterised INFO line, even when no profile is active (an empty list). Reads
     * {@link Config#asConfiguration()} itself (see
     * {@code openspec/changes/avaje-config-facade/design.md}, decision 1) rather than taking the
     * already-built instance as a parameter, since {@link Titan#Titan()} has nothing else to build
     * it for any more.
     */
    public static void activeProfiles() {
        List<String> profiles = Config.asConfiguration().list().of(ACTIVE_PROFILES_KEY);
        LOGGER.info("Active configuration profiles: {}", profiles);
    }
}
