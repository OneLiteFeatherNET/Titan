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
import java.util.Objects;
import java.util.function.Consumer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.trait.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Routes Minestom's uncaught exceptions through SLF4J so they reach both the console and, via the
 * resulting {@code ERROR} log record, Sentry's appender - Sentry's only way in, so nothing
 * double-reports. {@link #guard} attaches the failing player and module to that record.
 */
public final class TitanObservability {

    private static final Logger LOGGER = LoggerFactory.getLogger(TitanObservability.class);

    /** Sentry connection string. Absent or blank disables reporting entirely. */
    public static final String DSN_ENVIRONMENT_VARIABLE = "TITAN_SENTRY_DSN";

    /** Deployment name Sentry groups issues by ({@code production}, {@code beta}, ...). */
    public static final String ENVIRONMENT_ENVIRONMENT_VARIABLE = "TITAN_SENTRY_ENVIRONMENT";

    private static final String DEFAULT_ENVIRONMENT = "unknown";
    private static final String DEVELOPMENT_RELEASE = "dev";

    static final String PLAYER_UUID_KEY = "player.uuid";
    static final String PLAYER_NAME_KEY = "player.name";
    static final String MODULE_KEY = "module";

    // guard sets these on failure, handleException reads and clears them - same thread, same dispatch.
    private static final ThreadLocal<PlayerIdentity> FAILING_PLAYER = new ThreadLocal<>();
    private static final ThreadLocal<String> FAILING_MODULE = new ThreadLocal<>();

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
            // Player identity is attached deliberately in handleException instead of via PII defaults.
            options.setSendDefaultPii(false);
        });
        LOGGER.info("Sentry reporting enabled - release {}, environment {}", release, environment);
    }

    /**
     * Replaces Minestom's default {@code Throwable::printStackTrace} handler with
     * {@link #handleException}.
     */
    public static void installExceptionHandler() {
        MinecraftServer.getExceptionManager().setExceptionHandler(TitanObservability::handleException);
    }

    /** Wraps a listener so a failure records which player the event belonged to. */
    public static <T extends Event> Consumer<T> guard(Consumer<T> listener) {
        return event -> {
            try {
                listener.accept(event);
            } catch (Throwable throwable) {
                FAILING_PLAYER.set(identityOf(event));
                throw throwable;
            }
        };
    }

    /**
     * Like {@link #guard(Consumer)}, but also records the module id and puts it in the SLF4J MDC
     * for the whole call, so the module's own logging carries it too.
     */
    public static <T extends Event> Consumer<T> guard(String moduleId, Consumer<T> listener) {
        Objects.requireNonNull(moduleId, "moduleId");
        Consumer<T> guarded = guard(listener);
        return event -> {
            try (MDC.MDCCloseable ignoredModule = MDC.putCloseable(MODULE_KEY, moduleId)) {
                guarded.accept(event);
            } catch (Throwable throwable) {
                FAILING_MODULE.set(moduleId);
                throw throwable;
            }
        };
    }

    static PlayerIdentity consumeFailingPlayer() {
        PlayerIdentity identity = FAILING_PLAYER.get();
        FAILING_PLAYER.remove();
        return identity;
    }

    static String consumeFailingModule() {
        String moduleId = FAILING_MODULE.get();
        FAILING_MODULE.remove();
        return moduleId;
    }

    static void handleException(Throwable throwable) {
        PlayerIdentity identity = consumeFailingPlayer();
        String moduleId = consumeFailingModule();
        if (identity == null && moduleId == null) {
            LOGGER.error("Unhandled exception", throwable);
            return;
        }
        try (MDC.MDCCloseable ignoredModule = moduleId == null ? null : MDC.putCloseable(MODULE_KEY, moduleId); MDC.MDCCloseable ignoredUuid = identity == null ? null : MDC.putCloseable(PLAYER_UUID_KEY, identity.uuid()); MDC.MDCCloseable ignoredName = identity == null ? null : MDC.putCloseable(PLAYER_NAME_KEY, identity.name())) {
            if (moduleId != null && identity != null) {
                LOGGER.error("Unhandled exception in module {} while handling an event for {}", moduleId, identity.name(), throwable);
            } else if (moduleId != null) {
                LOGGER.error("Unhandled exception in module {}", moduleId, throwable);
            } else {
                LOGGER.error("Unhandled exception while handling an event for {}", identity.name(), throwable);
            }
        }
    }

    static PlayerIdentity identityOf(Event event) {
        if (!(event instanceof PlayerEvent playerEvent)) {
            return null;
        }
        Player player = playerEvent.getPlayer();
        return new PlayerIdentity(player.getUuid().toString(), player.getUsername());
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

    /**
     * The player an exception is attributed to. Strings, so nothing keeps a {@link Player} alive.
     */
    record PlayerIdentity(String uuid, String name) {
    }
}
