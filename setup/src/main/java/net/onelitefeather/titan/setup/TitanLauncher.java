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
package net.onelitefeather.titan.setup;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.titan.common.observability.TitanObservability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TitanLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TitanLauncher.class);

    public static void main(String[] args) {
        // First statement: anything logged before this reaches the console but not Sentry.
        TitanObservability.bootstrap();
        var minecraftServer = MinecraftServer.init();
        // Needs an initialised MinecraftServer, which the line above provides.
        TitanObservability.installExceptionHandler();

        // A syntactically broken application.yaml surfaces as a ConfigException (unchecked) from
        // Titan's constructor - see ConfigurationFactory#initialise() - the same way it does for the
        // lobby (:app, TitanApplication#main). Startup must abort with a clear log line instead of
        // leaving the process half-started on Minestom's already-running threads.
        if (!startCleanly(Titan::instance)) {
            System.exit(1);
            return;
        }

        // CloudNet passes the bind address/port via -Dservice.bind.host /
        // -Dservice.bind.port; fall back to the standalone defaults otherwise.
        String bindHost = System.getProperty("service.bind.host", "0.0.0.0");
        int bindPort = Integer.getInteger("service.bind.port", 25565);
        minecraftServer.start(bindHost, bindPort);
    }

    /**
     * Runs {@code startup}, catching {@link RuntimeException} or {@link Error} and logging exactly
     * once at ERROR instead of letting either escape {@link #main}, mirroring
     * {@code net.onelitefeather.titan.app.TitanApplication#main}'s abort-cleanly behaviour for the
     * lobby. Returns a boolean rather than calling {@link System#exit(int)} itself, so {@link
     * #main} decides the exit code and this method stays unit-testable.
     *
     * @param startup the startup step to run, e.g. {@link Titan#instance()}
     * @return {@code true} if {@code startup} completed without throwing, {@code false} if it was
     *         caught and logged
     */
    static boolean startCleanly(Runnable startup) {
        try {
            startup.run();
            return true;
        } catch (RuntimeException | Error throwable) {
            LOGGER.error("Titan setup failed to start: {}", throwable.getMessage(), throwable);
            return false;
        }
    }
}
