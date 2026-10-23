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
package net.onelitefeather.titan.core.portal;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The rule every entry point of a CloudNet task uses to choose a service to join: the navigator's
 * left click and the join NPCs. A service is joinable when it has announced its limit and is not
 * full; the quick join takes the joinable one with the most players, so a round fills before a new
 * one starts, and a tie goes to the lowest name, which keeps the choice the same for the same
 * snapshot.
 */
public final class TaskJoin {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskJoin.class);
    private static final Comparator<ServiceCount> FULLEST_THEN_NAME = Comparator.comparingInt(ServiceCount::online).reversed().thenComparing(ServiceCount::name);

    private TaskJoin() {
    }

    /** Whether a service can be joined: it has announced a limit and has a free slot. */
    public static boolean joinable(ServiceCount service) {
        return service.limitAnnounced() && !service.full();
    }

    /** The service to join, or nothing when no running service is joinable. */
    public static Optional<ServiceCount> pick(List<ServiceCount> running) {
        return running.stream().filter(TaskJoin::joinable).min(FULLEST_THEN_NAME);
    }

    /**
     * The running services of a task: an empty list when the task runs nowhere, and an empty
     * Optional when they cannot be read, because there is no provider or the lookup failed.
     */
    public static Optional<List<ServiceCount>> services(Optional<PlayerCounts> counts, String task) {
        if (counts.isEmpty() || !counts.get().supports(SourceType.TASK)) {
            return Optional.empty();
        }
        try {
            return Optional.of(counts.get().running(SourceType.TASK, task));
        } catch (RuntimeException e) {
            LOGGER.debug("Reading the services of task '{}' failed", task, e);
            return Optional.empty();
        }
    }
}
