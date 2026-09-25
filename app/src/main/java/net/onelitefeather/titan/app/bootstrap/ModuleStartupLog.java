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

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the single lifecycle line {@code Titan} emits once every lobby module has been enabled.
 *
 * <p>Pulled out of {@link net.onelitefeather.titan.app.Titan} on its own so the log line can be
 * unit-tested with a captured appender without booting a Minestom server - see
 * {@code openspec/changes/avaje-dependency-injection/design.md}, decision 5.
 */
public final class ModuleStartupLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleStartupLog.class);

    private ModuleStartupLog() {
    }

    /**
     * Logs the order every lobby module was just enabled in, as a single parameterised INFO line.
     *
     * @param moduleIds the enabled modules' ids, in the order they were started
     */
    public static void enabledInOrder(List<String> moduleIds) {
        LOGGER.info("Lobby modules enabled in order: {}", moduleIds);
    }
}
