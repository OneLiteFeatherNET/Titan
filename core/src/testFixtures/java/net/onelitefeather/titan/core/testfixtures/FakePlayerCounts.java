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
package net.onelitefeather.titan.core.testfixtures;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.portal.SourceType;

/**
 * A test-only {@link PlayerCounts} that is not CloudNet: it answers from what a test serves and
 * counts its reads. A source that was never served is not running. Services are served by source
 * name and answer for every {@link SourceType}; {@link #set(SourceType, String, PlayerCount)}
 * pins the count of one source instead of summing its services.
 */
public final class FakePlayerCounts implements PlayerCounts {

    private record Key(SourceType type, String name) {
    }

    private final Map<Key, PlayerCount> counts = new HashMap<>();
    private final Map<String, List<ServiceCount>> services = new HashMap<>();
    private final Set<SourceType> supported = EnumSet.allOf(SourceType.class);
    private RuntimeException failure;
    private int reads;

    /** One running service of {@code task} with {@code online} players. */
    public FakePlayerCounts serve(String task, int online) {
        return serveServices(task, new ServiceCount(task + "-1", online, 100));
    }

    /** The running services of the source named {@code name}. */
    public FakePlayerCounts serveServices(String name, ServiceCount... running) {
        this.services.put(name, List.of(running));
        return this;
    }

    /** The count of one source, whatever services are served for it. */
    public FakePlayerCounts set(SourceType type, String name, PlayerCount count) {
        this.counts.put(new Key(type, name), count);
        return this;
    }

    public FakePlayerCounts doNotSupport(SourceType type) {
        this.supported.remove(type);
        return this;
    }

    /** Makes every following read throw {@code failure}. */
    public FakePlayerCounts failWith(RuntimeException failure) {
        this.failure = failure;
        return this;
    }

    /** How often a count or the running services were read. */
    public int reads() {
        return this.reads;
    }

    @Override
    public boolean supports(SourceType type) {
        return this.supported.contains(type);
    }

    @Override
    public PlayerCount count(SourceType type, String name) {
        read();
        PlayerCount pinned = this.counts.get(new Key(type, name));
        if (pinned != null) {
            return pinned;
        }
        List<ServiceCount> running = servicesOf(name);
        if (running.isEmpty()) {
            return PlayerCount.NOT_RUNNING;
        }
        return new PlayerCount(running.stream().mapToInt(ServiceCount::online).sum(), running.stream().mapToInt(ServiceCount::max).sum(), true);
    }

    @Override
    public List<ServiceCount> running(SourceType type, String name) {
        read();
        return servicesOf(name);
    }

    private void read() {
        this.reads++;
        if (this.failure != null) {
            throw this.failure;
        }
    }

    private List<ServiceCount> servicesOf(String name) {
        return this.services.getOrDefault(name, List.of());
    }
}
