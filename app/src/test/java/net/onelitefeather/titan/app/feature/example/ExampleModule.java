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
package net.onelitefeather.titan.app.feature.example;

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
import net.onelitefeather.titan.app.module.FeatureNode;

/**
 * Template for a new lobby feature, referenced end to end from {@code docs/lobby-modules.md}. It
 * touches every extension point a typical feature needs: reading and validating a configuration
 * value at the edge of {@link #start()}, a hotbar item
 * ({@link ExampleGreetingItems#greetingToken(ExampleModule)}), and a listener that cleans up
 * per-player state on disconnect. The pure decision logic lives in {@link ExampleGreetingRule},
 * the pure validation in {@link ExampleGreetingSettings}, and the stateful cooldown tracking in
 * {@link ExampleGreetingTracker} - each unit-tested on its own.
 *
 * <p>This template has no section of its own in the shipped {@code application.yaml}, so
 * {@link #start()} validates {@link #DEFAULT_GREETING}/{@link #DEFAULT_COOLDOWN_MILLIS} directly
 * instead of reading them from {@code Config} - see {@link #start()}'s Javadoc for the snippet a
 * real feature would write in its place.
 *
 * <p>Test-only on purpose: it is a copyable starting point for a real feature, not a feature
 * itself. It carries {@code @Singleton} anyway, so it stays a <em>correct</em> copy template, but
 * Avaje Inject's annotation processor does not run for test sources, so it is never discovered as
 * a lobby feature. Its {@link #EVENT_PRIORITY} is deliberately past the highest real feature
 * (elytra, 700), so a real feature picks its own, still-unused value from the priority table in
 * {@code docs/lobby-modules.md} instead of copying this one.
 *
 * <p>Behaviour: using {@link ExampleGreetingItems#greetingToken(ExampleModule)} sends the player
 * {@link #DEFAULT_GREETING} with their name substituted in, unless they are still within
 * {@link #DEFAULT_COOLDOWN_MILLIS} of their last greeting, in which case they get
 * {@link ExampleItems#ON_COOLDOWN} instead. A disconnecting player's cooldown is forgotten, so
 * rejoining does not inherit it.
 */
@Singleton
final class ExampleModule {

    /**
     * This feature's position among its sibling {@link FeatureNode}s - deliberately past the
     * highest real feature (elytra, 700); see the class Javadoc.
     */
    static final int EVENT_PRIORITY = 800;

    private static final String ID = "example";

    /**
     * This template's default greeting - validated by {@link #start()} the same way a real feature
     * validates a value it actually read from {@code Config}. See the class Javadoc for why this
     * template has no section of its own to read from.
     */
    static final String DEFAULT_GREETING = "Welcome to the lobby, %s!";

    /**
     * This template's default cooldown in milliseconds - validated by {@link #start()} the same way
     * a real feature validates a value it actually read from {@code Config}. See the class Javadoc
     * for why this template has no section of its own to read from.
     */
    static final long DEFAULT_COOLDOWN_MILLIS = 5_000;

    private final EventNode<Event> titan;
    private final Clock clock;
    private FeatureNode node;
    private ExampleGreetingTracker tracker;

    /**
     * @param titan the shared event node this feature's own node attaches under
     */
    ExampleModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan) {
        this(titan, Clock.systemUTC());
    }

    /**
     * Creates a feature backed by {@code clock}, so a test can control what "now" is instead of the
     * feature depending on {@link System#currentTimeMillis()} - the same pattern {@code
     * TickleModule} uses. Carries {@code @Inject} because this class has more than one constructor
     * - Avaje Inject would otherwise not know which one to use, were this feature ever discovered
     * (see the class Javadoc for why it is not).
     *
     * @param titan the shared event node this feature's own node attaches under
     * @param clock the clock to read the current time from
     */
    @Inject
    ExampleModule(@Named(FeatureNode.TITAN_NODE) EventNode<Event> titan, Clock clock) {
        this.titan = Objects.requireNonNull(titan, "titan");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Reads and validates this feature's configuration at the edge, then attaches its own event
     * node with its one listener.
     *
     * <p>A real feature reads its own section here, e.g.
     *
     * <pre>{@code
     * String greeting = ExampleGreetingSettings.greeting(Config.get(ExampleGreetingSettings.GREETING_KEY));
     * long cooldownMillis =
     *         Config.getAs(ExampleGreetingSettings.COOLDOWN_KEY, ExampleGreetingSettings::cooldownMillis);
     * }</pre>
     *
     * <p>This template has no section of its own in the shipped {@code application.yaml} (see the
     * class Javadoc), so it validates {@link #DEFAULT_GREETING}/{@link #DEFAULT_COOLDOWN_MILLIS}
     * directly instead - a {@code Config.get(...)} call for a key that does not exist would fail
     * the start with "Missing required configuration parameter".
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
     * Greets {@code player} - or tells them they are on cooldown - through the shared
     * {@link ExampleGreetingTracker}. Called by {@link ExampleGreetingItems}' use handler once a
     * player uses the greeting token.
     *
     * @param player the player to greet
     */
    void greet(Player player) {
        this.tracker.greet(player).ifPresentOrElse(player::sendMessage, () -> player.sendMessage(ExampleItems.ON_COOLDOWN));
    }
}
