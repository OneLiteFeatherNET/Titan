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
package net.onelitefeather.titan.feature.dance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the dances of all players: at most one per player, a pause between starts, a camera the
 * dancer looks through, and an NPC that dances in his place for that view. Every way a dance ends
 * goes through {@link #stop}, which hands the view back and removes camera and NPC even if a step
 * before has failed.
 */
final class DanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DanceService.class);

    /** What {@link #start} did with a request. */
    enum StartResult {
        STARTED, BUSY, COOLDOWN, NOT_PERMITTED, UNAVAILABLE
    }

    /** Why a dance ended; only for the debug log. */
    enum StopReason {
        FINISHED, MOVED, INPUT, INTERACTED, LEFT_INSTANCE, DISCONNECTED, STOP_BUTTON, FAILED, SHUTDOWN
    }

    private final Scheduler scheduler;
    private final DanceCooldown cooldown;
    private final DancePermissions permissions;
    private final DanceTelemetry telemetry;
    private final Function<LivingEntity, DanceEffects> effectsFor;
    private final Map<UUID, ActiveDance> active = new HashMap<>();

    DanceService(Scheduler scheduler, DanceCooldown cooldown, DancePermissions permissions, DanceTelemetry telemetry, Function<LivingEntity, DanceEffects> effectsFor) {
        this.scheduler = scheduler;
        this.cooldown = cooldown;
        this.permissions = permissions;
        this.telemetry = telemetry;
        this.effectsFor = effectsFor;
    }

    /**
     * Starts {@code dance} for {@code player} if nothing blocks it: no dance running, the
     * permission present, the pause over, and an instance to dance in.
     */
    StartResult start(Player player, Dance dance) {
        UUID id = player.getUuid();
        if (this.active.containsKey(id)) {
            return StartResult.BUSY;
        }
        if (!this.permissions.may(id, dance)) {
            return StartResult.NOT_PERMITTED;
        }
        if (!this.cooldown.ready(id)) {
            return StartResult.COOLDOWN;
        }
        Instance instance = player.getInstance();
        if (instance == null) {
            return StartResult.UNAVAILABLE;
        }
        Pos anchor = player.getPosition();
        DanceNpc npc = DanceNpc.spawn(player, instance, anchor);
        DanceCamera camera;
        try {
            camera = DanceCamera.open(player, instance, anchor);
        } catch (RuntimeException e) {
            npc.close();
            throw e;
        }
        DanceEffects effects = this.effectsFor.apply(npc.entity());
        ActiveDance started = new ActiveDance(new DanceSession(dance), effects, anchor, camera, npc);
        this.active.put(id, started);
        try {
            started.task = this.scheduler.submitTask(() -> tick(id, started));
        } catch (RuntimeException e) {
            stop(id, StopReason.FAILED);
            throw e;
        }
        this.cooldown.markStarted(id);
        this.telemetry.started(dance);
        LOGGER.debug("Dance {} started for {}", dance.id(), id);
        return StartResult.STARTED;
    }

    boolean isDancing(UUID playerId) {
        return this.active.containsKey(playerId);
    }

    /** Ends the dance of {@code playerId}, if one runs; a second call does nothing. */
    void stop(UUID playerId, StopReason reason) {
        ActiveDance dance = this.active.remove(playerId);
        if (dance == null) {
            return;
        }
        LOGGER.debug("Dance stopped for {}: {}", playerId, reason);
        try {
            if (dance.task != null) {
                dance.task.cancel();
            }
        } finally {
            endView(dance);
        }
    }

    /**
     * Stands the dancer and his NPC up, hands the view back and removes both, even if one step
     * fails.
     */
    private static void endView(ActiveDance dance) {
        try {
            dance.effects.reset(dance.anchor);
        } finally {
            try {
                dance.camera.close();
            } finally {
                dance.npc.close();
            }
        }
    }

    /** Ends the dance of {@code playerId} if he has moved beyond the threshold since it started. */
    void onMove(UUID playerId, Pos position) {
        ActiveDance dance = this.active.get(playerId);
        if (dance != null && DanceSession.movedBeyond(dance.anchor, position)) {
            stop(playerId, StopReason.MOVED);
        }
    }

    /** A player has left: ends his dance and forgets his pause. */
    void disconnected(UUID playerId) {
        stop(playerId, StopReason.DISCONNECTED);
        this.cooldown.forget(playerId);
    }

    /** Ends every running dance; used when the column shuts down. */
    void stopAll() {
        for (UUID playerId : new ArrayList<>(this.active.keySet())) {
            try {
                stop(playerId, StopReason.SHUTDOWN);
            } catch (RuntimeException e) {
                LOGGER.warn("Could not end the dance of {} cleanly on shutdown", playerId, e);
            }
        }
    }

    private TaskSchedule tick(UUID playerId, ActiveDance dance) {
        if (this.active.get(playerId) != dance) {
            return TaskSchedule.stop();
        }
        try {
            DanceSession.Frame frame = dance.session.advance();
            if (frame.finished()) {
                stop(playerId, StopReason.FINISHED);
                return TaskSchedule.stop();
            }
            for (DanceAction action : frame.actions()) {
                dance.effects.apply(action, dance.anchor);
            }
            return TaskSchedule.tick(1);
        } catch (RuntimeException e) {
            LOGGER.warn("A step of the dance of {} failed; stopping the dance", playerId, e);
            stop(playerId, StopReason.FAILED);
            return TaskSchedule.stop();
        }
    }

    /** The state of one running dance; only {@link DanceService} touches it. */
    private static final class ActiveDance {

        private final DanceSession session;
        private final DanceEffects effects;
        private final Pos anchor;
        private final DanceCamera camera;
        private final DanceNpc npc;
        private Task task;

        ActiveDance(DanceSession session, DanceEffects effects, Pos anchor, DanceCamera camera, DanceNpc npc) {
            this.session = session;
            this.effects = effects;
            this.anchor = anchor;
            this.camera = camera;
            this.npc = npc;
        }
    }
}
