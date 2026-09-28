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
package net.onelitefeather.titan.runtime.feature.example;

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.util.Objects;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.titan.core.module.FeatureNode;

/**
 * Template for a new lobby feature (see {@code docs/lobby-modules.md}): config validation at the
 * edge of {@link #start()}, a hotbar item, and disconnect cleanup.
 *
 * <p>Decision logic, validation and stateful tracking live in separate, unit-tested classes:
 * {@link ExampleGreetingRule}, {@link ExampleGreetingSettings}, {@link ExampleGreetingTracker}.
 *
 * <p>Test-only: a copyable starting point, never discovered as a real feature since Avaje's
 * annotation processor does not run for test sources.
 *
 * <p>{@link #EVENT_PRIORITY} sits past the highest real feature so a real feature picks its own,
 * still-unused value from {@code docs/lobby-modules.md}'s priority table.
 */
@Singleton
final class ExampleModule {

    /** Sits past the highest real feature (elytra, 700); see the class Javadoc. */
    static final int EVENT_PRIORITY = 800;

    private static final String ID = "example";

    /**
     * Validated by {@link #start()} the same way a real feature validates a {@code Config} value.
     */
    static final String DEFAULT_GREETING = "Welcome to the lobby, %s!";

    /**
     * Validated by {@link #start()} the same way a real feature validates a {@code Config} value.
     */
    static final long DEFAULT_COOLDOWN_MILLIS = 5_000;

    private final EventNode<Event> titan;
    private final Clock clock;
    private FeatureNode node;
    private ExampleGreetingTracker tracker;

    ExampleModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan) {
        this(titan, Clock.systemUTC());
    }

    /**
     * Takes {@link Clock} so a test can fix "now"; {@code @Inject} disambiguates the two
     * constructors.
     */
    @Inject
    ExampleModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Clock clock) {
        this.titan = Objects.requireNonNull(titan, "titan");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Reads and validates this feature's configuration at the edge, then attaches its event node.
     *
     * <p>A real feature reads its own section here, e.g.
     *
     * <pre>{@code
     * String greeting = ExampleGreetingSettings.greeting(Config.get(ExampleGreetingSettings.GREETING_KEY));
     * long cooldownMillis =
     *         Config.getAs(ExampleGreetingSettings.COOLDOWN_KEY, ExampleGreetingSettings::cooldownMillis);
     * }</pre>
     *
     * <p>This template has no section to read, so it validates its hardcoded defaults instead.
     */
    @PostConstruct
    void start() {
        String greeting = ExampleGreetingSettings.greeting(DEFAULT_GREETING);
        long cooldownMillis = ExampleGreetingSettings.cooldownMillis(String.valueOf(DEFAULT_COOLDOWN_MILLIS));
        this.tracker = new ExampleGreetingTracker(this.clock, greeting, cooldownMillis);

        this.node = FeatureNode.attach(this.titan, ID, EVENT_PRIORITY).on(PlayerDisconnectEvent.class, event -> this.tracker.clear(event.getPlayer().getUuid()));
    }

    /** Detaches this feature's own event node, so its listener above runs no more. */
    @PreDestroy
    void stop() {
        this.node.close();
    }

    /**
     * Greets {@code player} through the shared {@link ExampleGreetingTracker}, or reports the
     * cooldown.
     */
    void greet(Player player) {
        this.tracker.greet(player).ifPresentOrElse(player::sendMessage, () -> player.sendMessage(ExampleItems.ON_COOLDOWN));
    }
}
