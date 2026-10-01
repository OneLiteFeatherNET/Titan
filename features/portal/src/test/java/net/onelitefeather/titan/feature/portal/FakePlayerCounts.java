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
package net.onelitefeather.titan.feature.portal;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.SourceType;
import org.jetbrains.annotations.NotNull;

/** A test-only provider that is not CloudNet: it answers from a map and counts its reads. */
final class FakePlayerCounts implements PlayerCounts {

    private record Key(SourceType type, String name) {
    }

    private final Map<Key, PlayerCount> counts = new HashMap<>();
    private final Set<SourceType> supported = EnumSet.allOf(SourceType.class);
    private int reads;

    void set(SourceType type, String name, PlayerCount count) {
        this.counts.put(new Key(type, name), count);
    }

    void doNotSupport(SourceType type) {
        this.supported.remove(type);
    }

    int reads() {
        return this.reads;
    }

    @Override
    public boolean supports(@NotNull SourceType type) {
        return this.supported.contains(type);
    }

    @Override
    public @NotNull PlayerCount count(@NotNull SourceType type, @NotNull String name) {
        this.reads++;
        return this.counts.getOrDefault(new Key(type, name), PlayerCount.NOT_RUNNING);
    }
}
