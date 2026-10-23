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
package net.onelitefeather.titan.feature.npc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.portal.SourceType;

/** A provider with fixed tasks; a task that was never set runs nowhere. */
final class FakePlayerCounts implements PlayerCounts {

    private final Map<String, List<ServiceCount>> tasks = new HashMap<>();
    private RuntimeException failure;

    FakePlayerCounts task(String name, ServiceCount... services) {
        this.tasks.put(name, List.of(services));
        return this;
    }

    FakePlayerCounts failingWith(RuntimeException failure) {
        this.failure = failure;
        return this;
    }

    @Override
    public PlayerCount count(SourceType type, String name) {
        if (this.failure != null) {
            throw this.failure;
        }
        List<ServiceCount> services = this.tasks.getOrDefault(name, List.of());
        int online = services.stream().mapToInt(ServiceCount::online).sum();
        int max = services.stream().mapToInt(ServiceCount::max).sum();
        return new PlayerCount(online, max, !services.isEmpty());
    }

    @Override
    public List<ServiceCount> running(SourceType type, String name) {
        if (this.failure != null) {
            throw this.failure;
        }
        return this.tasks.getOrDefault(name, List.of());
    }
}
