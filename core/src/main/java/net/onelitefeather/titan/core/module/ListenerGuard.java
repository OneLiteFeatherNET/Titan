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
package net.onelitefeather.titan.core.module;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import java.util.Objects;
import java.util.function.Consumer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.trait.PlayerEvent;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Wraps a {@link FeatureNode} listener so a failure is attributed to the feature and, if the event
 * carries one, the player - via SLF4J/MDC, so the module's own logging carries it too. Package
 * private: {@link FeatureNode} is the only public entry point, both for registering a guarded
 * listener and, via {@link FeatureNode#reportUnhandledException(Throwable)}, for
 * {@code common}'s {@code TitanObservability} to route Minestom's uncaught-exception handler
 * through it.
 */
final class ListenerGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerGuard.class);

    static final String PLAYER_UUID_KEY = "player.uuid";
    static final String PLAYER_NAME_KEY = "player.name";
    static final String MODULE_KEY = "module";
    static final String FAILURES_METRIC = "titan.listener.failures";

    // guard sets these on failure, handleException reads and clears them - same thread, same dispatch.
    private static final ThreadLocal<PlayerIdentity> FAILING_PLAYER = new ThreadLocal<>();
    private static final ThreadLocal<String> FAILING_MODULE = new ThreadLocal<>();

    private ListenerGuard() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /** Wraps a listener so a failure records which player the event belonged to. */
    static <T extends Event> Consumer<T> guard(Consumer<T> listener) {
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
    static <T extends Event> Consumer<T> guard(String moduleId, Consumer<T> listener) {
        return guard(moduleId, Telemetry.noop(), listener);
    }

    /**
     * Like {@link #guard(String, Consumer)}, and counts every failure as
     * {@code titan.listener.failures{titan.feature}}. The meter comes from the caller, so this
     * class keeps no global telemetry state.
     */
    static <T extends Event> Consumer<T> guard(String moduleId, Telemetry telemetry, Consumer<T> listener) {
        Objects.requireNonNull(moduleId, "moduleId");
        Consumer<T> guarded = guard(listener);
        LongCounter failures = telemetry.meter().counterBuilder(FAILURES_METRIC).setDescription("Exceptions thrown by feature listeners").build();
        Attributes feature = Attributes.of(Telemetry.FEATURE, moduleId);
        return event -> {
            try (MDC.MDCCloseable ignoredModule = MDC.putCloseable(MODULE_KEY, moduleId)) {
                guarded.accept(event);
            } catch (Throwable throwable) {
                FAILING_MODULE.set(moduleId);
                failures.add(1, feature);
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

    /**
     * The player an exception is attributed to. Strings, so nothing keeps a {@link Player} alive.
     */
    record PlayerIdentity(String uuid, String name) {
    }
}
