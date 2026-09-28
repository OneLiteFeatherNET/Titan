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

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

/**
 * Per-player cooldown state for {@link ExampleModule}'s greeting - the stateful half of the
 * {@code example} feature, deferring the rule itself to {@link ExampleGreetingRule}.
 *
 * <p>Takes a {@link Clock} rather than reading the system clock directly, so a test can fix "now"
 * (F.I.R.S.T. - repeatable).
 */
final class ExampleGreetingTracker {

    private final Clock clock;
    private final String greeting;
    private final long cooldownMillis;
    private final Map<UUID, Long> lastGreetedAtMillis = new ConcurrentHashMap<>();

    ExampleGreetingTracker(Clock clock, String greeting, long cooldownMillis) {
        this.clock = clock;
        this.greeting = greeting;
        this.cooldownMillis = cooldownMillis;
    }

    Optional<Component> greet(Player player) {
        long now = this.clock.millis();
        UUID playerId = player.getUuid();
        Long lastGreeted = this.lastGreetedAtMillis.get(playerId);
        if (lastGreeted != null && ExampleGreetingRule.isOnCooldown(lastGreeted, now, this.cooldownMillis)) {
            return Optional.empty();
        }
        this.lastGreetedAtMillis.put(playerId, now);
        return Optional.of(ExampleGreetingRule.greeting(this.greeting, player.getUsername()));
    }

    /** Forgets {@code playerId}'s cooldown, so a rejoining player is greeted immediately. */
    void clear(UUID playerId) {
        this.lastGreetedAtMillis.remove(playerId);
    }
}
