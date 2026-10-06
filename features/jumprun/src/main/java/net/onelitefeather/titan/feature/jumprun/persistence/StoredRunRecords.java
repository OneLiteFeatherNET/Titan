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
package net.onelitefeather.titan.feature.jumprun.persistence;

import io.avaje.inject.External;
import io.avaje.inject.RequiresProperty;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import net.onelitefeather.titan.feature.jumprun.course.Mode;
import net.onelitefeather.titan.persistence.DatabaseProperties;
import net.onelitefeather.titan.persistence.DatabaseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records that outlast the session: an {@link InMemoryRunRecords} is the cache the run reads, the
 * {@link RunStore} is where bests come from and runs go to. Gated like the store, so it replaces
 * the {@code @Secondary} in-memory bean only when a database is configured.
 */
@Singleton
@RequiresProperty(DatabaseProperties.URL)
public final class StoredRunRecords implements RunRecords {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoredRunRecords.class);

    private final InMemoryRunRecords cache = new InMemoryRunRecords();
    private final RunStore store;
    private final Executor writer;

    // The writer is persistence's, so Avaje drains it before it closes the SessionFactory; resolved
    // on first use, like the store's factory, because persistence is wired after this column.
    @Inject
    public StoredRunRecords(RunStore store, @External Provider<DatabaseWriter> writer) {
        this(store, task -> writer.get().execute(task));
    }

    public StoredRunRecords(RunStore store, Executor writer) {
        this.store = store;
        this.writer = writer;
    }

    @Override
    public OptionalInt best(UUID player, Mode mode) {
        return this.cache.best(player, mode);
    }

    @Override
    public boolean submit(FinishedRun run) {
        boolean isRecord = this.cache.submit(run);
        try {
            this.writer.execute(() -> append(run));
        } catch (RejectedExecutionException closed) {
            // The writer is gone (shutdown); the record still counts in the cache.
            LOGGER.atWarn().addKeyValue("player", run.player()).addKeyValue("mode", run.mode()).log("Could not store jump and run result, the writer is closed");
        }
        return isRecord;
    }

    @Override
    public void load(UUID player) {
        try {
            this.store.bestsOf(player).forEach((mode, score) -> this.cache.record(player, mode, score));
        } catch (RuntimeException failure) {
            LOGGER.atWarn().addKeyValue("player", player).setCause(failure).log("Could not load jump and run records");
        }
    }

    @Override
    public void forget(UUID player) {
        this.cache.forget(player);
    }

    private void append(FinishedRun run) {
        try {
            this.store.append(run);
        } catch (RuntimeException failure) {
            // The cache keeps the record until the player leaves; only its persistence is lost.
            LOGGER.atWarn().addKeyValue("player", run.player()).addKeyValue("mode", run.mode()).setCause(failure).log("Could not store jump and run result");
        }
    }
}
