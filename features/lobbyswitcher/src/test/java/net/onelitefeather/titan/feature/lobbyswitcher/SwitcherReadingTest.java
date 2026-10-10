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
package net.onelitefeather.titan.feature.lobbyswitcher;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.portal.SourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class SwitcherReadingTest {

    /** Answers with a list, or throws what it was told to throw. */
    private static final class FakeCounts implements PlayerCounts {
        List<ServiceCount> services = List.of();
        RuntimeException failure;
        SourceType askedType;
        String askedName;

        @Override
        public PlayerCount count(SourceType type, String name) {
            throw new UnsupportedOperationException("the switcher lists services, it does not sum them");
        }

        @Override
        public List<ServiceCount> running(SourceType type, String name) {
            this.askedType = type;
            this.askedName = name;
            if (this.failure != null) {
                throw this.failure;
            }
            return this.services;
        }
    }

    private static final ServiceCount LOBBY_1 = new ServiceCount("Lobby-1", 3, 50);
    private static final ServiceCount LOBBY_2 = new ServiceCount("Lobby-2", 8, 50);

    private final FakeCounts counts = new FakeCounts();
    private final SwitcherReading reading = new SwitcherReading(this.counts);

    private final Logger readingLogger = (Logger) LoggerFactory.getLogger(SwitcherReading.class);
    private final ListAppender<ILoggingEvent> lines = new ListAppender<>();

    @BeforeEach
    void captureLog() {
        this.lines.start();
        this.readingLogger.addAppender(this.lines);
    }

    @AfterEach
    void releaseLog() {
        this.readingLogger.detachAppender(this.lines);
        this.lines.stop();
    }

    private long linesAt(Level level) {
        return this.lines.list.stream().filter(line -> line.getLevel() == level).count();
    }

    @DisplayName("A working provider yields a fresh snapshot of the task's running services")
    @Test
    void freshSnapshot() {
        this.counts.services = List.of(LOBBY_1, LOBBY_2);

        SwitcherReading.Outcome outcome = this.reading.read("Lobby");

        Assertions.assertEquals(new SwitcherReading.Fresh(List.of(LOBBY_1, LOBBY_2)), outcome);
    }

    @DisplayName("The read asks for the running services of the task")
    @Test
    void asksForTheTask() {
        this.reading.read("Lobby");

        Assertions.assertEquals(SourceType.TASK, this.counts.askedType);
        Assertions.assertEquals("Lobby", this.counts.askedName);
    }

    @DisplayName("A failing provider keeps the last good snapshot")
    @Test
    void failureKeepsLastSnapshot() {
        this.counts.services = List.of(LOBBY_1);
        this.reading.read("Lobby");
        this.counts.failure = new IllegalStateException("bridge down");

        SwitcherReading.Outcome outcome = this.reading.read("Lobby");

        Assertions.assertEquals(new SwitcherReading.Stale(List.of(LOBBY_1)), outcome);
    }

    @DisplayName("A failing provider without any earlier snapshot is unavailable")
    @Test
    void failureWithoutSnapshotIsUnavailable() {
        this.counts.failure = new IllegalStateException("bridge down");

        Assertions.assertEquals(new SwitcherReading.Unavailable(), this.reading.read("Lobby"));
    }

    @DisplayName("The first failure warns once")
    @Test
    void firstFailureWarns() {
        this.counts.failure = new IllegalStateException("bridge down");

        this.reading.read("Lobby");

        Assertions.assertEquals(1, linesAt(Level.WARN), "one warning");
        String message = this.lines.list.getFirst().getFormattedMessage();
        Assertions.assertTrue(message.contains("Lobby") && message.contains("bridge down"), "names the task and the cause: " + message);
    }

    @DisplayName("Repeated failures of the same kind log DEBUG only")
    @Test
    void repeatedFailuresAreDebug() {
        this.counts.failure = new IllegalStateException("bridge down");

        this.reading.read("Lobby");
        this.reading.read("Lobby");
        this.reading.read("Lobby");

        Assertions.assertEquals(1, linesAt(Level.WARN), "still one warning");
        Assertions.assertEquals(3, linesAt(Level.DEBUG), "every failure leaves a DEBUG line");
    }

    @DisplayName("A different kind of failure warns again")
    @Test
    void otherKindWarnsAgain() {
        this.counts.failure = new IllegalStateException("bridge down");
        this.reading.read("Lobby");
        this.counts.failure = new IllegalArgumentException("bad task");

        this.reading.read("Lobby");

        Assertions.assertEquals(2, linesAt(Level.WARN), "one warning per failure kind");
    }

    @DisplayName("A failure after a recovery warns again")
    @Test
    void failureAfterRecoveryWarnsAgain() {
        this.counts.failure = new IllegalStateException("bridge down");
        this.reading.read("Lobby");
        this.counts.failure = null;
        this.reading.read("Lobby");
        this.counts.failure = new IllegalStateException("bridge down again");

        this.reading.read("Lobby");

        Assertions.assertEquals(2, linesAt(Level.WARN), "the recovery made the failure news again");
    }

    @DisplayName("A recovery is logged")
    @Test
    void recoveryIsLogged() {
        this.counts.failure = new IllegalStateException("bridge down");
        this.reading.read("Lobby");
        this.counts.failure = null;

        this.reading.read("Lobby");

        Assertions.assertEquals(1, linesAt(Level.INFO), "one recovery line");
    }

    @DisplayName("A read without an earlier failure logs nothing")
    @Test
    void healthyReadIsSilent() {
        this.reading.read("Lobby");
        this.reading.read("Lobby");

        Assertions.assertTrue(this.lines.list.isEmpty(), "no failure, no line");
    }

    @DisplayName("After a recovery the snapshot is the new one, not the old one")
    @Test
    void recoveryReplacesSnapshot() {
        this.counts.services = List.of(LOBBY_1);
        this.reading.read("Lobby");
        this.counts.services = List.of(LOBBY_2);
        this.reading.read("Lobby");
        this.counts.failure = new IllegalStateException("bridge down");

        Assertions.assertEquals(new SwitcherReading.Stale(List.of(LOBBY_2)), this.reading.read("Lobby"));
    }
}
