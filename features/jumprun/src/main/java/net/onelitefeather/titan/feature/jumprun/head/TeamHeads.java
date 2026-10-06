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
package net.onelitefeather.titan.feature.jumprun.head;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import net.minestom.server.entity.PlayerSkin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns the configured UUIDs into skins. A lookup runs on the executor under a timeout, never in
 * the caller, and its result stays in memory; until it is there the head of that player is simply
 * not offered. A UUID without a skin, or whose lookup failed or timed out, is skipped with a
 * warning (once) and not asked again before {@link #RETRY_AFTER}, so an outage at Mojang costs one
 * request per UUID and delay, not one per run.
 */
public final class TeamHeads {

    /** How long a UUID that could not be resolved is left alone. */
    public static final Duration RETRY_AFTER = Duration.ofMinutes(5);

    private static final Duration LOOKUP_TIMEOUT = Duration.ofSeconds(5);
    private static final Logger LOGGER = LoggerFactory.getLogger(TeamHeads.class);

    private final HeadSkins skins;
    private final Executor lookups;
    private final Clock clock;
    private final UnaryOperator<CompletableFuture<Optional<PlayerSkin>>> timeout;
    private final Map<UUID, HeadSkin> resolved = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> retryAt = new ConcurrentHashMap<>();
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
    private final Set<UUID> reported = ConcurrentHashMap.newKeySet();

    public TeamHeads(HeadSkins skins, Executor lookups, Clock clock) {
        this(skins, lookups, clock, lookup -> lookup.orTimeout(LOOKUP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    }

    /** @param timeout limits a lookup; it must complete it exceptionally when it takes too long */
    public TeamHeads(HeadSkins skins, Executor lookups, Clock clock, UnaryOperator<CompletableFuture<Optional<PlayerSkin>>> timeout) {
        this.skins = skins;
        this.lookups = lookups;
        this.clock = clock;
        this.timeout = timeout;
    }

    /** The skins found so far for {@code ids}, in the order of the list. */
    public List<HeadSkin> of(List<UUID> ids) {
        List<HeadSkin> found = new ArrayList<>();
        for (UUID id : ids) {
            if (isDueForLookup(id) && pending.add(id)) {
                lookUp(id);
            }
            Optional.ofNullable(resolved.get(id)).ifPresent(found::add);
        }
        return found;
    }

    private boolean isDueForLookup(UUID id) {
        return !resolved.containsKey(id) && !clock.instant().isBefore(retryAt.getOrDefault(id, Instant.MIN));
    }

    private void lookUp(UUID id) {
        timeout.apply(CompletableFuture.supplyAsync(() -> skins.skinOf(id), lookups)).whenComplete((skin, failure) -> {
            try {
                if (failure != null) {
                    skip(id, String.valueOf(failure.getCause() != null ? failure.getCause().getMessage() : failure.getMessage()));
                } else {
                    skin.ifPresentOrElse(found -> accept(id, found), () -> skip(id, "no skin"));
                }
            } finally {
                pending.remove(id);
            }
        });
    }

    private void accept(UUID id, PlayerSkin skin) {
        resolved.put(id, new HeadSkin(id, skin.textures(), skin.signature()));
        reported.remove(id);
    }

    private void skip(UUID id, String reason) {
        retryAt.put(id, clock.instant().plus(RETRY_AFTER));
        if (reported.add(id)) {
            LOGGER.warn("Skipping team head {}: no skin or the lookup failed ({})", id, reason);
        }
    }
}
