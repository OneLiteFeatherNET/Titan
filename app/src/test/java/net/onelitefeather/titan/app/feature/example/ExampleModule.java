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
 * typical feature needs: its own {@code app.json} section ({@link ExampleConfig}), a hotbar item
 * ({@link ExampleItems#GREETING_TOKEN}), a command, and a listener that cleans up per-player state
 * on disconnect. The pure decision logic lives in {@link ExampleGreetingRule}, unit-tested on its
 * own; the stateful cooldown tracking lives in {@link ExampleGreetingTracker}.
 *
 * <p>Test-only on purpose (see {@code app/src/test/.../app/feature/example}, not
 * {@code app/src/main}): it is a copyable starting point for a real feature, not a feature itself,
 * so it is never wired into {@code Titan}'s module list.
 *
 * <p>Behaviour: using {@link ExampleItems#GREETING_TOKEN} - or running the {@code
 * titan-example-greet} command - sends the player {@link ExampleConfig#greeting()} with their name
 * substituted in, unless they are still within {@link ExampleConfig#cooldownMillis()} of their last
 * greeting, in which case they get {@link ExampleItems#ON_COOLDOWN} instead. A disconnecting
 * player's cooldown is forgotten, so rejoining does not inherit it.
 */
public final class ExampleModule implements LobbyModule {

    /**
     * The hotbar slot {@link ExampleItems#GREETING_TOKEN} is pinned to - deliberately the last
     * slot, one no real feature module claims, so this template never collides with one in a test
     * that happens to start both.
     */
    static final int GREETING_TOKEN_SLOT = ItemSlot.MAX_HOTBAR_SLOT;

    /** This module's own command name, registered through {@link ModuleContext#commands()}. */
    static final String COMMAND_NAME = "titan-example-greet";

    private final Clock clock;

    /** Creates a module backed by the system clock. */
    public ExampleModule() {
        this(Clock.systemUTC());
    }

    /**
     * Creates a module backed by {@code clock}, so a test can control what "now" is instead of the
     * module depending on {@link System#currentTimeMillis()} - the same pattern {@code
     * TickleModule} uses.
     *
     * @param clock the clock to read the current time from
     */
    public ExampleModule(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String id() {
        return "example";
    }

    @Override
    public void enable(ModuleContext context) {
        // config(): read this module's own app.json section once, up front - never later, see
        // ModuleContext#config.
        ExampleConfig config = context.config(ExampleConfig.class, ExampleConfig.DEFAULTS);
        ExampleGreetingTracker tracker = new ExampleGreetingTracker(this.clock, config);

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
