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
package net.onelitefeather.titan.feature.throwing;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/** How a running charge is shown to its thrower: the meter, the tone and the preview cadence. */
final class ChargeFeedback {

    /** Cells of the action bar meter. */
    static final int CELLS = 10;

    /** The tone repeats every five ticks, so a long charge does not flood the sound channel. */
    static final int SOUND_EVERY_TICKS = 5;

    /** The preview is recomputed and resent every second tick. */
    static final int PREVIEW_EVERY_TICKS = 2;

    private static final char FILLED = '█';
    private static final char EMPTY = '░';
    private static final TextColor LOW = TextColor.color(0x55FF55);
    private static final TextColor MIDDLE = TextColor.color(0xFFFF55);
    private static final TextColor HIGH = TextColor.color(0xFF5555);

    private ChargeFeedback() {
    }

    /**
     * The meter's colour: green at no charge, through yellow at half, to red at a full charge.
     *
     * @return the colour for {@code strength}, clamped to 0 to 1
     */
    static TextColor colour(double strength) {
        double clamped = Math.max(0.0, Math.min(1.0, strength));
        if (clamped <= 0.5) {
            return blend(LOW, MIDDLE, clamped * 2);
        }
        return blend(MIDDLE, HIGH, (clamped - 0.5) * 2);
    }

    /**
     * The action bar meter: filled cells in the colour of the charge, the rest dark, and the
     * percentage in the same colour.
     */
    static Component meter(double strength) {
        int filled = filledCells(strength);
        TextColor colour = colour(strength);
        long percent = Math.round(Math.max(0.0, Math.min(1.0, strength)) * 100);
        return Component.text(String.valueOf(FILLED).repeat(filled), colour).append(Component.text(String.valueOf(EMPTY).repeat(CELLS - filled), NamedTextColor.DARK_GRAY)).append(Component.text(" " + percent + "%", colour));
    }

    private static TextColor blend(TextColor from, TextColor to, double fraction) {
        int red = (int) Math.round(from.red() + (to.red() - from.red()) * fraction);
        int green = (int) Math.round(from.green() + (to.green() - from.green()) * fraction);
        int blue = (int) Math.round(from.blue() + (to.blue() - from.blue()) * fraction);
        return TextColor.color(red, green, blue);
    }

    /** @return the tone's pitch: from 0.5 at no charge up to 2.0 at a full one */
    static float pitch(double strength) {
        double clamped = Math.max(0.0, Math.min(1.0, strength));
        return (float) (0.5 + 1.5 * clamped);
    }

    static boolean soundDue(long ticks) {
        return ticks % SOUND_EVERY_TICKS == 0;
    }

    static boolean previewDue(long ticks) {
        return ticks % PREVIEW_EVERY_TICKS == 0;
    }

    static int filledCells(double strength) {
        double clamped = Math.max(0.0, Math.min(1.0, strength));
        return (int) Math.round(clamped * CELLS);
    }
}
