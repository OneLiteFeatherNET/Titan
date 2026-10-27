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

import net.kyori.adventure.key.Key;

/** The 16 note block instruments a MIDI note can play. */
enum RadioInstrument {
    HARP("harp"), BASEDRUM("basedrum"), SNARE("snare"), HAT("hat"), BASS("bass"), FLUTE("flute"), BELL("bell"), GUITAR("guitar"), CHIME("chime"), XYLOPHONE("xylophone"), IRON_XYLOPHONE("iron_xylophone"), COW_BELL("cow_bell"), DIDGERIDOO("didgeridoo"), BIT("bit"), BANJO("banjo"), PLING("pling");

    private final Key sound;

    RadioInstrument(String id) {
        this.sound = Key.key("minecraft", "block.note_block." + id);
    }

    /** The client's note block sound of this instrument. */
    Key sound() {
        return this.sound;
    }
}
