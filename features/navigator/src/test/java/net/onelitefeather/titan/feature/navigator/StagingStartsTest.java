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
package net.onelitefeather.titan.feature.navigator;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.onelitefeather.titan.core.service.ServiceState;
import net.onelitefeather.titan.core.service.ServiceState.Phase;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.AdjustableClock;
import net.onelitefeather.titan.core.testfixtures.FakeOnDemandServices;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The decision of a staging click and of the refresh after it, against fakes and an explicit clock.
 */
class StagingStartsTest {

    private static final Destination STAGING = Destination.SLENDER_STAGING;
    private static final String TASK = STAGING.task();
    private static final String ANN = "ann";
    private static final String BOB = "bob";

    private final FakeOnDemandServices services = new FakeOnDemandServices();
    private final AdjustableClock clock = new AdjustableClock(Instant.parse("2026-10-26T12:00:00Z"));
    private final RecordingConnect connect = new RecordingConnect();
    private final RecordingNotice notice = new RecordingNotice();
    private final StagingStarts<String> starts = new StagingStarts<>(this.services, this.clock, this.connect, this.notice, new NavigatorTelemetry(Telemetry.noop()));

    StagingStartsTest() {
        this.connect.allowed.add(ANN);
        this.connect.allowed.add(BOB);
    }

    @DisplayName("A read error refuses the click and starts nothing")
    @Test
    void readErrorRefusesAndStartsNothing() {
        this.services.failReads(true);

        StagingStarts.Outcome outcome = click(ANN);

        Assertions.assertEquals(StagingStarts.Outcome.REFUSED, outcome, "an unreadable service list must refuse the click");
        Assertions.assertEquals(0, this.services.starts(), "nothing may be started while the services are unreadable");
        Assertions.assertEquals(List.of(ANN), this.notice.unavailable, "the player must be told the services are unavailable");
    }

    @DisplayName("A click with no service starts exactly one and makes the player wait")
    @Test
    void noServiceStartsExactlyOne() {
        Assertions.assertEquals(StagingStarts.Outcome.WAITING, click(ANN), "the player waits for the new service");

        Assertions.assertEquals(1, this.services.starts(), "one click with no service starts exactly one");
        Assertions.assertTrue(this.connect.connected.isEmpty(), "nobody is connected before the service is ready");
    }

    @DisplayName("A service that is still preparing is waited for and not started again")
    @Test
    void preparedServiceIsWaitedFor() {
        this.services.serve(TASK, new ServiceState("Slender-Staging-1", Phase.STARTING, 0, 0));

        Assertions.assertEquals(StagingStarts.Outcome.WAITING, click(ANN), "a preparing service makes the player wait");
        Assertions.assertEquals(0, this.services.starts(), "a preparing service must not be started a second time");
    }

    @DisplayName("A ready service connects the player at once")
    @Test
    void readyServiceConnectsAtOnce() {
        this.services.serve(TASK, new ServiceState("Slender-Staging-1", Phase.RUNNING, 0, 10));

        Assertions.assertEquals(StagingStarts.Outcome.CONNECTED, click(ANN), "a ready service connects the clicking player");
        Assertions.assertEquals(List.of(ANN + ":" + STAGING.name()), this.connect.connected, "the player is connected to the staging entry");
        Assertions.assertEquals(0, this.services.starts(), "a ready service needs no start");
    }

    @DisplayName("A running service without an announced limit makes the player wait")
    @Test
    void runningWithoutLimitWaits() {
        this.services.serve(TASK, new ServiceState("Slender-Staging-1", Phase.RUNNING, 0, 0));

        Assertions.assertEquals(StagingStarts.Outcome.WAITING, click(ANN), "a running service without a limit is not ready");
        Assertions.assertTrue(this.connect.connected.isEmpty(), "the player is not connected before the limit is announced");
    }

    @DisplayName("The cooldown after its own start blocks a second start of the same task")
    @Test
    void cooldownBlocksASecondStart() {
        click(ANN);
        this.services.serve(TASK);

        click(BOB);

        Assertions.assertEquals(1, this.services.starts(), "a second start within 30 seconds is blocked");
    }

    @DisplayName("The cooldown ends after 30 seconds, so a service that never appeared is started again")
    @Test
    void cooldownEndsAfterThirtySeconds() {
        click(ANN);
        this.services.serve(TASK);
        this.clock.advance(Duration.ofSeconds(30));

        click(BOB);

        Assertions.assertEquals(2, this.services.starts(), "after 30 seconds a new start is allowed");
    }

    @DisplayName("A second click while the service is starting starts nothing and keeps the player waiting")
    @Test
    void secondClickStartsNothing() {
        click(ANN);

        Assertions.assertEquals(StagingStarts.Outcome.WAITING, click(ANN), "the second click waits as well");
        Assertions.assertEquals(1, this.services.starts(), "the second click must not start a second service");
    }

    @DisplayName("A waiting player is connected by the refresh once the service is ready")
    @Test
    void waitingPlayerConnectsWhenReady() {
        click(ANN);
        this.services.serve(TASK, new ServiceState("Slender-Staging-1", Phase.RUNNING, 0, 10));

        refresh();

        Assertions.assertEquals(List.of(ANN + ":" + STAGING.name()), this.connect.connected, "the refresh connects the waiting player");
    }

    @DisplayName("Ninety seconds after the start the waiting players are refused and nothing is stopped")
    @Test
    void timeoutRefusesWithoutStopping() {
        click(ANN);
        this.clock.advance(Duration.ofSeconds(89));
        refresh();
        Assertions.assertTrue(this.notice.timeouts.isEmpty(), "no timeout before 90 seconds");

        this.clock.advance(Duration.ofSeconds(1));
        refresh();

        Assertions.assertEquals(List.of(ANN), this.notice.timeouts, "the waiting player is refused after 90 seconds");
        Assertions.assertTrue(this.services.stopped().isEmpty(), "the timeout must not stop the service");
        Assertions.assertTrue(this.connect.connected.isEmpty(), "a timed out player is not connected");
    }

    @DisplayName("A player who logs out while waiting is removed from the waiting list")
    @Test
    void logoutRemovesTheWaiter() {
        click(ANN);
        this.starts.disconnected(ANN);
        this.services.serve(TASK, new ServiceState("Slender-Staging-1", Phase.RUNNING, 0, 10));

        refresh();

        Assertions.assertTrue(this.connect.connected.isEmpty(), "a player who left must not be connected");
    }

    @DisplayName("A player who loses the permission while waiting is not connected and is dropped silently")
    @Test
    void revokedPermissionDoesNotConnect() {
        click(ANN);
        this.connect.allowed.remove(ANN);
        this.services.serve(TASK, new ServiceState("Slender-Staging-1", Phase.RUNNING, 0, 10));

        refresh();

        Assertions.assertTrue(this.connect.connected.isEmpty(), "a revoked permission must not connect the waiting player");
        Assertions.assertTrue(this.notice.timeouts.isEmpty(), "a revoked permission is dropped without a message");
    }

    @DisplayName("A start the provider refuses refuses the click, and the player waits for nothing")
    @Test
    void rejectedStartRefusesTheClick() {
        this.services.rejectStarts(true);

        StagingStarts.Outcome outcome = click(ANN);

        Assertions.assertEquals(StagingStarts.Outcome.REFUSED, outcome, "a refused start is a refusal, not a wait");
        Assertions.assertEquals(List.of(ANN), this.notice.unavailable, "the player is told the entry is unavailable");
    }

    @DisplayName("A click without the permission starts nothing and is denied")
    @Test
    void deniedClickStartsNothing() {
        this.connect.allowed.remove(ANN);

        StagingStarts.Outcome outcome = click(ANN);

        Assertions.assertEquals(StagingStarts.Outcome.DENIED, outcome, "a click without the permission is denied");
        Assertions.assertEquals(0, this.services.starts(), "a denied click must not start a service");
    }

    private StagingStarts.Outcome click(String player) {
        return this.starts.click(player, STAGING, StagingStarts.read(this.services, STAGING));
    }

    private void refresh() {
        this.starts.refresh(Map.of(STAGING, StagingStarts.read(this.services, STAGING)));
    }

    private static final class RecordingConnect implements StagingConnect<String> {

        private final Set<String> allowed = new HashSet<>();
        private final List<String> connected = new ArrayList<>();

        @Override
        public boolean allowed(String player, Destination destination) {
            return this.allowed.contains(player);
        }

        @Override
        public void connect(String player, Destination destination) {
            this.connected.add(player + ":" + destination.name());
        }
    }

    private static final class RecordingNotice implements StagingNotice<String> {

        private final List<String> waiting = new ArrayList<>();
        private final List<String> timeouts = new ArrayList<>();
        private final List<String> unavailable = new ArrayList<>();

        @Override
        public void waiting(String player, Destination destination) {
            this.waiting.add(player);
        }

        @Override
        public void timeout(String player, Destination destination) {
            this.timeouts.add(player);
        }

        @Override
        public void unavailable(String player) {
            this.unavailable.add(player);
        }
    }
}
