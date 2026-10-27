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

import java.nio.file.Path;
import java.util.Objects;
import net.minestom.server.item.Material;

/**
 * A MIDI song from {@code radio/songs.yaml}, played on note blocks. The song holds the path of its
 * file, not its notes: the notes are read when the title starts, through the notes cache.
 *
 * @param id       the song's id in the configuration, unique within it
 * @param title    the title shown in the menu
 * @param artist   the artist shown in the lore
 * @param disc     the music disc item that stands for the song in its menu
 * @param category the category the song is listed in
 * @param file     the MIDI file, inside {@code radio/}
 * @param midi     who made the MIDI file or where it comes from, or {@code null} when the entry
 *                 names no source
 */
record MidiSong(String id, String title, String artist, Material disc, RadioCategory category,
                Path file, String midi) implements RadioSong {

    MidiSong(String id, String title, String artist, Material disc, RadioCategory category, Path file) {
        this(id, title, artist, disc, category, file, null);
    }

    MidiSong {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(artist, "artist must not be null");
        Objects.requireNonNull(disc, "disc must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(file, "file must not be null");
    }

    @Override
    public RadioKind kind() {
        return RadioKind.MIDI;
    }

    @Override
    public RadioCategory category() {
        return this.category;
    }
}
