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
package net.onelitefeather.titan.app.bootstrap.reload;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

/**
 * A test-only {@link Executor} that never runs a submitted task on its own:
 * {@link #execute(Runnable)}
 * only enqueues it. The test drives all timing explicitly via {@link #runNext()}/{@link #runAll()},
 * on the test's own thread - no real thread, no sleep, exactly what
 * {@code openspec/changes/config-reload-feature-flags/tasks.md} task 3.2 asks
 * {@link ConfigReloader}
 * to be tested against (F.I.R.S.T. - Fast, Repeatable).
 *
 * <p>Used as both the worker and the tick executor in {@link ConfigReloaderTest}, so a test can
 * observe exactly how many runs of each stage happened, and in what order, without any timing
 * assumption.
 */
final class QueueExecutor implements Executor {

    private final Deque<Runnable> tasks = new ArrayDeque<>();

    @Override
    public void execute(Runnable command) {
        tasks.addLast(command);
    }

    boolean isEmpty() {
        return tasks.isEmpty();
    }

    int size() {
        return tasks.size();
    }

    /**
     * Runs exactly the next queued task, on the calling thread.
     *
     * @throws IllegalStateException if nothing is queued
     */
    void runNext() {
        Runnable next = tasks.pollFirst();
        if (next == null) {
            throw new IllegalStateException("no task queued");
        }
        next.run();
    }

    /**
     * Runs every currently queued task, in order - including ones a running task itself enqueues,
     * until the queue is empty.
     */
    void runAll() {
        while (!tasks.isEmpty()) {
            runNext();
        }
    }
}
