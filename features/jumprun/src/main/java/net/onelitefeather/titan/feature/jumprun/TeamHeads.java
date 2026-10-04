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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.minestom.server.entity.PlayerSkin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns the configured UUIDs into skins. A lookup runs on the executor, never in the caller, and
 * its result stays in memory; until it is there the head of that player is simply not offered. A
 * UUID that could not be resolved is skipped with a warning (once) and tried again on the next
 * read.
 */
final class TeamHeads {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeamHeads.class);

    private final HeadSkins skins;
    private final Executor lookups;
    private final Map<UUID, HeadSkin> resolved = new ConcurrentHashMap<>();
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
    private final Set<UUID> reported = ConcurrentHashMap.newKeySet();

    TeamHeads(HeadSkins skins, Executor lookups) {
        this.skins = skins;
        this.lookups = lookups;
    }

    /** The skins found so far for {@code ids}, in the order of the list. */
    List<HeadSkin> of(List<UUID> ids) {
        List<HeadSkin> found = new ArrayList<>();
        for (UUID id : ids) {
            if (!resolved.containsKey(id) && pending.add(id)) {
                lookups.execute(() -> lookUp(id));
            }
            Optional.ofNullable(resolved.get(id)).ifPresent(found::add);
        }
        return found;
    }

    private void lookUp(UUID id) {
        try {
            skins.skinOf(id).ifPresentOrElse(skin -> accept(id, skin), () -> skip(id, "it has no skin"));
        } catch (RuntimeException e) {
            skip(id, String.valueOf(e.getMessage()));
        } finally {
            pending.remove(id);
        }
    }

    private void accept(UUID id, PlayerSkin skin) {
        resolved.put(id, new HeadSkin(id, skin.textures(), skin.signature()));
        reported.remove(id);
    }

    private void skip(UUID id, String reason) {
        if (reported.add(id)) {
            LOGGER.warn("Skipping team head {}: {}", id, reason);
        }
    }
}
