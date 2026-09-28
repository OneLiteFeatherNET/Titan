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
 * {@code io.avaje.config.Config} facade is initialised, pulled out on its own so it can be
 * unit-tested with a captured appender.
 */
public final class ConfigurationStartupLog {

    /**
     * The {@code avaje-config} key populated from {@code AVAJE_PROFILES}/{@code -Davaje.profiles}.
     */
    public static final String ACTIVE_PROFILES_KEY = "avaje.profiles";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationStartupLog.class);

    private ConfigurationStartupLog() {
    }

    /** Logs {@link #ACTIVE_PROFILES_KEY} as a single INFO line, even when the list is empty. */
    public static void activeProfiles() {
        List<String> profiles = Config.asConfiguration().list().of(ACTIVE_PROFILES_KEY);
        LOGGER.info("Active configuration profiles: {}", profiles);
    }
}
