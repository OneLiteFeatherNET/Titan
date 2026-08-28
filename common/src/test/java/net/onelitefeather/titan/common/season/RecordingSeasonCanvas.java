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
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A real {@link SeasonCanvas} with a notebook.
 *
 * <p>It delegates everything to a {@link MinestomSeasonCanvas} over a running instance — the blocks
 * are real blocks, the displays real entities, the handles real Minestom tasks — and only remembers
 * what it handed out, so a test can afterwards inspect the state of those very objects. It stands
 * in for nothing; a test using it is still asserting against the world.
 */
final class RecordingSeasonCanvas implements SeasonCanvas {

    private final SeasonCanvas delegate;
    private final List<UUID> displays = new ArrayList<>();
    private final List<Handle> handles = new ArrayList<>();

    RecordingSeasonCanvas(SeasonCanvas delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns the ids of every display spawned through this canvas, in order.
     *
     * @return the display ids
     */
    List<UUID> displays() {
        return List.copyOf(this.displays);
    }

    /**
     * Returns the handles of every task scheduled through this canvas, in order.
     *
     * @return the task handles
     */
    List<Handle> handles() {
        return List.copyOf(this.handles);
    }

    @Override
    public Block blockAt(Point position) {
        return this.delegate.blockAt(position);
    }

    @Override
    public void setBlock(Point position, Block block) {
        this.delegate.setBlock(position, block);
    }

    @Override
    public UUID spawnDisplay(Pos position, Component text) {
        UUID id = this.delegate.spawnDisplay(position, text);
        this.displays.add(id);
        return id;
    }

    @Override
    public void removeDisplay(UUID displayId) {
        this.delegate.removeDisplay(displayId);
    }

    @Override
    public void playSound(Pos position, Key sound) {
        this.delegate.playSound(position, sound);
    }

    @Override
    public Component prefix() {
        return this.delegate.prefix();
    }

    @Override
    public void prefix(Component prefix) {
        this.delegate.prefix(prefix);
    }

    @Override
    public Handle schedule(Duration period, Runnable action) {
        Handle handle = this.delegate.schedule(period, action);
        this.handles.add(handle);
        return handle;
    }
}
