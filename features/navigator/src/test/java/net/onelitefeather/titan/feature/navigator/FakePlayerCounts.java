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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.portal.SourceType;
import org.jetbrains.annotations.NotNull;

/** Answers task counts and running services from maps; a task it does not know is not running. */
final class FakePlayerCounts implements PlayerCounts {

    private final Map<String, PlayerCount> tasks = new HashMap<>();
    private final Map<String, List<ServiceCount>> services = new HashMap<>();
    private boolean supportsTasks = true;
    private RuntimeException failure;

    void serve(String task, int online) {
        this.tasks.put(task, new PlayerCount(online, 100, true));
    }

    /** The running services of {@code task}, as the provider lists them. */
    void serveServices(String task, ServiceCount... running) {
        this.services.put(task, List.of(running));
    }

    void doNotSupportTasks() {
        this.supportsTasks = false;
    }

    void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    @Override
    public boolean supports(@NotNull SourceType type) {
        return type != SourceType.TASK || this.supportsTasks;
    }

    @Override
    public @NotNull PlayerCount count(@NotNull SourceType type, @NotNull String name) {
        if (this.failure != null) {
            throw this.failure;
        }
        return this.tasks.getOrDefault(name, PlayerCount.NOT_RUNNING);
    }

    @Override
    public @NotNull List<ServiceCount> running(@NotNull SourceType type, @NotNull String name) {
        if (this.failure != null) {
            throw this.failure;
        }
        return this.services.getOrDefault(name, List.of());
    }
}
