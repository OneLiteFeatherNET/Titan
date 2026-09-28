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
package net.onelitefeather.titan.common.observability;

import io.sentry.Sentry;
import net.minestom.server.MinecraftServer;
import net.onelitefeather.titan.core.module.FeatureNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts Sentry reporting and routes Minestom's uncaught exceptions through SLF4J so they reach
 * both the console and, via the resulting {@code ERROR} log record, Sentry's appender - Sentry's
 * only way in, so nothing double-reports. The player/feature attribution itself lives in
 * {@code core}'s {@code FeatureNode}/{@code ListenerGuard}, which every feature's listener already
 * runs through; this class only wires Minestom's exception handler to
 * {@link FeatureNode#reportUnhandledException(Throwable)}.
 */
public final class TitanObservability {

    private static final Logger LOGGER = LoggerFactory.getLogger(TitanObservability.class);

    /** Sentry connection string. Absent or blank disables reporting entirely. */
    public static final String DSN_ENVIRONMENT_VARIABLE = "TITAN_SENTRY_DSN";

    /** Deployment name Sentry groups issues by ({@code production}, {@code beta}, ...). */
    public static final String ENVIRONMENT_ENVIRONMENT_VARIABLE = "TITAN_SENTRY_ENVIRONMENT";

    private static final String DEFAULT_ENVIRONMENT = "unknown";
    private static final String DEVELOPMENT_RELEASE = "dev";

    private TitanObservability() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Initialises Sentry when {@value #DSN_ENVIRONMENT_VARIABLE} is set, otherwise a no-op. Call
     * this as early in {@code main} as possible - anything logged before it is never reported.
     */
    public static void bootstrap() {
        bootstrap(release());
    }

    static void bootstrap(String release) {
        String dsn = System.getenv(DSN_ENVIRONMENT_VARIABLE);
        if (dsn == null || dsn.isBlank()) {
            LOGGER.info("Sentry reporting disabled - {} is not set", DSN_ENVIRONMENT_VARIABLE);
            return;
        }
        String environment = environment();
        Sentry.init(options -> {
            options.setDsn(dsn);
            options.setRelease(release);
            options.setEnvironment(environment);
            // Player identity is attached deliberately in ListenerGuard instead of via PII defaults.
            options.setSendDefaultPii(false);
        });
        LOGGER.info("Sentry reporting enabled - release {}, environment {}", release, environment);
    }

    /**
     * Replaces Minestom's default {@code Throwable::printStackTrace} handler with
     * {@link FeatureNode#reportUnhandledException(Throwable)}.
     */
    public static void installExceptionHandler() {
        MinecraftServer.getExceptionManager().setExceptionHandler(FeatureNode::reportUnhandledException);
    }

    // Falls back to dev when not loaded from a jar (tests, IDE runs).
    static String release() {
        String version = TitanObservability.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? DEVELOPMENT_RELEASE : version;
    }

    private static String environment() {
        String environment = System.getenv(ENVIRONMENT_ENVIRONMENT_VARIABLE);
        return environment == null || environment.isBlank() ? DEFAULT_ENVIRONMENT : environment;
    }
}
