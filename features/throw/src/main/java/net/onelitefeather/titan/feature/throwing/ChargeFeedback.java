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

/** How a running charge is shown to its thrower: the bar, the tone and the preview cadence. */
final class ChargeFeedback {

    /** Cells of the actionbar bar. */
    static final int CELLS = 10;

    /** The tone repeats every five ticks, so a long charge does not flood the sound channel. */
    static final int SOUND_EVERY_TICKS = 5;

    /** The preview is recomputed and resent every second tick. */
    static final int PREVIEW_EVERY_TICKS = 2;

    private static final char FILLED = '#';
    private static final char EMPTY = '-';

    private ChargeFeedback() {
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

    static String bar(double strength) {
        int filled = filledCells(strength);
        return String.valueOf(FILLED).repeat(filled) + String.valueOf(EMPTY).repeat(CELLS - filled);
    }
}
