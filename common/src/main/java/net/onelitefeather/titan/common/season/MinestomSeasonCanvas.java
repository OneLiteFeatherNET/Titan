/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.season;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;

/**
 * The {@link SeasonCanvas} the lobby actually runs on: one Minestom {@link Instance}.
 *
 * <p>Scheduling goes through the instance's own scheduler rather than the server's. That is not a
 * detail — an instance scheduler dies with the instance, so a season whose world is unloaded
 * cannot keep a task alive against a world that no longer exists.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class MinestomSeasonCanvas implements SeasonCanvas {

    private final Instance instance;

    private MinestomSeasonCanvas(Instance instance) {
        this.instance = instance;
    }

    /**
     * Creates a canvas painting on the given instance.
     *
     * @param instance the lobby world seasons change
     * @return the canvas
     */
    @Contract(value = "_ -> new", pure = true)
    public static MinestomSeasonCanvas of(Instance instance) {
        return new MinestomSeasonCanvas(instance);
    }

    @Override
    public Block blockAt(Point position) {
        // Reading an unloaded chunk throws, and a season is applied at startup, before anybody has
        // walked anywhere - so the chunk holding a decoration is routinely not loaded yet. Load it
        // rather than let the season fail on the first position outside the spawn chunks.
        ensureLoaded(position);
        return this.instance.getBlock(position);
    }

    @Override
    public void setBlock(Point position, Block block) {
        ensureLoaded(position);
        this.instance.setBlock(position, block);
    }

    private void ensureLoaded(Point position) {
        if (!ChunkUtils.isLoaded(this.instance, position)) {
            this.instance.loadChunk(position).join();
        }
    }

    @Override
    public UUID spawnDisplay(Pos position, Component text) {
        // Same reason as blockAt: an entity put into an unloaded chunk is not in the world yet, and
        // a season is applied before anybody has walked anywhere.
        ensureLoaded(position);
        Entity display = new Entity(EntityType.TEXT_DISPLAY);
        display.editEntityMeta(TextDisplayMeta.class, meta -> {
            meta.setText(text);
            meta.setHasNoGravity(true);
            meta.setBillboardRenderConstraints(TextDisplayMeta.BillboardConstraints.CENTER);
        });
        display.setInstance(this.instance, position);
        return display.getUuid();
    }

    @Override
    public void removeDisplay(UUID displayId) {
        Entity display = this.instance.getEntityByUuid(displayId);
        if (display != null) {
            display.remove();
        }
    }

    @Override
    public void playSound(Pos position, Key sound) {
        this.instance.playSound(Sound.sound(sound, Sound.Source.AMBIENT, 1.0f, 1.0f), position.x(), position.y(), position.z());
    }

    @Override
    public Component prefix() {
        return SeasonPrefix.current();
    }

    @Override
    public void prefix(@Nullable Component prefix) {
        SeasonPrefix.current(prefix);
    }

    @Override
    public Handle schedule(Duration period, Runnable action) {
        TaskSchedule schedule = TaskSchedule.duration(period);
        return new TaskHandle(this.instance.scheduler().scheduleTask(action, schedule, schedule));
    }

    /** A Minestom {@link Task} seen through the narrow window a season needs. */
    private record TaskHandle(@Nullable Task task) implements Handle {

        @Override
        public void cancel() {
            if (this.task != null) {
                this.task.cancel();
            }
        }

        @Override
        public boolean alive() {
            return this.task != null && this.task.isAlive();
        }
    }
}
