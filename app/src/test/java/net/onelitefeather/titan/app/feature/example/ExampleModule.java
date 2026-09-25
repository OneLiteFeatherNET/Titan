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

import io.avaje.inject.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;

/**
 * Template for a new lobby feature module, referenced end to end from
 * {@code docs/lobby-modules.md}. It is deliberately small but touches every extension point a
 * typical feature needs: reading and validating a configuration value at the edge of
 * {@link #enable}, a hotbar item ({@link ExampleItems#GREETING_TOKEN}), a command, and a listener
 * that cleans up per-player state on disconnect. The pure decision logic lives in
 * {@link ExampleGreetingRule}, unit-tested on its own; the pure validation lives in
 * {@link ExampleGreetingSettings}, also unit-tested on its own; the stateful cooldown tracking
 * lives in {@link ExampleGreetingTracker}.
 *
 * <p>This template has no section of its own in the shipped {@code application.yaml}: adding one
 * just for a copy-and-delete template would be a key nothing in production ever reads, and
 * design.md, decision 5 forbids a test from adding a key just to make a module's own tests pass. So
 * {@link #enable} validates {@link #DEFAULT_GREETING}/{@link #DEFAULT_COOLDOWN_MILLIS} directly
 * instead of reading them from {@code Config} - see {@link #enable}'s Javadoc for the snippet a
 * real
 * module would write in its place.
 *
 * <p>Test-only on purpose (see {@code app/src/test/.../app/feature/example}, not
 * {@code app/src/main}): it is a copyable starting point for a real feature, not a feature itself.
 * It carries {@code @Singleton}/{@code @Priority} anyway, so it stays a <em>correct</em> copy
 * template - but it is still never discovered as a lobby module, because Avaje Inject's annotation
 * processor does not run for test sources (no {@code testAnnotationProcessor}, see
 * {@code app/build.gradle.kts}); {@code ModuleWiringTest} keeps finding exactly the seven modules
 * under {@code app/src/main}, not this one. Its {@code @Priority(800)} is deliberately past the
 * highest real module (elytra, 700), so nobody mistakes it for a real slot in the priority table
 * in {@code docs/lobby-modules.md} - a real module picks its own, still-unused value from that
 * table instead of copying this one.
 *
 * <p>Behaviour: using {@link ExampleItems#GREETING_TOKEN} - or running the {@code
 * titan-example-greet} command - sends the player {@link #DEFAULT_GREETING} with their name
 * substituted in, unless they are still within {@link #DEFAULT_COOLDOWN_MILLIS} of their last
 * greeting, in which case they get {@link ExampleItems#ON_COOLDOWN} instead. A disconnecting
 * player's cooldown is forgotten, so rejoining does not inherit it.
 */
@Singleton
@Priority(800)
public final class ExampleModule implements LobbyModule {

    /**
     * The hotbar slot {@link ExampleItems#GREETING_TOKEN} is pinned to - deliberately the last
     * slot, one no real feature module claims, so this template never collides with one in a test
     * that happens to start both.
     */
    static final int GREETING_TOKEN_SLOT = ItemSlot.MAX_HOTBAR_SLOT;

    /** This module's own command name, registered through {@link ModuleContext#commands()}. */
    static final String COMMAND_NAME = "titan-example-greet";

    /**
     * This template's default greeting - validated by {@link #enable} the same way a real module
     * validates a value it actually read from {@code Config}. See the class Javadoc for why this
     * template has no section of its own to read from.
     */
    static final String DEFAULT_GREETING = "Welcome to the lobby, %s!";

    /**
     * This template's default cooldown in milliseconds - validated by {@link #enable} the same way
     * a real module validates a value it actually read from {@code Config}. See the class Javadoc
     * for why this template has no section of its own to read from.
     */
    static final long DEFAULT_COOLDOWN_MILLIS = 5_000;

    private final Clock clock;

    /** Creates a module backed by the system clock. */
    public ExampleModule() {
        this(Clock.systemUTC());
    }

    /**
     * Creates a module backed by {@code clock}, so a test can control what "now" is instead of the
     * module depending on {@link System#currentTimeMillis()} - the same pattern {@code
     * TickleModule} uses. Carries {@code @Inject} because this class has more than one
     * constructor - Avaje Inject would otherwise not know which one to use, were this module ever
     * discovered (see the class Javadoc for why it is not).
     *
     * @param clock the clock to read the current time from
     */
    @Inject
    public ExampleModule(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String id() {
        return "example";
    }

    /**
     * Reads and validates this module's configuration at the edge, then wires up its extension
     * points.
     *
     * <p>Lesen am Rand (design.md, decision 3): a real module reads its own section here, e.g.
     *
     * <pre>{@code
     * String greeting = ExampleGreetingSettings.greeting(Config.get(ExampleGreetingSettings.GREETING_KEY));
     * long cooldownMillis =
     *         ExampleGreetingSettings.cooldownMillis(Config.getAs(ExampleGreetingSettings.COOLDOWN_KEY, Long::parseLong));
     * }</pre>
     *
     * <p>This template has no section of its own in the shipped {@code application.yaml} (see the
     * class Javadoc), so it validates {@link #DEFAULT_GREETING}/{@link #DEFAULT_COOLDOWN_MILLIS}
     * directly instead - a {@code Config.get(...)} call for a key that does not exist would fail
     * the start with "Missing required configuration parameter", and no test may add a key just to
     * avoid that (design.md, decision 5).
     */
    @Override
    public void enable(ModuleContext context) {
        String greeting = ExampleGreetingSettings.greeting(DEFAULT_GREETING);
        long cooldownMillis = ExampleGreetingSettings.cooldownMillis(DEFAULT_COOLDOWN_MILLIS);
        ExampleGreetingTracker tracker = new ExampleGreetingTracker(this.clock, greeting, cooldownMillis);

        // items(): register a hotbar item; ItemRegistry stamps it and dispatches its use back to
        // the handler below - see ModuleItems#register.
        context.items().register(new LobbyItem(Key.key("titan:example"), ExampleItems.GREETING_TOKEN, ItemSlot.hotbar(GREETING_TOKEN_SLOT), (player, event) -> greet(player, tracker)));

        // commands(): register a command; ModuleCommands unregisters it again once this module is
        // disabled - see ModuleCommands#register.
        Command command = new Command(COMMAND_NAME);
        command.addSyntax((sender, commandContext) -> {
            if (sender instanceof Player player) {
                greet(player, tracker);
            }
        });
        context.commands().register(command);

        // listen(): a player-scoped cleanup listener, registered up front while enable() runs -
        // never in response to something happening later, see ModuleContext#listen.
        context.listen(PlayerDisconnectEvent.class, event -> tracker.clear(event.getPlayer().getUuid()));
    }

    private static void greet(Player player, ExampleGreetingTracker tracker) {
        tracker.greet(player).ifPresentOrElse(player::sendMessage, () -> player.sendMessage(ExampleItems.ON_COOLDOWN));
    }
}
