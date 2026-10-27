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
import java.util.Objects;
import net.minestom.server.item.Material;

/**
 * A MIDI song from {@code radio/songs.yaml}, played on note blocks. The notes are sorted by tick.
 *
 * @param id     the song's id in the configuration, unique within it
 * @param title  the title shown in the menu
 * @param artist the artist shown in the lore
 * @param disc   the music disc item that stands for the song in its menu
 * @param notes  the notes, which must not be empty
 */
record MidiSong(String id, String title, String artist, Material disc,
                List<MidiNote> notes) implements RadioSong {

    MidiSong {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(artist, "artist must not be null");
        Objects.requireNonNull(disc, "disc must not be null");
        notes = notes.stream().sorted(Comparator.comparingInt(MidiNote::tick)).toList();
        if (notes.isEmpty()) {
            throw new IllegalArgumentException("a MIDI song needs at least one note");
        }
    }

    @Override
    public RadioKind kind() {
        return RadioKind.MIDI;
    }

    /** The tick of the last note: the song ends there. */
    int lengthTicks() {
        return this.notes.getLast().tick();
    }
}
