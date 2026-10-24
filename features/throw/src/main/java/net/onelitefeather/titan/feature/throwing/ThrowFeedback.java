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
package net.onelitefeather.titan.feature.throwing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.feature.throwing.ThrowRules.Refusal;

/**
 * Tells the thrower why a pickup was refused. Refusals go to the action bar, never to chat, and at
 * most one per {@link #INTERVAL} per player: a held click repeats the interact every few ticks, so
 * without the limit one held click would fill the chat.
 */
final class ThrowFeedback {

    static final Duration INTERVAL = Duration.ofSeconds(1);

    /** Where a text goes; the module sends to the action bar of the player. */
    @FunctionalInterface
    interface Sink {
        void actionBar(Player player, Component text);
    }

    private final Clock clock;
    private final Sink sink;
    private final Map<UUID, Instant> lastShown = new HashMap<>();

    ThrowFeedback(Clock clock, Sink sink) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.sink = Objects.requireNonNull(sink, "sink must not be null");
    }

    /** Shows the reason to the thrower, unless one was shown less than {@link #INTERVAL} ago. */
    void refused(Player thrower, Refusal refusal) {
        Instant now = this.clock.instant();
        Instant last = this.lastShown.get(thrower.getUuid());
        if (last != null && now.isBefore(last.plus(INTERVAL))) {
            return;
        }
        this.lastShown.put(thrower.getUuid(), now);
        this.sink.actionBar(thrower, ThrowMessages.text(ThrowMessages.refusalKey(refusal), thrower.getLocale()));
    }

    /** Forgets the player; called when they leave, so the map does not grow with every visitor. */
    void forget(UUID player) {
        this.lastShown.remove(player);
    }
}
