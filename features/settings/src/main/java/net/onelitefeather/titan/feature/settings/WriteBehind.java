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
package net.onelitefeather.titan.feature.settings;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import net.onelitefeather.titan.core.settings.SettingKey;

/**
 * The queue of switched settings waiting to be saved. A switch waits for the debounce, so several
 * switches of one setting cost one write. A failed write that may succeed later is queued again
 * after a short gap, up to the retry limit; after that the setting stays queued, parked, until the
 * player leaves or switches it again.
 *
 * <p>Pure bookkeeping: it never calls the store. The caller takes the due writes, performs them off
 * the tick thread and reports each failure back here.
 */
final class WriteBehind {

    static final Duration RETRY_GAP = Duration.ofMillis(500);

    private final Clock clock;
    private final Duration debounce;
    private final int retries;
    private final Map<Target, Pending> pending = new HashMap<>();

    WriteBehind(Clock clock, Duration debounce, int retries) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.debounce = Objects.requireNonNull(debounce, "debounce must not be null");
        this.retries = retries;
    }

    /** Queues {@code value}; a switch that is already queued waits for the debounce again. */
    synchronized void changed(UUID player, SettingKey key, boolean value) {
        this.pending.put(new Target(player, key), new Pending(value, this.clock.instant().plus(this.debounce), 0));
    }

    /** @return the writes whose debounce has run out; they leave the queue */
    synchronized List<Write> takeDue() {
        Instant now = this.clock.instant();
        return take(target -> {
            Pending entry = this.pending.get(target);
            return entry.dueAt() != null && !entry.dueAt().isAfter(now);
        });
    }

    /** @return every queued write of {@code player}, due or not; they leave the queue */
    synchronized List<Write> takeAll(UUID player) {
        return take(target -> target.player().equals(player));
    }

    /** @return every queued write, due or not; they leave the queue */
    synchronized List<Write> takeEverything() {
        return take(target -> true);
    }

    /**
     * Records a failed write.
     *
     * @return {@code true} if the write is queued again
     */
    synchronized boolean failed(Write write, boolean retryable) {
        Target target = new Target(write.player(), write.key());
        if (!retryable || this.pending.containsKey(target)) {
            // A newer switch of the same setting replaces the failed value, so it is not retried.
            return false;
        }
        int failures = write.failures() + 1;
        boolean again = failures <= this.retries;
        Instant dueAt = again ? this.clock.instant().plus(RETRY_GAP) : null;
        this.pending.put(target, new Pending(write.value(), dueAt, failures));
        return again;
    }

    private List<Write> take(Predicate<Target> selected) {
        List<Write> taken = new ArrayList<>();
        for (Map.Entry<Target, Pending> entry : new ArrayList<>(this.pending.entrySet())) {
            if (selected.test(entry.getKey())) {
                this.pending.remove(entry.getKey());
                taken.add(new Write(entry.getKey().player(), entry.getKey().key(), entry.getValue().value(), entry.getValue().failures()));
            }
        }
        return taken;
    }

    private record Target(UUID player, SettingKey key) {
    }

    /** {@code dueAt} is {@code null} for a parked write, which waits for the player to leave. */
    private record Pending(boolean value, Instant dueAt, int failures) {
    }

    /** One write to perform: the value to store and how many attempts have failed so far. */
    record Write(UUID player, SettingKey key, boolean value, int failures) {
    }
}
