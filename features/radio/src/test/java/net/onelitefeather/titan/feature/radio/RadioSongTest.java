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

import java.util.List;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RadioSongTest {

    private static final RadioCategory GAMES = new RadioCategory("games", "Spiele", "Games", Material.EMERALD, 10);

    @DisplayName("A vanilla disc is of kind VANILLA and carries its title, artist and disc")
    @Test
    void vanillaDiscIsOfKindVanilla() {
        RadioSong song = VanillaDiscs.ofMaterial(Material.MUSIC_DISC_13).orElseThrow();

        Assertions.assertEquals(RadioKind.VANILLA, song.kind(), "a vanilla disc must report the kind VANILLA");
        Assertions.assertEquals(Material.MUSIC_DISC_13, song.disc(), "a vanilla disc must report its own disc");
        Assertions.assertEquals("13", song.title(), "a vanilla disc must report its title");
        Assertions.assertEquals("C418", song.artist(), "a vanilla disc must report its artist");
    }

    @DisplayName("A MIDI song is of kind MIDI and carries its title, artist and disc")
    @Test
    void midiSongIsOfKindMidi() {
        RadioSong song = new MidiSong("sunrise", "Sunrise", "Beispiel", Material.MUSIC_DISC_WARD, GAMES, List.of(new MidiNote(0, RadioInstrument.HARP, 12, 100)));

        Assertions.assertEquals(RadioKind.MIDI, song.kind(), "a MIDI song must report the kind MIDI");
        Assertions.assertEquals(Material.MUSIC_DISC_WARD, song.disc(), "a MIDI song must report its own disc");
        Assertions.assertEquals("Sunrise", song.title(), "a MIDI song must report its title");
        Assertions.assertEquals("Beispiel", song.artist(), "a MIDI song must report its artist");
    }

    @DisplayName("Every vanilla disc belongs to the built-in Minecraft category")
    @Test
    void vanillaDiscsBelongToMinecraft() {
        for (RadioSong song : VanillaDiscs.ALL) {
            Assertions.assertEquals(RadioCategories.MINECRAFT, song.category(), song.title() + " must belong to the Minecraft category");
        }
    }

    @DisplayName("A MIDI song keeps the category it was declared with")
    @Test
    void midiSongKeepsItsCategory() {
        RadioSong song = new MidiSong("sunrise", "Sunrise", "Beispiel", Material.MUSIC_DISC_WARD, GAMES, List.of(new MidiNote(0, RadioInstrument.HARP, 12, 100)));

        Assertions.assertEquals(GAMES, song.category(), "a MIDI song must report the category it was declared with");
    }
}
