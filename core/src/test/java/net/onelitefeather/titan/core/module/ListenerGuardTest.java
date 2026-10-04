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
package net.onelitefeather.titan.core.module;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Moved from {@code common}'s {@code TitanObservabilityTest} together with {@link ListenerGuard}.
 */
@ExtendWith(MicrotusExtension.class)
class ListenerGuardTest {

    /** A listener failure that has nothing to do with a player. */
    private record PlainEvent() implements Event {
    }

    private record PlayerBoundEvent(Player player) implements PlayerEvent {

        @Override
        public Player getPlayer() {
            return this.player;
        }
    }

    @AfterEach
    void clearRecordedIdentity() {
        ListenerGuard.consumeFailingPlayer();
        ListenerGuard.consumeFailingModule();
    }

    @DisplayName("A listener that returns normally is passed through and records no player")
    @Test
    void guardDelegatesWithoutRecordingOnTheHealthyPath() {
        AtomicInteger calls = new AtomicInteger();
        Consumer<PlainEvent> guarded = ListenerGuard.guard(event -> calls.incrementAndGet());

        guarded.accept(new PlainEvent());

        Assertions.assertEquals(1, calls.get(), "the wrapped listener must still be invoked");
        Assertions.assertNull(ListenerGuard.consumeFailingPlayer(), "a successful dispatch must not leave player context behind");
    }

    @DisplayName("A failing listener rethrows the original throwable unchanged")
    @Test
    void guardRethrowsTheOriginalThrowable() {
        IllegalStateException failure = new IllegalStateException("listener broke");
        Consumer<PlainEvent> guarded = ListenerGuard.guard(event -> {
            throw failure;
        });

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> guarded.accept(new PlainEvent()));

        Assertions.assertSame(failure, thrown, "the guard must not wrap or swallow the failure");
    }

    @DisplayName("A failing listener on a player event records that player's uuid and name")
    @Test
    void guardRecordsThePlayerOfAFailingEvent(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        Consumer<PlayerBoundEvent> guarded = ListenerGuard.guard(event -> {
            throw new IllegalStateException("listener broke");
        });

        Assertions.assertThrows(IllegalStateException.class, () -> guarded.accept(new PlayerBoundEvent(player)));

        ListenerGuard.PlayerIdentity identity = ListenerGuard.consumeFailingPlayer();
        Assertions.assertNotNull(identity, "a failure on a player event must record the player");
        Assertions.assertEquals(player.getUuid().toString(), identity.uuid());
        Assertions.assertEquals(player.getUsername(), identity.name());
    }

    @DisplayName("Recorded player context is consumed once, so it cannot mis-attribute a later failure")
    @Test
    void recordedIdentityIsClearedAfterBeingConsumed(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        Consumer<PlayerBoundEvent> guarded = ListenerGuard.guard(event -> {
            throw new IllegalStateException("listener broke");
        });
        Assertions.assertThrows(IllegalStateException.class, () -> guarded.accept(new PlayerBoundEvent(player)));

        Assertions.assertNotNull(ListenerGuard.consumeFailingPlayer());
        Assertions.assertNull(ListenerGuard.consumeFailingPlayer(), "the second read must be empty - otherwise the next exception is blamed on this player");
    }

    @DisplayName("A failure on an event without a player records no identity")
    @Test
    void guardRecordsNothingForAnEventWithoutAPlayer() {
        Consumer<PlainEvent> guarded = ListenerGuard.guard(event -> {
            throw new IllegalStateException("listener broke");
        });

        Assertions.assertThrows(IllegalStateException.class, () -> guarded.accept(new PlainEvent()));

        Assertions.assertNull(ListenerGuard.consumeFailingPlayer());
    }

    @DisplayName("guard(moduleId, listener) still delegates to the wrapped listener on the healthy path")
    @Test
    void guardWithModuleIdDelegatesOnTheHealthyPath() {
        AtomicInteger calls = new AtomicInteger();
        Consumer<PlainEvent> guarded = ListenerGuard.guard("sit", Telemetry.noop(), event -> calls.incrementAndGet());

        guarded.accept(new PlainEvent());

        Assertions.assertEquals(1, calls.get(), "the wrapped listener must still be invoked");
        Assertions.assertNull(ListenerGuard.consumeFailingModule(), "a successful dispatch must not leave module context behind");
    }

    @DisplayName("A failing guard(moduleId, listener) records both the module and the player")
    @Test
    void guardWithModuleIdRecordsModuleAndPlayerOnFailure(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        Consumer<PlayerBoundEvent> guarded = ListenerGuard.guard("sit", Telemetry.noop(), event -> {
            throw new IllegalStateException("listener broke");
        });

        Assertions.assertThrows(IllegalStateException.class, () -> guarded.accept(new PlayerBoundEvent(player)));

        Assertions.assertEquals("sit", ListenerGuard.consumeFailingModule(), "the failing module must be recorded");
        ListenerGuard.PlayerIdentity identity = ListenerGuard.consumeFailingPlayer();
        Assertions.assertNotNull(identity, "the failing player must still be recorded alongside the module");
        Assertions.assertEquals(player.getUsername(), identity.name());
    }

    @DisplayName("A failure in guard(moduleId, listener) on an event without a player records the module but no player")
    @Test
    void guardWithModuleIdRecordsModuleWithoutAPlayer() {
        Consumer<PlainEvent> guarded = ListenerGuard.guard("sit", Telemetry.noop(), event -> {
            throw new IllegalStateException("listener broke");
        });

        Assertions.assertThrows(IllegalStateException.class, () -> guarded.accept(new PlainEvent()));

        Assertions.assertEquals("sit", ListenerGuard.consumeFailingModule());
        Assertions.assertNull(ListenerGuard.consumeFailingPlayer());
    }

    @DisplayName("guard(moduleId, listener) rethrows the original throwable unchanged")
    @Test
    void guardWithModuleIdRethrowsTheOriginalThrowable() {
        IllegalStateException failure = new IllegalStateException("listener broke");
        Consumer<PlainEvent> guarded = ListenerGuard.guard("sit", Telemetry.noop(), event -> {
            throw failure;
        });

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> guarded.accept(new PlainEvent()));

        Assertions.assertSame(failure, thrown);
    }

    @DisplayName("handleException logs and clears both module and player context without rethrowing")
    @Test
    void handleExceptionConsumesModuleAndPlayerWithoutRethrowing(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        Consumer<PlayerBoundEvent> guarded = ListenerGuard.guard("sit", Telemetry.noop(), event -> {
            throw new IllegalStateException("listener broke");
        });
        Assertions.assertThrows(IllegalStateException.class, () -> guarded.accept(new PlayerBoundEvent(player)));

        Assertions.assertDoesNotThrow(() -> ListenerGuard.handleException(new IllegalStateException("listener broke")), "handleException must never propagate - the lobby keeps running");

        Assertions.assertNull(ListenerGuard.consumeFailingModule(), "handleException must consume the recorded module");
        Assertions.assertNull(ListenerGuard.consumeFailingPlayer(), "handleException must consume the recorded player");
    }
}
