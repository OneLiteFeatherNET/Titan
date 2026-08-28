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
import java.util.function.Consumer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.trait.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Error reporting for the Titan server processes.
 *
 * <p>Two problems are solved here, and they are related. Minestom's default
 * {@link net.minestom.server.exception.ExceptionManager ExceptionManager} handler is
 * {@code Throwable::printStackTrace} - every exception thrown inside an event listener or a tick
 * went to {@code System.err} unformatted, past SLF4J entirely. And because no SLF4J binding was
 * ever
 * declared, the shipped fat jars answered every log call with "No SLF4J providers were found" and
 * dropped it. Together that meant a crashing listener left nothing behind but a bare stack trace on
 * the service's stdout.
 *
 * <p>{@link #installExceptionHandler()} routes those exceptions through SLF4J instead, and
 * {@code logback.xml} attaches Sentry's appender to the root logger. Sentry therefore has exactly
 * one way in - an {@code ERROR} log record - rather than a second, parallel reporting path that
 * would have to be kept in sync and would double-report every event.
 *
 * <h2>Player attribution</h2>
 *
 * <p>{@link net.minestom.server.event.EventNodeImpl EventNodeImpl} catches whatever a listener
 * throws and hands it to the exception manager one frame up, on the same thread. {@link #guard}
 * sits
 * inside that frame: it records who the failing event belonged to and rethrows, so the handler can
 * tag the log record - and with it the Sentry event - with the player's UUID and name.
 *
 * <p>The recording happens in a {@code catch} block, never on the healthy path. A listener that
 * returns normally pays for an entered {@code try} and nothing else, which matters because the
 * guarded listeners include {@code PlayerMoveEvent} and {@code PlayerPacketEvent}.
 *
 * <h2>Sentry is optional</h2>
 *
 * <p>Without {@value #DSN_ENVIRONMENT_VARIABLE} in the environment {@link Sentry#init} is never
 * called, so nothing is installed and the process behaves exactly as it does today - the state an
 * operator without a Sentry instance is already in. The same jar serves both.
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

    /**
     * Set by {@link #guard} on the failure path and consumed by {@link #handleException}. Both run
     * on the same thread within one dispatch, so a plain thread local carries the value across the
     * rethrow without touching the healthy path.
     */
    private static final ThreadLocal<PlayerIdentity> FAILING_PLAYER = new ThreadLocal<>();

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

    static void handleException(Throwable throwable) {
        PlayerIdentity identity = consumeFailingPlayer();
        if (identity == null) {
            LOGGER.error("Unhandled exception", throwable);
            return;
        }
        try (MDC.MDCCloseable ignoredUuid = MDC.putCloseable(PLAYER_UUID_KEY, identity.uuid()); MDC.MDCCloseable ignoredName = MDC.putCloseable(PLAYER_NAME_KEY, identity.name())) {
            LOGGER.error("Unhandled exception while handling an event for {}", identity.name(), throwable);
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
