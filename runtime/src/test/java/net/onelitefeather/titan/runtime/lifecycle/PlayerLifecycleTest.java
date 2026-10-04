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
package net.onelitefeather.titan.runtime.lifecycle;

import io.opentelemetry.api.common.Attributes;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class PlayerLifecycleTest {

    private final TestTelemetry test = TestTelemetry.create();

    private PlayerLifecycle lifecycle;

    @AfterEach
    void closeTelemetry() {
        if (this.lifecycle != null) {
            this.lifecycle.stop();
        }
        this.test.close();
    }

    private void attach(Env env, Telemetry telemetry) {
        EventNode<Event> titan = EventNode.all("titan");
        env.process().eventHandler().addChild(titan);
        this.lifecycle = new PlayerLifecycle(telemetry, titan, () -> env.process().connection().getOnlinePlayerCount());
        this.lifecycle.start();
    }

    @DisplayName("Joining records player.configure and player.join spans carrying the player's uuid")
    @Test
    void joiningRecordsConfigureAndJoinSpans(Env env) {
        attach(env, this.test.telemetry());
        Instance instance = env.createFlatInstance();

        Player player = env.createPlayer(instance);
        env.tick();

        Assertions.assertEquals(player.getUuid().toString(), this.test.attribute(this.test.span("player.configure"), Telemetry.USER_ID));
        Assertions.assertEquals(player.getUuid().toString(), this.test.attribute(this.test.span("player.join"), Telemetry.USER_ID));
    }

    @DisplayName("Leaving records a player.disconnect span carrying the player's uuid")
    @Test
    void leavingRecordsADisconnectSpan(Env env) {
        attach(env, this.test.telemetry());
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        player.remove();
        env.tick();

        Assertions.assertEquals(player.getUuid().toString(), this.test.attribute(this.test.span("player.disconnect"), Telemetry.USER_ID));
    }

    @DisplayName("player.joins and player.disconnects each count one for one join and one leave")
    @Test
    void joinAndLeaveAreCountedOnce(Env env) {
        attach(env, this.test.telemetry());
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        env.tick();
        Assertions.assertEquals(1, this.test.counter("titan.player.joins", Attributes.empty()), "one join so far");
        Assertions.assertEquals(0, this.test.counter("titan.player.disconnects", Attributes.empty()), "nobody left yet");

        player.remove();
        env.tick();

        Assertions.assertEquals(1, this.test.counter("titan.player.joins", Attributes.empty()));
        Assertions.assertEquals(1, this.test.counter("titan.player.disconnects", Attributes.empty()));
    }

    @DisplayName("A spawn that is not the first one does not count as a join")
    @Test
    void anInstanceChangeIsNotAJoin(Env env) {
        attach(env, this.test.telemetry());
        Player player = env.createPlayer(env.createFlatInstance());
        env.tick();

        player.setInstance(env.createFlatInstance()).join();
        env.tick();

        Assertions.assertEquals(1, this.test.counter("titan.player.joins", Attributes.empty()), "moving between instances must not count as another join");
    }

    @DisplayName("titan.players.online follows the number of connected players")
    @Test
    void theGaugeFollowsTheOnlinePlayerCount(Env env) {
        attach(env, this.test.telemetry());
        Instance instance = env.createFlatInstance();
        Assertions.assertEquals(0, this.test.gauge("titan.players.online"), "no player yet");

        env.createPlayer(instance);
        env.tick();

        Assertions.assertEquals(1, this.test.gauge("titan.players.online"));
    }

    @DisplayName("No span name or attribute value contains the player's name")
    @Test
    void theSpansDoNotCarryThePlayersName(Env env) {
        attach(env, this.test.telemetry());
        Player player = env.createPlayer(env.createFlatInstance());
        env.tick();
        player.remove();
        env.tick();

        Assertions.assertFalse(this.test.spans().isEmpty(), "the lifecycle spans must exist for this check to mean anything");
        Assertions.assertTrue(this.test.allSpanText().stream().noneMatch(text -> text.contains(player.getUsername())), "the player name must not appear in any span");
    }

    @DisplayName("With the no-op telemetry a join and a leave run without an exception")
    @Test
    void joinAndLeaveWorkWithNoopTelemetry(Env env) {
        attach(env, Telemetry.noop());
        Player player = env.createPlayer(env.createFlatInstance());
        env.tick();

        Assertions.assertDoesNotThrow(() -> {
            player.remove();
            env.tick();
        });
        Assertions.assertTrue(player.isRemoved(), "the player must be gone after the leave");
    }

    @DisplayName("A player who was never counted as joined is not counted as disconnected")
    @Test
    void aDisconnectWithoutACountedJoinIsNotCounted(Env env) {
        Player earlyPlayer = env.createPlayer(env.createFlatInstance());
        env.tick();
        attach(env, this.test.telemetry());

        earlyPlayer.remove();
        env.tick();

        Assertions.assertEquals(0, this.test.counter("titan.player.disconnects", Attributes.empty()), "this player joined before the lifecycle was attached");
        Assertions.assertEquals(1, this.test.spans().stream().filter(span -> span.getName().equals("player.disconnect")).count(), "the span is still recorded");
    }
}
