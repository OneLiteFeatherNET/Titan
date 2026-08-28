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
package net.onelitefeather.titan.common.observability;

import io.sentry.Sentry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class TitanObservabilityTest {

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
        TitanObservability.consumeFailingPlayer();
    }

    @DisplayName("A listener that returns normally is passed through and records no player")
    @Test
    void guardDelegatesWithoutRecordingOnTheHealthyPath() {
        AtomicInteger calls = new AtomicInteger();
        Consumer<PlainEvent> guarded = TitanObservability.guard(event -> calls.incrementAndGet());

        guarded.accept(new PlainEvent());

        Assertions.assertEquals(1, calls.get(), "the wrapped listener must still be invoked");
        Assertions.assertNull(TitanObservability.consumeFailingPlayer(), "a successful dispatch must not leave player context behind");
    }

    @DisplayName("A failing listener rethrows the original throwable unchanged")
    @Test
    void guardRethrowsTheOriginalThrowable() {
        IllegalStateException failure = new IllegalStateException("listener broke");
        Consumer<PlainEvent> guarded = TitanObservability.guard(event -> {
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
        Consumer<PlayerBoundEvent> guarded = TitanObservability.guard(event -> {
            throw new IllegalStateException("listener broke");
        });

        Assertions.assertThrows(IllegalStateException.class, () -> guarded.accept(new PlayerBoundEvent(player)));

        TitanObservability.PlayerIdentity identity = TitanObservability.consumeFailingPlayer();
        Assertions.assertNotNull(identity, "a failure on a player event must record the player");
        Assertions.assertEquals(player.getUuid().toString(), identity.uuid());
        Assertions.assertEquals(player.getUsername(), identity.name());
    }

    @DisplayName("Recorded player context is consumed once, so it cannot mis-attribute a later failure")
    @Test
    void recordedIdentityIsClearedAfterBeingConsumed(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        Consumer<PlayerBoundEvent> guarded = TitanObservability.guard(event -> {
            throw new IllegalStateException("listener broke");
        });
        Assertions.assertThrows(IllegalStateException.class, () -> guarded.accept(new PlayerBoundEvent(player)));

        Assertions.assertNotNull(TitanObservability.consumeFailingPlayer());
        Assertions.assertNull(TitanObservability.consumeFailingPlayer(), "the second read must be empty - otherwise the next exception is blamed on this player");
    }

    @DisplayName("A failure on an event without a player records no identity")
    @Test
    void guardRecordsNothingForAnEventWithoutAPlayer() {
        Consumer<PlainEvent> guarded = TitanObservability.guard(event -> {
            throw new IllegalStateException("listener broke");
        });

        Assertions.assertThrows(IllegalStateException.class, () -> guarded.accept(new PlainEvent()));

        Assertions.assertNull(TitanObservability.consumeFailingPlayer());
    }

    @DisplayName("Without a DSN Sentry is never initialised")
    @Test
    void bootstrapLeavesSentryDisabledWithoutADsn() {
        // The environment variable is not set for this build, which is the operator-without-Sentry
        // case: bootstrap must be a no-op rather than a failure.
        Assertions.assertNull(System.getenv(TitanObservability.DSN_ENVIRONMENT_VARIABLE), "this test asserts the disabled path - unset " + TitanObservability.DSN_ENVIRONMENT_VARIABLE);

        TitanObservability.bootstrap("test-release");

        Assertions.assertFalse(Sentry.isEnabled(), "no DSN must leave the SDK untouched");
    }

    @DisplayName("Outside a jar the release falls back to dev rather than null")
    @Test
    void releaseFallsBackToDevWhenNoManifestIsPresent() {
        // Tests run from a class directory, so the Implementation-Version attribute the shaded jar
        // carries is absent here.
        Assertions.assertEquals("dev", TitanObservability.release());
    }
}
