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

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.portal.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the running services of the lobby task. When the provider throws, the last good snapshot
 * stays; the first failure of each kind is a warning, later ones only DEBUG, and a recovery is
 * reported so that the next failure is news again.
 */
final class SwitcherReading {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwitcherReading.class);

    /** The result of one read. */
    sealed interface Outcome permits Fresh, Stale, Unavailable {
    }

    /** Read just now. */
    record Fresh(List<ServiceCount> services) implements Outcome {
    }

    /** The read failed; this is the last good snapshot. */
    record Stale(List<ServiceCount> services) implements Outcome {
    }

    /** The read failed and there is no earlier snapshot. */
    record Unavailable() implements Outcome {
    }

    private final PlayerCounts counts;
    // Reads run on a virtual thread, not on the tick thread.
    private final Set<String> failureKinds = ConcurrentHashMap.newKeySet();
    private volatile List<ServiceCount> lastGood;

    SwitcherReading(PlayerCounts counts) {
        this.counts = counts;
    }

    Outcome read(String task) {
        try {
            List<ServiceCount> services = List.copyOf(this.counts.running(SourceType.TASK, task));
            this.lastGood = services;
            if (!this.failureKinds.isEmpty()) {
                this.failureKinds.clear();
                LOGGER.info("Reading the running services of task '{}' works again", task);
            }
            return new Fresh(services);
        } catch (RuntimeException | LinkageError e) {
            return failed(task, e);
        }
    }

    private Outcome failed(String task, Throwable e) {
        if (this.failureKinds.add(e.getClass().getName())) {
            LOGGER.warn("Reading the running services of task '{}' failed: {}", task, e.toString(), e);
        }
        LOGGER.debug("Reading the running services of task '{}' failed", task, e);
        List<ServiceCount> kept = this.lastGood;
        return kept == null ? new Unavailable() : new Stale(kept);
    }
}
