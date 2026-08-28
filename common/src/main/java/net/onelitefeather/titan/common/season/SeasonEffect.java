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
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * One thing a season does. The hierarchy is sealed on purpose: it is the mechanism behind US-4.04.
 *
 * <p>Two different failures are caught by two different means, and both matter:
 *
 * <ul>
 * <li>A season file naming an effect type nobody implements is rejected by {@link SeasonLoader}
 * with a message naming the type — at load, not at the moment a player would have seen it.</li>
 * <li>An effect type that exists but that some part of the code forgot to handle does not compile.
 * Every {@code switch} over this interface is exhaustive and has no {@code default}; adding a
 * record here breaks the build until {@link ConfiguredSeason} and {@link SeasonPresentation}
 * have said what to do with it.</li>
 * </ul>
 *
 * <p>The effects are deliberately modest — decoration, a display, an ambient sound, an item swap,
 * a message prefix. That is not a placeholder for something richer later: it is the line between a
 * value and a verb. Anything that needs to decide something at runtime is a verb and belongs in
 * Java, not in a season file.
 *
 * <p>{@link #scope()} splits them into the two kinds that behave differently and must not be
 * conflated. A {@link Scope#WORLD} effect is in the shared world or it is not — it cannot be shown
 * to one player. A {@link Scope#PLAYER} effect is computed per viewer, which is what lets a
 * preview holder see it early (US-4.07).
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public sealed interface SeasonEffect {

    /**
     * Returns the type id this effect is written as in a season file.
     *
     * @return the effect type
     */
    @Contract(pure = true)
    Type type();

    /**
     * Returns whether this effect changes the shared world or is computed per viewer.
     *
     * @return the scope of this effect
     */
    @Contract(pure = true)
    default Scope scope() {
        return type().scope();
    }

    /**
     * Whether an effect changes the world everybody shares or is answered per player.
     *
     * <p>The distinction is not a taxonomy, it is the reason preview cannot be uniform: a block is
     * placed once for the whole lobby, so no permission can hide it from anyone, while a navigator
     * icon is chosen while the menu is being drawn and can therefore differ per viewer.
     *
     * @author TheMeinerLP
     * @version 1.0.0
     * @since 1.15.0
     */
    enum Scope {

        /** Changes the shared world; applied once, taken back once, seen by everybody. */
        WORLD,

        /** Computed while something is shown to one player; never written to the world. */
        PLAYER
    }

    /**
     * The effect types a season file may name, and the record each one deserialises into.
     *
     * <p>This enum is the only place a JSON string is turned into a Java type. Everything else
     * switches over the sealed interface and is therefore checked by the compiler.
     *
     * @author TheMeinerLP
     * @version 1.0.0
     * @since 1.15.0
     */
    enum Type {

        /** {@link PlaceDecoration}. */
        PLACE_DECORATION("place_decoration", PlaceDecoration.class, Scope.WORLD),

        /** {@link PlaceDisplay}. */
        PLACE_DISPLAY("place_display", PlaceDisplay.class, Scope.WORLD),

        /** {@link AmbientSound}. */
        AMBIENT_SOUND("ambient_sound", AmbientSound.class, Scope.WORLD),

        /** {@link ReplaceIcon}. */
        REPLACE_ICON("replace_icon", ReplaceIcon.class, Scope.PLAYER),

        /** {@link MessagePrefix}. */
        MESSAGE_PREFIX("message_prefix", MessagePrefix.class, Scope.WORLD);

        private final String id;
        private final Class<? extends SeasonEffect> effectClass;
        private final Scope scope;

        Type(String id, Class<? extends SeasonEffect> effectClass, Scope scope) {
            this.id = id;
            this.effectClass = effectClass;
            this.scope = scope;
        }

        /**
         * Resolves the type written in a season file.
         *
         * @param id the {@code type} value from the file, may be {@code null}
         * @return the matching type, or an empty optional when the id is absent or unknown
         */
        @Contract(pure = true)
        public static Optional<Type> fromId(@Nullable String id) {
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            String normalized = id.trim().toLowerCase(Locale.ROOT);
            for (Type type : values()) {
                if (type.id.equals(normalized)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }

        /**
         * Lists every known type id, for the error message an unknown one produces.
         *
         * @return the known ids, comma separated and in declaration order
         */
        @Contract(pure = true)
        public static String knownIds() {
            return Arrays.stream(values()).map(Type::id).collect(Collectors.joining(", "));
        }

        /**
         * Returns the id used in a season file, for example {@code place_decoration}.
         *
         * @return the configured id of this type
         */
        @Contract(pure = true)
        public String id() {
            return this.id;
        }

        /**
         * Returns the record this type deserialises into.
         *
         * @return the effect class
         */
        @Contract(pure = true)
        public Class<? extends SeasonEffect> effectClass() {
            return this.effectClass;
        }

        /**
         * Returns whether effects of this type change the world or are computed per viewer.
         *
         * @return the scope of this type
         */
        @Contract(pure = true)
        public Scope scope() {
            return this.scope;
        }
    }

    /**
     * Puts a block into the world and remembers what was there before.
     *
     * @param position where the block goes
     * @param block    the block key, for example {@code minecraft:jack_o_lantern}
     * @author TheMeinerLP
     * @version 1.0.0
     * @since 1.15.0
     */
    record PlaceDecoration(Pos position, Key block) implements SeasonEffect {

        /**
         * Rejects a half-written effect at construction, so the loader can name the file it came
         * from rather than the lobby discovering it later.
         */
        public PlaceDecoration {
            if (position == null) {
                throw new IllegalArgumentException("place_decoration needs a position");
            }
            if (block == null) {
                throw new IllegalArgumentException("place_decoration needs a block");
            }
        }

        @Override
        public Type type() {
            return Type.PLACE_DECORATION;
        }
    }

    /**
     * Spawns a floating text display.
     *
     * @param position where the display floats
     * @param text     the text, in MiniMessage
     * @author TheMeinerLP
     * @version 1.0.0
     * @since 1.15.0
     */
    record PlaceDisplay(Pos position, String text) implements SeasonEffect {

        /**
         * Rejects a display without a position or without anything to say.
         */
        public PlaceDisplay {
            if (position == null) {
                throw new IllegalArgumentException("place_display needs a position");
            }
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("place_display needs a text");
            }
        }

        @Override
        public Type type() {
            return Type.PLACE_DISPLAY;
        }
    }

    /**
     * Plays a sound at a place, over and over, until the season ends.
     *
     * <p>This is the one effect that leaves a scheduled task behind, and therefore the one that
     * makes {@link SeasonalContent#deactivate()} more than a formality: a season that forgets it
     * keeps making noise into the next one.
     *
     * @param position      where the sound comes from
     * @param sound         the sound key, for example {@code minecraft:ambient.cave}
     * @param periodSeconds how many seconds pass between two plays, at least one
     * @author TheMeinerLP
     * @version 1.0.0
     * @since 1.15.0
     */
    record AmbientSound(Pos position, Key sound, long periodSeconds) implements SeasonEffect {

        /**
         * Rejects a sound loop that would spin every tick or never fire.
         */
        public AmbientSound {
            if (position == null) {
                throw new IllegalArgumentException("ambient_sound needs a position");
            }
            if (sound == null) {
                throw new IllegalArgumentException("ambient_sound needs a sound");
            }
            if (periodSeconds < 1) {
                throw new IllegalArgumentException("ambient_sound needs a periodSeconds of at least 1, got " + periodSeconds);
            }
        }

        @Override
        public Type type() {
            return Type.AMBIENT_SOUND;
        }
    }

    /**
     * Swaps the material of a navigator icon while the season runs.
     *
     * @param destination the navigator destination whose icon changes, for example {@code Survival}
     * @param material    the material key the icon takes on
     * @author TheMeinerLP
     * @version 1.0.0
     * @since 1.15.0
     */
    record ReplaceIcon(String destination, Key material) implements SeasonEffect {

        /**
         * Rejects an icon swap that names no destination or no material.
         */
        public ReplaceIcon {
            if (destination == null || destination.isBlank()) {
                throw new IllegalArgumentException("replace_icon needs a destination");
            }
            if (material == null) {
                throw new IllegalArgumentException("replace_icon needs a material");
            }
        }

        @Override
        public Type type() {
            return Type.REPLACE_ICON;
        }
    }

    /**
     * Replaces what the {@code <prefix>} tag resolves to while the season runs.
     *
     * <p>Scoped to the world rather than to a player, which is a statement about messages and not
     * about permissions: a prefix goes into text that is broadcast, so there is no viewer to
     * compute
     * it for. That is also why a preview holder does not get to see it early.
     *
     * @param prefix the prefix, in MiniMessage
     * @author TheMeinerLP
     * @version 1.0.0
     * @since 1.15.0
     */
    record MessagePrefix(String prefix) implements SeasonEffect {

        /**
         * Rejects an empty prefix, which would be an effect that does nothing.
         */
        public MessagePrefix {
            if (prefix == null || prefix.isBlank()) {
                throw new IllegalArgumentException("message_prefix needs a prefix");
            }
        }

        @Override
        public Type type() {
            return Type.MESSAGE_PREFIX;
        }
    }
}
