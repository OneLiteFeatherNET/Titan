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

import io.avaje.config.Configuration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the single lifecycle line {@link PlatformBeans#configuration()} emits once its {@link
 * Configuration} bean has been built.
 *
 * <p>Pulled out on its own, mirroring {@link ModuleStartupLog}, so the log line can be
 * unit-tested with a captured appender and a {@link Configuration} built directly from a {@link
 * java.util.Map} (design.md decision 1's spike result) - never from the real environment's
 * {@code AVAJE_PROFILES}, which would make a test depend on whatever happens to be set outside
 * the test itself (F.I.R.S.T. - Repeatable).
 */
public final class ConfigurationStartupLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationStartupLog.class);

    private ConfigurationStartupLog() {
    }

    /**
     * Logs the currently active configuration profiles - the {@code avaje.profiles} key {@code
     * avaje-config} itself populates from {@code AVAJE_PROFILES}/{@code -Davaje.profiles} (see
     * {@code openspec/changes/standardized-config-profiles/design.md}, decision 6 - as a single
     * parameterised INFO line, even when no profile is active (an empty list).
     *
     * @param configuration the built {@link Configuration} to read the active profiles from
     */
    public static void activeProfiles(Configuration configuration) {
        List<String> profiles = configuration.list().of("avaje.profiles");
        LOGGER.info("Active configuration profiles: {}", profiles);
    }
}
