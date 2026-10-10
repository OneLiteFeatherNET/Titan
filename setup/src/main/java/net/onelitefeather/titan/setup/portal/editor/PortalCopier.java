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
package net.onelitefeather.titan.setup.portal.editor;

import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.setup.portal.PortalSourceException;
import net.onelitefeather.titan.setup.portal.PortalSources;
import net.onelitefeather.titan.setup.portal.PortalStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns the portals of another world into the player's drafts. Nothing is saved here: the copies
 * wait as drafts until {@code save} or {@code save-all} writes them.
 */
public final class PortalCopier {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalCopier.class);

    private final PortalEditor editor;
    private final PortalSources sources;
    private final PortalStore store;

    public PortalCopier(PortalEditor editor, PortalSources sources, PortalStore store) {
        this.editor = editor;
        this.sources = sources;
        this.store = store;
    }

    public CopyResult copy(UUID player, String world) {
        if (world.equalsIgnoreCase(sources.active())) {
            return new CopyResult.SameWorld(world);
        }
        List<String> available = sources.worlds();
        if (!available.contains(world)) {
            return new CopyResult.UnknownWorld(world, available);
        }
        Optional<List<Portal>> source;
        try {
            source = sources.portalsOf(world);
        } catch (PortalSourceException exception) {
            LOGGER.warn("Cannot read the portals of world {}", world, exception);
            return new CopyResult.Unreadable(world, exception.getMessage());
        }
        if (source.isEmpty()) {
            return new CopyResult.UnknownWorld(world, available);
        }
        if (source.get().isEmpty()) {
            return new CopyResult.NothingToCopy(world);
        }
        return openDrafts(player, world, source.get());
    }

    private CopyResult openDrafts(UUID player, String world, List<Portal> source) {
        List<String> saved = store.portals().stream().map(Portal::id).toList();
        List<Portal> adopted = new ArrayList<>();
        List<String> added = new ArrayList<>();
        List<String> replacing = new ArrayList<>();
        List<SkippedPortal> skipped = new ArrayList<>();
        for (Portal portal : source) {
            Optional<String> refusal = editor.adopt(player, portal);
            if (refusal.isPresent()) {
                skipped.add(new SkippedPortal(portal.id(), refusal.get()));
                continue;
            }
            adopted.add(portal);
            (saved.contains(portal.id()) ? replacing : added).add(portal.id());
        }
        LOGGER.info("Copied {} portals from world {} into drafts in world {}", adopted.size(), world, store.world());
        return new CopyResult.Copied(world, adopted, added, replacing, skipped);
    }
}
