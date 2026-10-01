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
package net.onelitefeather.titan.feature.portal;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

/** A test-only executor that holds a task until the test runs it, standing in for a slow read. */
final class QueuedExecutor implements Executor {

    private final Queue<Runnable> queue = new ArrayDeque<>();

    @Override
    public void execute(Runnable task) {
        this.queue.add(task);
    }

    int pending() {
        return this.queue.size();
    }

    void runAll() {
        Runnable task;
        while ((task = this.queue.poll()) != null) {
            task.run();
        }
    }
}
