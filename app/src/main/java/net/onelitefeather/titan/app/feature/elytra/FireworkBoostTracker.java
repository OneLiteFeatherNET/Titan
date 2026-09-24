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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;
import net.minestom.server.ServerFlag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.FireworkList;
import net.minestom.server.timer.TaskSchedule;

/**
 * Tracks each flying player's active firework boost and drives its per-tick velocity update on
 * the player's own {@link net.minestom.server.timer.Scheduler}, applying the pure math in
 * {@link FireworkBoostPhysics} - the split mirrors {@code ElytraBoostListener} on {@code main},
 * moved here and separated from the physics for testability (see {@code design.md}, decision 11).
 *
 * <p>Using a firework while already boosted extends the remaining lifetime instead of starting a
 * second boost, exactly like stacking rockets in vanilla. A player's boost state is removed as
 * soon as it naturally expires, the player stops flying or goes offline (checked every tick in
 * {@link #tick(Player, State)}), or - explicitly, so no state lingers even a single tick longer
 * than necessary - via {@link #clear(UUID)}, which {@link ElytraModule} calls on stop-flying and
 * on disconnect.
 */
final class FireworkBoostTracker {

    private final RandomGenerator random;
    private final double boostMultiplier;
    private final Map<UUID, State> boosts = new ConcurrentHashMap<>();

    /**
     * @param random          the random source the boost lifetime is drawn from; inject a seeded
     *                        one in tests
     * @param boostMultiplier the configured {@link ElytraConfig#boostMultiplier()}
     */
    FireworkBoostTracker(RandomGenerator random, double boostMultiplier) {
        this.random = random;
        this.boostMultiplier = boostMultiplier;
    }

    /**
     * Boosts {@code player} if - and only if - they are currently flying with an elytra, per the
     * {@code lobby-hotbar} spec's "Boost beim Fliegen" scenario. A no-op while not flying, so a
     * player holding the firework on the ground never gets pushed around.
     *
     * @param player    the player who used the firework
     * @param usedStack the used firework stack, read only for its flight-duration component
     */
    void useFirework(Player player, ItemStack usedStack) {
        if (!player.isFlyingWithElytra()) {
            return;
        }
        int lifetime = FireworkBoostPhysics.lifetime(flightCount(usedStack), this.random);
        State active = this.boosts.get(player.getUuid());
        if (active != null) {
            // Using a rocket during an active boost extends it, as stacking rockets does in vanilla.
            active.remainingTicks = Math.max(active.remainingTicks, lifetime);
            return;
        }
        Vec initial = FireworkBoostPhysics.toPerTickVelocity(player.getVelocity(), ServerFlag.SERVER_TICKS_PER_SECOND);
        State state = new State(initial, lifetime);
        this.boosts.put(player.getUuid(), state);
        player.scheduler().submitTask(() -> tick(player, state));
    }

    /**
     * Removes {@code playerId}'s boost state immediately, without waiting for its next scheduled
     * tick to notice the player can no longer be boosted. Safe to call for a player with no active
     * boost.
     *
     * @param playerId the player to clear
     */
    void clear(UUID playerId) {
        this.boosts.remove(playerId);
    }

    /**
     * @param playerId the player to check
     * @return whether this tracker currently holds boost state for that player
     */
    boolean isTracking(UUID playerId) {
        return this.boosts.containsKey(playerId);
    }

    private TaskSchedule tick(Player player, State state) {
        if (!player.isOnline() || !player.isFlyingWithElytra() || state.remainingTicks-- <= 0) {
            this.boosts.remove(player.getUuid());
            return TaskSchedule.stop();
        }
        Vec look = player.getPosition().direction();
        Vec next = FireworkBoostPhysics.nextVelocity(state.velocity, look);
        state.velocity = next;
        player.setVelocity(FireworkBoostPhysics.toAppliedVelocity(next, ServerFlag.SERVER_TICKS_PER_SECOND, this.boostMultiplier));
        return TaskSchedule.tick(1);
    }

    private static int flightCount(ItemStack itemStack) {
        FireworkList fireworks = itemStack.get(DataComponents.FIREWORKS);
        return fireworks != null ? 1 + fireworks.flightDuration() : 1;
    }

    /** A single player's in-flight boost: the velocity it left off at and its remaining ticks. */
    private static final class State {

        private Vec velocity;
        private int remainingTicks;

        private State(Vec velocity, int remainingTicks) {
            this.velocity = velocity;
            this.remainingTicks = remainingTicks;
        }
    }
}
