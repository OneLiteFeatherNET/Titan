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
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The menu items as built for the client. Minestom's material prototypes exist only once the server
 * registries are bound, so the in-process server of Microtus runs under each test.
 */
@ExtendWith(MicrotusExtension.class)
class SongItemTest {

    private static final RadioCategory GAMES = new RadioCategory("games", "Spiele", "Games", Material.EMERALD, 10);

    private RadioMessages messages;

    @BeforeEach
    void registerMessages(Env env) {
        this.messages = new RadioMessages();
        this.messages.register();
    }

    @AfterEach
    void unregisterMessages() {
        this.messages.close();
    }

    private static MidiSong midiSong(String midi) {
        return new MidiSong("sunrise", "Sunrise", "DJ Bjra ft. Julia", Material.MUSIC_DISC_CAT, GAMES, Path.of("sunrise.mid"), midi);
    }

    private static List<String> loreLines(ItemStack item) {
        return item.get(DataComponents.LORE, List.of()).stream().map(line -> PlainTextComponentSerializer.plainText().serialize(line)).toList();
    }

    @DisplayName("A vanilla disc item carries no jukebox description")
    @Test
    void vanillaDiscItemHasNoJukeboxDescription() {
        ItemStack item = RadioMenuBuilder.songItem(VanillaDiscs.ofMaterial(Material.MUSIC_DISC_13).orElseThrow(), false, Locale.ENGLISH);

        Assertions.assertNull(item.get(DataComponents.JUKEBOX_PLAYABLE), "the vanilla disc description must be removed from the item");
    }

    @DisplayName("A MIDI song item carries no jukebox description, although its disc has one")
    @Test
    void midiSongItemHasNoJukeboxDescription() {
        Assertions.assertNotNull(ItemStack.builder(Material.MUSIC_DISC_CAT).build().get(DataComponents.JUKEBOX_PLAYABLE), "the disc must have a jukebox description to remove");

        ItemStack item = RadioMenuBuilder.songItem(midiSong("BitMidi-Upload (unbekannt)"), false, Locale.ENGLISH);

        Assertions.assertNull(item.get(DataComponents.JUKEBOX_PLAYABLE), "the MIDI song must not show the vanilla disc description");
    }

    @DisplayName("A MIDI song with a midi source shows the original and the source on separate lines")
    @Test
    void midiSongShowsOriginalAndSourceOnSeparateLines() {
        ItemStack item = RadioMenuBuilder.songItem(midiSong("BitMidi-Upload (unbekannt)"), false, Locale.ENGLISH);

        Assertions.assertEquals(List.of("Original: DJ Bjra ft. Julia", "MIDI: BitMidi-Upload (unbekannt)"), loreLines(item), "the lore must name the original and the MIDI source, one per line");
    }

    @DisplayName("A MIDI song without a midi source shows only the original")
    @Test
    void midiSongWithoutSourceShowsOnlyTheOriginal() {
        ItemStack item = RadioMenuBuilder.songItem(midiSong(null), false, Locale.ENGLISH);

        Assertions.assertEquals(List.of("Original: DJ Bjra ft. Julia"), loreLines(item), "the lore must not contain a MIDI line without a midi source");
    }

    @DisplayName("A vanilla disc shows its original artist and no MIDI line")
    @Test
    void vanillaDiscShowsOnlyItsArtist() {
        ItemStack item = RadioMenuBuilder.songItem(VanillaDiscs.ofMaterial(Material.MUSIC_DISC_13).orElseThrow(), false, Locale.ENGLISH);

        Assertions.assertEquals(List.of("C418 · Music disc"), loreLines(item), "the vanilla lore must name the artist and no MIDI line");
    }
}
