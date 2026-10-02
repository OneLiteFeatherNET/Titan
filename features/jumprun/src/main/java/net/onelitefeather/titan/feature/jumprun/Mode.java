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
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * How hard a run is: when each shape appears, how steeply the difficulty climbs, how wide a gap may
 * be and how much an ascent weighs in the cost of a jump; and what happens to the blocks while the
 * runner stands (see {@link Reroll}). The names are the same in every language, so one text
 * display serves all viewers.
 */
enum Mode {
    EASY(Component.text("Easy", NamedTextColor.GREEN), Params.EASY, Reroll.NONE, true), MEDIUM(Component.text("Medium", NamedTextColor.YELLOW), Params.MEDIUM, Reroll.NONE, true), HARD(Component.text("Hard", NamedTextColor.RED), Params.HARD, Reroll.NONE, true), RAINBOW(MiniMessage.miniMessage().deserialize("<rainbow>Rainbow</rainbow>"), Params.MEDIUM, Reroll.MATERIAL, true), ULTRA(Component.text("Ultra", NamedTextColor.DARK_RED, TextDecoration.BOLD), Params.HARD, Reroll.COURSE, false);

    /** What changes in the blocks of the window while the runner stands on a block. */
    enum Reroll {
        NONE, MATERIAL, COURSE
    }

    /** The generator parameters, shared by the modes that play like another one. */
    private record Params(double scale, int maxGap, int maxGapAscent, double ascentWeight,
                          Map<Surface, Integer> unlocks) {

        static final Params EASY = new Params(160.0, 2, 2, 3.0, Map.of(Surface.FULL, 0, Surface.SLAB, 10));
        static final Params MEDIUM = new Params(80.0, Jump.MAX_GAP, Jump.MAX_GAP_ASCENT, 1.0, Map.of(Surface.FULL, 0, Surface.TRAPDOOR, 10, Surface.SLAB, 10, Surface.FENCE, 25, Surface.PANE, 25, Surface.POST, 40));
        static final Params HARD = new Params(40.0, Jump.MAX_GAP, Jump.MAX_GAP_ASCENT, 1.0, Map.of(Surface.FULL, 0, Surface.TRAPDOOR, 5, Surface.SLAB, 5, Surface.FENCE, 10, Surface.PANE, 10, Surface.POST, 20));
    }

    private final Component label;
    private final Params params;
    private final Reroll reroll;
    private final boolean outlined;

    Mode(Component label, Params params, Reroll reroll, boolean outlined) {
        this.label = label;
        this.params = params;
        this.reroll = reroll;
        this.outlined = outlined;
    }

    /** The mode that follows in the order of the constants, and the first one after the last. */
    Mode next() {
        Mode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /** The coloured name, as shown in the head label and the messages. */
    Component label() {
        return label;
    }

    /** Score at which the level has climbed to 1 - 1/e; a larger value flattens the curve. */
    double scale() {
        return params.scale();
    }

    /** Widest gap of a jump that does not climb. */
    int maxGap() {
        return params.maxGap();
    }

    /** Widest gap of a jump that climbs. */
    int maxGapAscent() {
        return params.maxGapAscent();
    }

    /** What a climbing jump adds to its cost. */
    double ascentWeight() {
        return params.ascentWeight();
    }

    /** What the blocks do while the runner stands still. */
    Reroll reroll() {
        return reroll;
    }

    /** Whether the runner sees the glowing outline of the next block. */
    boolean outlined() {
        return outlined;
    }

    /** The score from which the shape may appear; {@link Integer#MAX_VALUE} when it never does. */
    int minScore(Surface surface) {
        return params.unlocks().getOrDefault(surface, Integer.MAX_VALUE);
    }

    /** The shapes that may appear once the score has reached {@code score}. */
    List<Surface> unlockedAt(int score) {
        return Arrays.stream(Surface.values()).filter(surface -> minScore(surface) <= score).toList();
    }
}
