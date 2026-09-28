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
 * Routes Minestom's uncaught exceptions through SLF4J, and, when a Sentry DSN is configured, on to
 * Sentry via the resulting {@code ERROR} log record - the appender is Sentry's only way in, so
 * nothing double-reports.
 *
 * <p>{@link #guard} records which player and, via {@link #guard(String, Consumer)}, which module a
 * failing listener belonged to. The module id is also placed in the SLF4J MDC for the whole call,
 * healthy or not, so both the module's own logging and the final {@link #handleException} record
 * carry it.
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

    /**
     * Set by {@link #guard} on the failure path and consumed by {@link #handleException}. Both run
     * on the same thread within one dispatch, so a plain thread local carries the value across the
     * rethrow without touching the healthy path.
     */
    private static final ThreadLocal<PlayerIdentity> FAILING_PLAYER = new ThreadLocal<>();

    /**
     * Set by {@link #guard(String, Consumer)} on the failure path and consumed by
     * {@link #handleException}, mirroring {@link #FAILING_PLAYER}.
     */
    private static final ThreadLocal<String> FAILING_MODULE = new ThreadLocal<>();

    private TitanObservability() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Initialises Sentry when {@value #DSN_ENVIRONMENT_VARIABLE} is set, and does nothing
     * otherwise.
     *
     * <p>Call this as early in {@code main} as possible: log records emitted before it - LuckPerms'
     * bootstrap, for instance - are written to the console but not reported.
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
            // The SDK's PII defaults collect request headers and IP addresses, which say nothing
            // useful about a Minestom crash. The player identity that does is attached
            // deliberately in handleException instead.
            options.setSendDefaultPii(false);
        });
        LOGGER.info("Sentry reporting enabled - release {}, environment {}", release, environment);
    }

    /**
     * Replaces Minestom's {@code Throwable::printStackTrace} default with one that logs through
     * SLF4J, so exceptions reach both the console and Sentry's appender.
     */
    public static void installExceptionHandler() {
        MinecraftServer.getExceptionManager().setExceptionHandler(TitanObservability::handleException);
    }

    /**
     * Wraps a listener so a failure records which player the event belonged to.
     *
     * @param listener the listener to wrap
     * @param <T>      the event type
     * @return a listener that behaves identically but leaves player context behind when it throws
     */
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
     * Wraps a listener so a failure records which module the listener belongs to, in addition to
     * everything {@link #guard(Consumer)} already records for the player. The module id is also
     * placed in the SLF4J MDC ({@value #MODULE_KEY}) for the whole duration of the call, healthy or
     * not, so a module's own log statements carry it too.
     *
     * @param moduleId the id of the module {@code listener} belongs to
     * @param listener the listener to wrap
     * @param <T>      the event type
     * @return a listener that behaves identically but leaves module (and player) context behind
     *         when it throws
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

    /**
     * Returns the identity {@link #guard} recorded for this thread's most recent failure, and
     * clears it. Clearing is unconditional: a stale identity left behind would mis-attribute the
     * next exception this thread reports.
     *
     * @return the player the failing event belonged to, or {@code null} if there was none
     */
    static PlayerIdentity consumeFailingPlayer() {
        PlayerIdentity identity = FAILING_PLAYER.get();
        FAILING_PLAYER.remove();
        return identity;
    }

    /**
     * Returns the module id {@link #guard(String, Consumer)} recorded for this thread's most recent
     * failure, and clears it, mirroring {@link #consumeFailingPlayer()}.
     *
     * @return the module the failing listener belonged to, or {@code null} if there was none
     */
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

    /**
     * The release reported to Sentry, read from the fat jar's {@code Implementation-Version}
     * manifest attribute. Returns {@value #DEVELOPMENT_RELEASE} when the classes are not loaded
     * from a jar, which is the case in tests and when running from an IDE.
     */
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
