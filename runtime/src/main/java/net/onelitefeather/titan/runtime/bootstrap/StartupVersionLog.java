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
package net.onelitefeather.titan.runtime.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the single line that names the running build, so a Loki query shows which deploy a server
 * runs.
 */
public final class StartupVersionLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupVersionLog.class);

    private StartupVersionLog() {
    }

    public static void log(TitanBuild build) {
        LOGGER.info("Titan {} (commit {}) starting", build.version(), build.commit());
    }
}
