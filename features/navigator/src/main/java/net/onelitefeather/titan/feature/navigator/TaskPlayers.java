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

import java.util.Optional;
import java.util.OptionalInt;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The players on a CloudNet task, summed over its running services by the {@link PlayerCounts}
 * provider. Empty whenever the number is unknown: no provider, a task that is not running, or a
 * lookup that failed.
 */
final class TaskPlayers {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskPlayers.class);

    private final Optional<PlayerCounts> counts;

    TaskPlayers(Optional<PlayerCounts> counts) {
        this.counts = counts;
    }

    OptionalInt online(String task) {
        if (this.counts.isEmpty() || !this.counts.get().supports(SourceType.TASK)) {
            return OptionalInt.empty();
        }
        try {
            PlayerCount count = this.counts.get().count(SourceType.TASK, task);
            return count.running() ? OptionalInt.of(count.online()) : OptionalInt.empty();
        } catch (RuntimeException e) {
            LOGGER.debug("Reading the players of task '{}' failed", task, e);
            return OptionalInt.empty();
        }
    }
}
