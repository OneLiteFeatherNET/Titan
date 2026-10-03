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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class DatabaseWriterTest {

    private static final long BOUND_SECONDS = 10;

    /** Spins until {@code closer} is parked, i.e. blocked in close(); no sleeping. */
    private static void awaitParked(Thread closer) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(BOUND_SECONDS);
        while (closer.getState() != Thread.State.WAITING && closer.getState() != Thread.State.TIMED_WAITING) {
            assertTrue(closer.isAlive(), "close() returned although a task was still pending");
            assertTrue(System.nanoTime() < deadline, "close() neither returned nor blocked in time, state " + closer.getState());
            Thread.onSpinWait();
        }
    }

    @Test
    void closeWaitsForThePendingTasks() throws InterruptedException {
        DatabaseWriter writer = new DatabaseWriter();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean done = new AtomicBoolean();
        writer.execute(() -> {
            entered.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            done.set(true);
        });
        assertTrue(entered.await(BOUND_SECONDS, TimeUnit.SECONDS), "the task started");
        Thread closer = Thread.ofPlatform().start(writer::close);

        awaitParked(closer);
        assertFalse(done.get(), "the task is still blocked while close() waits");

        release.countDown();
        closer.join(TimeUnit.SECONDS.toMillis(BOUND_SECONDS));
        assertFalse(closer.isAlive(), "close() returns once the task is done");
        assertTrue(done.get(), "the task finished before close() returned");
    }

    @Test
    void executeAfterCloseIsRejected() {
        DatabaseWriter writer = new DatabaseWriter();
        writer.close();

        assertThrows(RejectedExecutionException.class, () -> writer.execute(() -> {
        }), "a closed writer refuses new tasks");
    }
}
