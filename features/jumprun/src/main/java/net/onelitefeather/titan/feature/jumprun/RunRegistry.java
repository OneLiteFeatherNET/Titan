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
package net.onelitefeather.titan.feature.jumprun;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;

/** The running courses by player; a player has at most one. */
final class RunRegistry {

    private final Map<UUID, Run> runs = new ConcurrentHashMap<>();

    void add(Run run) {
        runs.put(run.player().getUuid(), run);
    }

    @Nullable
    Run get(UUID player) {
        return runs.get(player);
    }

    /** Whether this very run was still registered, so only one caller ends it. */
    boolean remove(Run run) {
        return runs.remove(run.player().getUuid(), run);
    }

    List<Run> all() {
        return List.copyOf(runs.values());
    }
}
