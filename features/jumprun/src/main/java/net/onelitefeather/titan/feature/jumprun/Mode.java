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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * How hard a run is: when each shape appears, how steeply the difficulty climbs, how wide a gap may
 * be and how much an ascent weighs in the cost of a jump. The names are the same in every language,
 * so one text display serves all viewers.
 */
enum Mode {
    EASY("Easy", NamedTextColor.GREEN, 160.0, 2, 2, 3.0, Map.of(Surface.FULL, 0, Surface.SLAB, 10)), MEDIUM("Medium", NamedTextColor.YELLOW, 80.0, Jump.MAX_GAP, Jump.MAX_GAP_ASCENT, 1.0, Map.of(Surface.FULL, 0, Surface.TRAPDOOR, 10, Surface.SLAB, 10, Surface.FENCE, 25, Surface.PANE, 25, Surface.POST, 40)), HARD("Hard", NamedTextColor.RED, 40.0, Jump.MAX_GAP, Jump.MAX_GAP_ASCENT, 1.0, Map.of(Surface.FULL, 0, Surface.TRAPDOOR, 5, Surface.SLAB, 5, Surface.FENCE, 10, Surface.PANE, 10, Surface.POST, 20));

    private final String displayName;
    private final TextColor color;
    private final double scale;
    private final int maxGap;
    private final int maxGapAscent;
    private final double ascentWeight;
    private final Map<Surface, Integer> unlocks;

    Mode(String displayName, TextColor color, double scale, int maxGap, int maxGapAscent, double ascentWeight, Map<Surface, Integer> unlocks) {
        this.displayName = displayName;
        this.color = color;
        this.scale = scale;
        this.maxGap = maxGap;
        this.maxGapAscent = maxGapAscent;
        this.ascentWeight = ascentWeight;
        this.unlocks = unlocks;
    }

    /** The mode that follows in the cycle Easy, Medium, Hard, and back to Easy. */
    Mode next() {
        Mode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /** The coloured name, as shown in the head label and the messages. */
    Component label() {
        return Component.text(displayName, color);
    }

    /** Score at which the level has climbed to 1 - 1/e; a larger value flattens the curve. */
    double scale() {
        return scale;
    }

    /** Widest gap of a jump that does not climb. */
    int maxGap() {
        return maxGap;
    }

    /** Widest gap of a jump that climbs. */
    int maxGapAscent() {
        return maxGapAscent;
    }

    /** What a climbing jump adds to its cost. */
    double ascentWeight() {
        return ascentWeight;
    }

    /** The score from which the shape may appear; {@link Integer#MAX_VALUE} when it never does. */
    int minScore(Surface surface) {
        return unlocks.getOrDefault(surface, Integer.MAX_VALUE);
    }

    /** The shapes that may appear once the score has reached {@code score}. */
    List<Surface> unlockedAt(int score) {
        return Arrays.stream(Surface.values()).filter(surface -> minScore(surface) <= score).toList();
    }
}
