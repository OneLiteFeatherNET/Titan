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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * A season built from its file, and the only implementation of {@link SeasonalContent} there is.
 *
 * <p>There is deliberately no second one. A season that needed its own class would need its own
 * deployment, its own review and its own way of cleaning up after itself — which is exactly the
 * cost that makes a purely decorative season a net loss. Adding a season here is adding a file.
 *
 * <p>The undo stack is the whole of {@link #deactivate()}. Each effect, as it is applied, pushes
 * the action that takes it back; deactivation pops them. Two consequences fall out of that and both
 * are wanted: the undo is written next to the change it undoes rather than in a second method that
 * can drift out of sync, and it restores what was actually at a position rather than what the
 * author assumed was there.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class ConfiguredSeason implements SeasonalContent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfiguredSeason.class);

    private final SeasonDefinition definition;
    private final Deque<Runnable> undo = new ArrayDeque<>();

    private boolean active;

    private ConfiguredSeason(SeasonDefinition definition) {
        this.definition = definition;
    }

    /**
     * Creates the content described by a season file.
     *
     * @param definition the season as read from its file
     * @return the content
     */
    @Contract(value = "_ -> new", pure = true)
    public static ConfiguredSeason of(SeasonDefinition definition) {
        return new ConfiguredSeason(definition);
    }

    /**
     * Returns the season this content was built from.
     *
     * @return the definition
     */
    @Contract(pure = true)
    public SeasonDefinition definition() {
        return this.definition;
    }

    @Override
    public String id() {
        return this.definition.id();
    }

    @Override
    public int priority() {
        return this.definition.priority();
    }

    @Override
    public boolean active() {
        return this.active;
    }

    @Override
    public void activate(SeasonCanvas canvas) {
        if (this.active) {
            return;
        }
        this.active = true;
        for (SeasonEffect effect : this.definition.effects(SeasonEffect.Scope.WORLD)) {
            apply(canvas, effect);
        }
        LOGGER.info("Season {} activated with {} world effect(s)", id(), this.undo.size());
    }

    @Override
    public void deactivate() {
        if (!this.active) {
            return;
        }
        int taken = this.undo.size();
        while (!this.undo.isEmpty()) {
            Runnable step = this.undo.pop();
            try {
                step.run();
            } catch (RuntimeException exception) {
                // Keep unwinding. One step that cannot be taken back is a leftover; stopping here
                // would turn it into every remaining step being a leftover as well.
                LOGGER.error("Season {} could not undo one of its changes; continuing with the rest", id(), exception);
            }
        }
        this.active = false;
        LOGGER.info("Season {} deactivated, {} change(s) taken back", id(), taken);
    }

    /**
     * Applies one world effect and pushes the step that takes it back.
     *
     * <p>The switch is exhaustive over the sealed {@link SeasonEffect} hierarchy and has no
     * {@code default}: an effect type that nobody has decided how to undo does not compile
     * (US-4.04).
     */
    private void apply(SeasonCanvas canvas, SeasonEffect effect) {
        switch (effect) {
            case SeasonEffect.PlaceDecoration decoration -> {
                Pos position = decoration.position();
                // Read first, then write: the undo puts back whatever was actually there, which is
                // not necessarily air and is not necessarily what the season author expected.
                Block previous = canvas.blockAt(position);
                Block block = Block.fromKey(decoration.block());
                if (block == null) {
                    // Unreachable through SeasonLoader, which resolves every key while reading the
                    // file. Kept as a guard for a definition built in code.
                    throw new IllegalStateException("season " + id() + " places the unknown block " + decoration.block().asString());
                }
                canvas.setBlock(position, block);
                this.undo.push(() -> canvas.setBlock(position, previous));
            }
            case SeasonEffect.PlaceDisplay display -> {
                UUID id = canvas.spawnDisplay(display.position(), MiniMessage.miniMessage().deserialize(display.text()));
                this.undo.push(() -> canvas.removeDisplay(id));
            }
            case SeasonEffect.AmbientSound sound -> {
                SeasonCanvas.Handle handle = canvas.schedule(Duration.ofSeconds(sound.periodSeconds()), () -> canvas.playSound(sound.position(), sound.sound()));
                this.undo.push(handle::cancel);
            }
            case SeasonEffect.MessagePrefix messagePrefix -> {
                Component previous = canvas.prefix();
                canvas.prefix(MiniMessage.miniMessage().deserialize(messagePrefix.prefix()));
                this.undo.push(() -> canvas.prefix(previous));
            }
            // A player-scoped effect never touches the world, so there is nothing to place and
            // nothing to take back. It is answered per viewer by SeasonPresentation, which is also
            // what lets a preview holder see it outside the window.
            case SeasonEffect.ReplaceIcon ignored -> {
            }
        }
    }
}
