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
package net.onelitefeather.titan.feature.navigator;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.portal.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The players on a CloudNet task, read from the running services of the {@link PlayerCounts}
 * provider and summed in {@link #online}. A count is unknown while the task runs nowhere; the
 * services themselves are unreadable (see {@link #services}) when there is no provider or the
 * lookup fails, which is not the same as a task that runs nowhere.
 */
final class TaskPlayers {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskPlayers.class);

    private final Optional<PlayerCounts> counts;

    TaskPlayers(Optional<PlayerCounts> counts) {
        this.counts = counts;
    }

    /** The players over one snapshot of a task's services; unknown when nothing runs. */
    static OptionalInt online(List<ServiceCount> running) {
        if (running.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(running.stream().mapToInt(ServiceCount::online).sum());
    }

    /**
     * The players a task can hold over one snapshot: the sum of the services' limits, or unknown
     * unless every running service has announced its limit.
     */
    static OptionalInt capacity(List<ServiceCount> running) {
        if (running.isEmpty() || running.stream().anyMatch(service -> !service.limitAnnounced())) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(running.stream().mapToInt(ServiceCount::max).sum());
    }

    /**
     * The running services of a task: an empty list when the task runs nowhere, and an empty
     * Optional when they cannot be read, because there is no provider or the lookup failed.
     */
    Optional<List<ServiceCount>> services(String task) {
        if (this.counts.isEmpty() || !this.counts.get().supports(SourceType.TASK)) {
            return Optional.empty();
        }
        try {
            return Optional.of(this.counts.get().running(SourceType.TASK, task));
        } catch (RuntimeException e) {
            LOGGER.debug("Reading the services of task '{}' failed", task, e);
            return Optional.empty();
        }
    }
}
