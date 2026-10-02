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

import io.avaje.inject.Secondary;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps records until the lobby stops; deliberately not cleared on disconnect. {@code @Secondary},
 * so a
 * persistent {@link RunRecords} bean takes its place.
 */
@Singleton
@Secondary
final class InMemoryRunRecords implements RunRecords {

    private final Map<UUID, Integer> best = new ConcurrentHashMap<>();

    @Override
    public OptionalInt best(UUID player) {
        Integer score = best.get(player);
        return score == null ? OptionalInt.empty() : OptionalInt.of(score);
    }

    @Override
    public boolean submit(UUID player, int score) {
        AtomicBoolean isRecord = new AtomicBoolean();
        best.compute(player, (_, previous) -> {
            isRecord.set(previous == null || score > previous);
            return isRecord.get() ? score : previous;
        });
        return isRecord.get();
    }
}
