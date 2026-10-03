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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.avaje.inject.BeanScope;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.onelitefeather.titan.persistence.DatabaseWriter;
import org.junit.jupiter.api.Test;

class JumprunWiringIntegrationTest extends JumprunDatabaseTest {

    private static final Instant AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void withTheDatabaseConfigured_theColumnsUnitIsPickedUpAndTheHibernateStoreIsWired() {
        // The unit comes from the column's own module, so persistence built the table it reads.
        assertInstanceOf(HibernateRunStore.class, start().get(RunStore.class), "the real store is the RunStore");
    }

    @Test
    void withTheDatabaseConfigured_theStoredRecordsAndTheLeaderboardReplaceTheInMemoryOnes() {
        BeanScope scope = start();

        assertInstanceOf(StoredRunRecords.class, scope.get(RunRecords.class), "records are kept in the database");
        assertTrue(scope.getOptional(Leaderboard.class).isPresent(), "the leaderboard exists");
    }

    @Test
    void withoutADatabaseUrl_theInMemoryRecordsStayAndThereIsNoLeaderboardOrStore() {
        BeanScope scope = startWithoutDatabase();

        assertInstanceOf(InMemoryRunRecords.class, scope.get(RunRecords.class), "records live in memory");
        assertTrue(scope.getOptional(Leaderboard.class).isEmpty(), "no leaderboard without a database");
        assertTrue(scope.getOptional(RunStore.class).isEmpty(), "no store without a database");
    }

    @Test
    void withoutADatabaseUrl_theModuleStartsWithoutALeaderboard() {
        BeanScope scope = startWithoutDatabase();

        // Built only if Avaje injects an empty Optional<Leaderboard> for the absent conditional bean.
        assertTrue(scope.getOptional(JumprunModule.class).isPresent(), "the module is wired");
        assertTrue(scope.getOptional(Leaderboard.class).isEmpty(), "and has no leaderboard");
    }

    // The scope of start() holds the runtime's Scheduler, which is itself an Executor.
    @Test
    void withARuntimeScheduler_theDatabaseWriterStillExists() {
        assertTrue(start().getOptional(DatabaseWriter.class).isPresent(), "the writer is not displaced by the Scheduler");
    }

    @Test
    void aFinishedRun_isWrittenThroughTheDatabaseWriter() {
        BeanScope scope = start();
        UUID player = UUID.randomUUID();

        scope.get(RunRecords.class).submit(new FinishedRun(player, "Alex", Mode.HARD, 12, EndReason.FALL, AT));
        scope.get(DatabaseWriter.class).close();

        assertEquals(Map.of(Mode.HARD, 12), scope.get(RunStore.class).bestsOf(player), "the drained write reached the database");
    }

    @Test
    void theLeaderboardRefresh_runsThroughTheDatabaseWriter() {
        BeanScope scope = start();
        UUID player = UUID.randomUUID();
        scope.get(RunStore.class).append(new FinishedRun(player, "Alex", Mode.EASY, 5, EndReason.FALL, AT));
        Leaderboard board = scope.get(Leaderboard.class);
        assertTrue(board.top(Mode.EASY).entries().isEmpty(), "nothing is on the board before the refresh");

        scope.get(JumprunModule.class).refreshLeaderboard();
        scope.get(DatabaseWriter.class).close();

        assertEquals(List.of(new TopEntry(player, "Alex", 5, AT)), board.top(Mode.EASY).entries(), "the handed-over refresh read the store");
    }
}
