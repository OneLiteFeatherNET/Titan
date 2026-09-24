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

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.FireworkList;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Covers {@link FireworkBoostTracker}'s per-player state: only boosts a flying player, applies the
 * first tick synchronously, stacks (extends) rather than restarts a boost already in progress, and
 * - the "no leak" requirement from task 6.7 - cleans up its state both explicitly
 * ({@link FireworkBoostTracker#clear}, as {@link ElytraModule} calls on stop-flying and
 * disconnect) and on its own once a tracked player stops flying or the boost's lifetime runs out.
 * Drives ticks with {@link Env#tick()}; no sleeps.
 */
@ExtendWith(MicrotusExtension.class)
class FireworkBoostTrackerTest {

    /**
     * A firework with an explicit, zero-length {@link FireworkList} - unlike
     * {@link ElytraItems#FIREWORK}, whose flight duration comes from
     * {@link Material#FIREWORK_ROCKET}'s own default components. Fixing the flight count at
     * {@code 1} keeps the tick counts below deterministic and independent of that default.
     *
     * <p>Built lazily (not as a static field) so it is only ever constructed once
     * {@link MicrotusExtension} has resolved an {@code Env} for the running test and Minestom's
     * registries are available.
     */
    private static ItemStack deterministicFirework() {
        return ItemStack.builder(Material.FIREWORK_ROCKET).set(DataComponents.FIREWORKS, new FireworkList(0, List.of())).build();
    }

    /** A {@link RandomGenerator} that always draws {@code 0}, for a deterministic lifetime. */
    private static RandomGenerator alwaysZeroRandom() {
        return new Random() {

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    @DisplayName("useFirework() does nothing for a player who is not flying")
    @Test
    void useFireworkDoesNothingForAPlayerWhoIsNotFlying(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        Vec before = player.getVelocity();
        FireworkBoostTracker tracker = new FireworkBoostTracker(new Random(1L), 2.0);

        tracker.useFirework(player, deterministicFirework());

        Assertions.assertFalse(tracker.isTracking(player.getUuid()));
        Assertions.assertEquals(before, player.getVelocity());
    }

    @DisplayName("useFirework() tracks a flying player and applies the first boost tick synchronously")
    @Test
    void useFireworkTracksAFlyingPlayerAndAppliesTheFirstTickSynchronously(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.setFlyingWithElytra(true);
        Vec before = player.getVelocity();
        FireworkBoostTracker tracker = new FireworkBoostTracker(new Random(1L), 2.0);

        tracker.useFirework(player, deterministicFirework());

        Assertions.assertTrue(tracker.isTracking(player.getUuid()));
        Assertions.assertNotEquals(before, player.getVelocity(), "the first boost tick must apply as soon as the firework is used, without waiting for a server tick");
    }

    @DisplayName("clear() removes a player's boost state immediately, with ticks still remaining")
    @Test
    void clearRemovesTrackingImmediately(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.setFlyingWithElytra(true);
        FireworkBoostTracker tracker = new FireworkBoostTracker(new Random(1L), 1.0);
        tracker.useFirework(player, deterministicFirework());
        Assertions.assertTrue(tracker.isTracking(player.getUuid()));

        tracker.clear(player.getUuid());

        Assertions.assertFalse(tracker.isTracking(player.getUuid()));
    }

    @DisplayName("clear() for a player with no active boost is a safe no-op")
    @Test
    void clearForAnUntrackedPlayerIsANoOp(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        FireworkBoostTracker tracker = new FireworkBoostTracker(new Random(1L), 1.0);

        Assertions.assertDoesNotThrow(() -> tracker.clear(player.getUuid()));
    }

    @DisplayName("Using a second firework while already boosted extends the boost instead of restarting it")
    @Test
    void usingASecondFireworkWhileBoostedExtendsTheBoost(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.setFlyingWithElytra(true);
        // Deterministic lifetime of 10 ticks per use (10 * flightCount=1 + 0 + 0).
        FireworkBoostTracker tracker = new FireworkBoostTracker(alwaysZeroRandom(), 1.0);

        tracker.useFirework(player, deterministicFirework());
        tracker.useFirework(player, deterministicFirework());

        // Without the extension, the boost (started with 10 ticks, one already consumed
        // synchronously) would have expired by the 10th env.tick(). With the extension, it must
        // still be tracked.
        for (int i = 0; i < 10; i++) {
            env.tick();
        }
        Assertions.assertTrue(tracker.isTracking(player.getUuid()), "a second rocket used during an active boost must extend it, not leave it to expire on the first rocket's own lifetime");

        env.tick();

        Assertions.assertFalse(tracker.isTracking(player.getUuid()), "the extended boost must still expire once its own (also extended) lifetime runs out");
    }

    @DisplayName("A boost's state is removed on its own once its lifetime runs out, without an explicit clear()")
    @Test
    void aBoostExpiresOnItsOwnOnceItsLifetimeRunsOut(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.setFlyingWithElytra(true);
        FireworkBoostTracker tracker = new FireworkBoostTracker(alwaysZeroRandom(), 1.0);

        tracker.useFirework(player, deterministicFirework());
        Assertions.assertTrue(tracker.isTracking(player.getUuid()));

        for (int i = 0; i < 10; i++) {
            env.tick();
        }

        Assertions.assertFalse(tracker.isTracking(player.getUuid()), "a boost that is never extended must expire on its own lifetime and remove its state - no leak");
    }

    @DisplayName("A tick notices the player stopped flying and removes the boost state on its own")
    @Test
    void aTickNoticesThePlayerStoppedFlyingAndRemovesTheBoostState(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.setFlyingWithElytra(true);
        FireworkBoostTracker tracker = new FireworkBoostTracker(alwaysZeroRandom(), 1.0);
        tracker.useFirework(player, deterministicFirework());
        Assertions.assertTrue(tracker.isTracking(player.getUuid()));

        player.setFlyingWithElytra(false);
        env.tick();

        Assertions.assertFalse(tracker.isTracking(player.getUuid()), "the next scheduled tick must notice the player is no longer flying and remove the boost state itself");
    }
}
