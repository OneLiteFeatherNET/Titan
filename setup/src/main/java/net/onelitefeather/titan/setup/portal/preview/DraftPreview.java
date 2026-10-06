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
package net.onelitefeather.titan.setup.portal.preview;

import net.minestom.server.entity.Player;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.setup.portal.editor.PortalDraft;
import net.onelitefeather.titan.setup.portal.editor.PortalEditor;
import net.onelitefeather.titan.setup.portal.editor.PortalMessages;


import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Live particle preview of the draft a player is working on: one task per player, following
 * the player's position, ended when the draft ends. Each run reads the draft afresh, so it never
 * shows stale state.
 */
public final class DraftPreview {

    private record Running(String id, Task task) {
    }

    private final PortalEditor editor;
    private final Map<UUID, Running> running = new ConcurrentHashMap<>();

    public DraftPreview(PortalEditor editor) {
        this.editor = editor;
    }

    /** Previews draft {@code id} for the player, replacing the preview they had. */
    public void start(Player player, String id) {
        UUID uuid = player.getUuid();
        Running current = running.get(uuid);
        if (current != null && current.id().equals(id)) {
            // Every edit calls start; keeping the task also keeps its one-time hint quiet.
            return;
        }
        stop(uuid);
        boolean[] hinted = {false};
        Task[] self = new Task[1];
        self[0] = player.scheduler().submitTask(() -> {
            Optional<PortalDraft> draft = editor.drafts(uuid).stream().filter(candidate -> id.equals(candidate.id())).findFirst();
            if (draft.isEmpty() || !player.isOnline()) {
                running.computeIfPresent(uuid, (key, entry) -> entry.task() == self[0] ? null : entry);
                return TaskSchedule.stop();
            }
            // Once per switch into the default-radius state, not on every run.
            boolean defaultRadius = DraftPoints.usesDefaultRadius(draft.get());
            if (defaultRadius && !hinted[0]) {
                player.sendMessage(PortalMessages.defaultRadiusHint());
            }
            hinted[0] = defaultRadius;
            OutlineParticles.send(player, DraftPoints.of(draft.get(), player.getPosition(), player.getEyeHeight()));
            return TaskSchedule.tick(OutlineParticles.INTERVAL_TICKS);
        });
        running.put(uuid, new Running(id, self[0]));
    }

    /** Ends the player's preview, whichever draft it shows. */
    public void stop(UUID player) {
        Running current = running.remove(player);
        if (current != null) {
            current.task().cancel();
        }
    }

    /** Ends the preview only if it shows draft {@code id}; a preview of another draft goes on. */
    public void stop(UUID player, String id) {
        Running current = running.get(player);
        if (current != null && current.id().equals(id)) {
            stop(player);
        }
    }

    /** Running previews, for tests that check nothing leaks. */
    public int running() {
        return running.size();
    }
}
