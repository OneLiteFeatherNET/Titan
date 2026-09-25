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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ported from Voyager ({@code net.elytrarace.voyager.platform.flight.FireworkBoostTracker}):
 * whose firework is burning, for how much longer, and who may not light another one yet.
 *
 * <p>This is the whole of the boost's determinism. Vanilla's rocket decides its own lifetime with
 * two dice rolls, which is fine for a fireworks display and not for a lobby that hands the same
 * rocket to everyone; here the server counts the ticks and the rocket entity ({@link
 * FireworkRockets}) is only the thing the client watches. Nothing in this class is random, nothing
 * in it reads a clock, and nothing in it knows what a rocket or a player is - it holds two counters
 * per player and is driven by {@link #advance()}.
 *
 * <h2>The two counters, and why the cooldown starts with the burn</h2>
 *
 * <p>A boost sets both at once: the burn to {@link ElytraConfig#burnDurationTicks()} and the
 * cooldown to {@link ElytraConfig#cooldownTicks()}. {@link #advance()} decrements both. So the
 * cooldown is measured <strong>from the tick the burn started</strong>, and because
 * {@code ElytraConfig} refuses a cooldown that is not strictly longer than the burn, a second
 * boost cannot begin before the first has ended.
 *
 * <h2>Where {@link #advance()} goes in a tick</h2>
 *
 * <p><strong>{@link ElytraModule} schedules {@link #advance()} once per tick through
 * {@code context.tasks()}.</strong> A tick that both starts a boost and advances it in the same
 * call would spend a tick of the burn before anything had observed it, and a boost configured for
 * 30 ticks would only ever run 29 - see {@link #requestBoost} and {@link #advance()}'s own
 * javadoc.
 *
 * <p>Not thread-safe: every method must be called from the single thread driving the tick loop,
 * exactly as in Voyager.
 */
final class FireworkBoostTracker {

    private final Map<UUID, Burn> burnByPlayer = new HashMap<>();

    /**
     * Lights a rocket for {@code playerId}, if they are allowed one.
     *
     * <p>Refused while a cooldown is running - which, by the class javadoc, includes the whole of
     * the current burn - and refused for a player who is not gliding. The second check is not
     * politeness: Vanilla's rocket applies its impulse only to an entity that is fall-flying, so a
     * rocket used on the ground moves nobody, and starting a burn for it would hold the player in a
     * cooldown for a boost they never got.
     *
     * @param playerId         who asked
     * @param config           the tuning to start this burn under, read now and not again for it
     * @param flyingWithElytra this tick's gliding flag for {@code playerId}
     * @return whether a burn started; {@code false} means nothing changed
     */
    boolean requestBoost(UUID playerId, ElytraConfig config, boolean flyingWithElytra) {
        if (!flyingWithElytra) {
            return false;
        }
        Burn held = this.burnByPlayer.get(playerId);
        if (held != null && held.cooldownTicks > 0) {
            return false;
        }
        this.burnByPlayer.put(playerId, new Burn(config.burnDurationTicks(), config.cooldownTicks()));
        return true;
    }

    /**
     * Counts every burn and every cooldown down by one tick. Call once per server tick, after any
     * per-tick sample of the boost's effect has already been taken.
     */
    void advance() {
        this.burnByPlayer.values().removeIf(Burn::expireOneTick);
    }

    /** Whether a rocket is burning for {@code playerId} on this tick. */
    boolean burning(UUID playerId) {
        return ticksRemaining(playerId) > 0;
    }

    /**
     * How many ticks of burn are left for {@code playerId}, this tick included; zero when none is
     * running.
     */
    int ticksRemaining(UUID playerId) {
        Burn held = this.burnByPlayer.get(playerId);
        return held == null ? 0 : held.burnTicks;
    }

    /**
     * How many ticks before {@code playerId} may boost again; zero when they may boost now.
     * Non-zero for the whole of a burn as well as for the wait after it, because the cooldown is
     * measured from the burn's start.
     */
    int cooldownTicksRemaining(UUID playerId) {
        Burn held = this.burnByPlayer.get(playerId);
        return held == null ? 0 : held.cooldownTicks;
    }

    /**
     * Drops everything held for {@code playerId} - {@link ElytraModule} calls this on
     * {@code PlayerStopFlyingWithElytraEvent} and on {@code PlayerDisconnectEvent}, so a player who
     * lands (or leaves) mid-burn never reconnects, or starts flying again, into the remains of a
     * cooldown they cannot see the reason for.
     *
     * @param playerId the player to forget
     */
    void forget(UUID playerId) {
        this.burnByPlayer.remove(playerId);
    }

    /**
     * One player's two counters. Mutable and package-private by design: this is on the tick path
     * for every flying player, and a record replaced in the map twice a tick would allocate for
     * nothing.
     */
    private static final class Burn {

        private int burnTicks;
        private int cooldownTicks;

        private Burn(int burnTicks, int cooldownTicks) {
            this.burnTicks = burnTicks;
            this.cooldownTicks = cooldownTicks;
        }

        /**
         * Counts both down by one and answers whether this player is worth holding an entry for
         * any longer.
         *
         * @return true once neither counter is running, so the entry can be dropped
         */
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
