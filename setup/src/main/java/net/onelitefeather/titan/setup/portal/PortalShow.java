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
package net.onelitefeather.titan.setup.portal;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.core.portal.Portal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code /setup portal show}: outlines every saved portal to the executing player for a few
 * seconds. One run per player, so a second show replaces the first.
 */
public final class PortalShow {

    /** 32 runs every 5 ticks are 8 seconds. */
    static final int RUNS = 32;

    private final Map<UUID, Task> tasks = new ConcurrentHashMap<>();

    /** Starts the show; false, without a task, when there is no portal to show. */
    public boolean start(Player player, List<Portal> portals) {
        UUID uuid = player.getUuid();
        stop(uuid);
        List<Vec> points = new ArrayList<>();
        portals.forEach(portal -> points.addAll(PortalOutline.points(portal.shape())));
        if (points.isEmpty()) {
            return false;
        }
        int[] runs = {0};
        Task[] self = new Task[1];
        // Returning stop() ends the task by itself, so nothing lingers on the player's scheduler.
        self[0] = player.scheduler().submitTask(() -> {
            OutlineParticles.send(player, points);
            if (++runs[0] >= RUNS) {
                tasks.remove(uuid, self[0]);
                return TaskSchedule.stop();
            }
            return TaskSchedule.tick(OutlineParticles.INTERVAL_TICKS);
        });
        tasks.put(uuid, self[0]);
        return true;
    }

    public void stop(UUID player) {
        Task task = tasks.remove(player);
        if (task != null) {
            task.cancel();
        }
    }

    /** Running shows, for tests that check nothing leaks. */
    public int running() {
        return tasks.size();
    }
}
