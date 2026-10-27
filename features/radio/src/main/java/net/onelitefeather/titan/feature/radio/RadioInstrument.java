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
package net.onelitefeather.titan.feature.radio;

import java.util.Comparator;
import java.util.List;
import net.kyori.adventure.key.Key;

/**
 * The 20 note block instruments a MIDI note can play. Each has a register: note 0 is its base key,
 * and
 * it reaches 25 notes from there. A note the register does not hold plays on a range extension
 * instead.
 */
enum RadioInstrument {
    HARP("harp", Pitch.LOWEST_KEY), BASEDRUM("basedrum", Pitch.LOWEST_KEY), SNARE("snare", Pitch.LOWEST_KEY), HAT("hat", Pitch.LOWEST_KEY), BASS("bass", 30), FLUTE("flute", 66), BELL("bell", 78), GUITAR("guitar", 42), CHIME("chime", 78), XYLOPHONE("xylophone", 78), IRON_XYLOPHONE("iron_xylophone", Pitch.LOWEST_KEY), COW_BELL("cow_bell", 66), DIDGERIDOO("didgeridoo", 30), BIT("bit", Pitch.LOWEST_KEY), BANJO("banjo", Pitch.LOWEST_KEY), PLING("pling", Pitch.LOWEST_KEY),
    // Trumpet and trumpet_exposed registers are from the wiki; weathered and oxidized are assumed until heard in game.
    TRUMPET("trumpet", Pitch.LOWEST_KEY), TRUMPET_EXPOSED("trumpet_exposed", Pitch.LOWEST_KEY), TRUMPET_WEATHERED("trumpet_weathered", Pitch.LOWEST_KEY), TRUMPET_OXIDIZED("trumpet_oxidized", Pitch.LOWEST_KEY);

    /**
     * The instruments a note outside its own register moves to; their registers cover the whole
     * span.
     */
    private static final List<RadioInstrument> EXTENSIONS = List.of(BASS, GUITAR, FLUTE, BELL);

    private final Key sound;
    private final int baseKey;

    RadioInstrument(String id, int baseKey) {
        this.sound = Key.key("minecraft", "block.note_block." + id);
        this.baseKey = baseKey;
    }

    /** The client's note block sound of this instrument. */
    Key sound() {
        return this.sound;
    }

    /** The MIDI key of note 0 of this instrument. */
    int baseKey() {
        return this.baseKey;
    }

    /** Whether the register of this instrument holds {@code key}. */
    boolean reaches(int key) {
        return key >= this.baseKey && key <= this.baseKey + Pitch.HIGHEST;
    }

    /** The note that plays {@code key} on this instrument; the register must hold the key. */
    int semitoneOf(int key) {
        if (!reaches(key)) {
            throw new IllegalArgumentException(this + " cannot play key " + key);
        }
        return key - this.baseKey;
    }

    /**
     * The instrument that plays {@code key}: this one if its register holds the key, otherwise the
     * range
     * extension whose register holds it and whose base key is nearest to this instrument's base
     * key.
     */
    RadioInstrument playing(int key) {
        if (reaches(key)) {
            return this;
        }
        return EXTENSIONS.stream().filter(extension -> extension.reaches(key)).min(Comparator.comparingInt(extension -> Math.abs(extension.baseKey - this.baseKey))).orElseThrow(() -> new IllegalArgumentException("no instrument plays key " + key));
    }
}
