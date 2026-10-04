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
package net.onelitefeather.titan.persistence;

import io.opentelemetry.context.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Where other modules run database work that must not block a tick. It is created after the
 * {@code SessionFactory} (see {@link DatabaseFactory}), so Avaje closes it first: {@link #close()}
 * drains the pending tasks while the factory they use is still open. A module cannot own such an
 * executor itself, because it only reaches the factory lazily and would be closed after it.
 */
public final class DatabaseWriter implements AutoCloseable {

    // Deliberately not an Executor: Avaje would register it as one, and the runtime's own Executor
    // beans (Minestom's Scheduler) would then keep it from being created.
    // Virtual threads: a task blocks on JDBC, which must not stall a tick or a fixed pool.
    private final ExecutorService tasks = Executors.newVirtualThreadPerTaskExecutor();

    DatabaseWriter() {
    }

    /** @throws java.util.concurrent.RejectedExecutionException once the writer is closed */
    public void execute(Runnable task) {
        // The agent does not carry the context over an Executor, so database spans would be roots.
        this.tasks.execute(Context.current().wrap(task));
    }

    /** Waits for the tasks still pending, then refuses new ones. */
    @Override
    public void close() {
        this.tasks.close();
    }
}
