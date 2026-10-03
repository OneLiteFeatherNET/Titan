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
package net.onelitefeather.titan.feature.jumprun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StoredRunRecordsTest {

    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000a1ec");

    private final FakeRunStore store = new FakeRunStore();
    private final StoredRunRecords records = new StoredRunRecords(this.store, Runnable::run);
    private CapturedLog log;

    @BeforeEach
    void captureLog() {
        this.log = new CapturedLog(StoredRunRecords.class);
    }

    @AfterEach
    void releaseLog() {
        this.log.close();
    }

    private static FinishedRun run(Mode mode, int score) {
        return RunStoreContract.run(ALEX, "Alex", mode, score, 0);
    }

    @Test
    void loadFillsTheBestsFromTheStore() {
        this.store.append(run(Mode.HARD, 15));
        this.store.append(run(Mode.EASY, 30));

        this.records.load(ALEX);

        assertEquals(OptionalInt.of(15), this.records.best(ALEX, Mode.HARD), "hard best from the store");
        assertEquals(OptionalInt.of(30), this.records.best(ALEX, Mode.EASY), "easy best from the store");
    }

    @Test
    void aScoreBelowTheLoadedBestIsNoRecord() {
        this.store.append(run(Mode.HARD, 15));
        this.records.load(ALEX);

        assertFalse(this.records.submit(run(Mode.HARD, 15)), "a tie with the stored best is no record");
        assertTrue(this.records.submit(run(Mode.HARD, 16)), "16 beats the stored 15");
    }

    @Test
    void aRunWithScoreZeroIsAppendedToo() {
        this.records.submit(run(Mode.EASY, 0));

        assertEquals(0, this.store.bestsOf(ALEX).get(Mode.EASY), "a run that never scored is stored");
    }

    @Test
    void aRunThatIsNoRecordIsAppendedToo() {
        this.records.submit(run(Mode.HARD, 9));
        this.store.failWith(new IllegalStateException("database is gone"));

        this.records.submit(run(Mode.HARD, 2));

        assertEquals(1, this.log.warnings().size(), "the run below the record still went to the store, and failed there");
    }

    @Test
    void aRunThatFailsToStoreStaysARecordInTheCacheAndLogsOneWarning() {
        IllegalStateException failure = new IllegalStateException("database is gone");
        this.store.failWith(failure);

        boolean isRecord = this.records.submit(run(Mode.HARD, 7));

        assertTrue(isRecord, "the player still gets the record");
        assertEquals(OptionalInt.of(7), this.records.best(ALEX, Mode.HARD), "the cache keeps it");
        assertEquals(1, this.log.warnings().size(), "one warning per failed run");
        ILoggingEvent line = this.log.warnings().getFirst();
        assertEquals("Could not store jump and run result", line.getMessage());
        assertEquals(ALEX, CapturedLog.valueOf(line, "player"));
        assertEquals(Mode.HARD, CapturedLog.valueOf(line, "mode"));
        assertSame(failure, CapturedLog.causeOf(line), "the exception is logged once, as the last argument");
    }

    @Test
    void aFailedLoadLeavesTheCacheEmptyAndLogsOneWarning() {
        this.store.append(run(Mode.HARD, 15));
        IllegalStateException failure = new IllegalStateException("database is gone");
        this.store.failWith(failure);

        this.records.load(ALEX);

        assertTrue(this.records.best(ALEX, Mode.HARD).isEmpty(), "nothing was loaded");
        assertEquals(1, this.log.warnings().size(), "one warning per failed load");
        ILoggingEvent line = this.log.warnings().getFirst();
        assertEquals("Could not load jump and run records", line.getMessage());
        assertEquals(ALEX, CapturedLog.valueOf(line, "player"));
        assertSame(failure, CapturedLog.causeOf(line), "the exception is logged once");
    }

    @Test
    void forgetClearsTheCacheButNotTheStore() {
        this.records.submit(run(Mode.HARD, 15));

        this.records.forget(ALEX);

        assertTrue(this.records.best(ALEX, Mode.HARD).isEmpty(), "the cache is empty");
        assertEquals(15, this.store.bestsOf(ALEX).get(Mode.HARD), "the store keeps the run");
    }

    @Test
    void aRunSubmittedAfterTheWriterClosedStaysInTheCacheAndLogsOneWarning() {
        StoredRunRecords closed = new StoredRunRecords(this.store, task -> {
            throw new RejectedExecutionException("closed");
        });

        boolean isRecord = closed.submit(run(Mode.HARD, 7));

        assertTrue(isRecord, "the player still gets the record");
        assertEquals(OptionalInt.of(7), closed.best(ALEX, Mode.HARD), "the cache keeps it");
        assertEquals(1, this.log.warnings().size(), "one warning for the lost write");
        ILoggingEvent line = this.log.warnings().getFirst();
        assertEquals(ALEX, CapturedLog.valueOf(line, "player"));
        assertEquals(Mode.HARD, CapturedLog.valueOf(line, "mode"));
        assertTrue(this.store.bestsOf(ALEX).isEmpty(), "nothing reached the store");
    }
}
