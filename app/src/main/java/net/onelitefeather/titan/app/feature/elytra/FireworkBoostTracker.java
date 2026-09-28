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
package net.onelitefeather.titan.app.feature.elytra;

import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks each player's firework boost: burn ticks and cooldown ticks remaining, both advanced
 * once per tick by {@link #advance()}.
 *
 * <p>A {@code @Singleton} bean shared by {@link ElytraModule} and {@link ElytraLobbyItems}; not
 * thread-safe, so every method must run on the tick thread.
 */
@Singleton
final class FireworkBoostTracker {

    private final Map<UUID, Burn> burnByPlayer = new HashMap<>();

    // Also refused for a non-gliding player: Vanilla applies a rocket's impulse only while
    // fall-flying, so a boost granted on the ground would start a cooldown for nothing.
    boolean requestBoost(UUID playerId, int burnDurationTicks, int cooldownTicks, boolean flyingWithElytra) {
        if (!flyingWithElytra) {
            return false;
        }
        Burn held = this.burnByPlayer.get(playerId);
        if (held != null && held.cooldownTicks > 0) {
            return false;
        }
        this.burnByPlayer.put(playerId, new Burn(burnDurationTicks, cooldownTicks));
        return true;
    }

    void advance() {
        this.burnByPlayer.values().removeIf(Burn::expireOneTick);
    }

    boolean burning(UUID playerId) {
        return ticksRemaining(playerId) > 0;
    }

    int ticksRemaining(UUID playerId) {
        Burn held = this.burnByPlayer.get(playerId);
        return held == null ? 0 : held.burnTicks;
    }

    // Non-zero for the whole burn too, since the cooldown is measured from the burn's start.
    int cooldownTicksRemaining(UUID playerId) {
        Burn held = this.burnByPlayer.get(playerId);
        return held == null ? 0 : held.cooldownTicks;
    }

    void forget(UUID playerId) {
        this.burnByPlayer.remove(playerId);
    }

    private static final class Burn {

        private int burnTicks;
        private int cooldownTicks;

        private Burn(int burnTicks, int cooldownTicks) {
            this.burnTicks = burnTicks;
            this.cooldownTicks = cooldownTicks;
        }

        private boolean expireOneTick() {
            if (this.burnTicks > 0) {
                this.burnTicks--;
            }
            if (this.cooldownTicks > 0) {
                this.cooldownTicks--;
            }
            return this.burnTicks == 0 && this.cooldownTicks == 0;
        }
    }
}
