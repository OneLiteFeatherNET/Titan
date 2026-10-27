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

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.testfixtures.CapturedLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SongCatalogTest {

    @TempDir
    Path radio;

    private static byte[] oneNoteMidi() throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 24);
        sequence.createTrack().add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, 66, 100), 24));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MidiSystem.write(sequence, 0, out);
        return out.toByteArray();
    }

    private static String entry(String id, String disc, String file) {
        return "  - id: " + id + "\n    title: " + id + "\n    artist: Beispiel\n    disc: " + disc + "\n    file: " + file + "\n";
    }

    private static String songs(String... entries) {
        StringBuilder yaml = new StringBuilder("songs:\n");
        for (String entry : entries) {
            yaml.append(entry);
        }
        return yaml.toString();
    }

    private void writeSongs(String yaml) throws IOException {
        Files.writeString(this.radio.resolve("songs.yaml"), yaml);
    }

    private void writeMidi(String name) throws Exception {
        Files.write(this.radio.resolve(name), oneNoteMidi());
    }

    private static List<String> rejectedNames(SongCatalog.Load load) {
        List<String> names = new ArrayList<>();
        load.rejections().forEach(rejection -> names.add(rejection.name()));
        return names;
    }

    @DisplayName("A missing radio directory loads no MIDI songs and no rejections")
    @Test
    void missingDirectoryLoadsNothing() {
        SongCatalog.Load load = SongCatalog.load(this.radio.resolve("missing"));

        Assertions.assertTrue(load.songs().isEmpty(), "without a radio directory there must be no MIDI songs");
        Assertions.assertTrue(load.rejections().isEmpty(), "a missing directory is not an error");
    }

    @DisplayName("A valid entry with its MIDI file becomes a song with its title, artist and disc")
    @Test
    void validEntryBecomesASong() throws Exception {
        writeMidi("sunrise.mid");
        writeSongs(songs(entry("sunrise", "MUSIC_DISC_CAT", "sunrise.mid")));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertEquals(1, load.songs().size(), "the valid entry must be loaded");
        MidiSong song = load.songs().getFirst();
        Assertions.assertEquals("sunrise", song.title(), "the title must come from the entry");
        Assertions.assertEquals(Material.MUSIC_DISC_CAT, song.disc(), "the disc must come from the entry");
    }

    @DisplayName("An entry whose MIDI file is missing is skipped and named")
    @Test
    void missingFileIsSkipped() throws Exception {
        writeSongs(songs(entry("gone", "MUSIC_DISC_CAT", "gone.mid")));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertTrue(load.songs().isEmpty(), "an entry without its file must not be loaded");
        Assertions.assertEquals(List.of("gone.mid"), rejectedNames(load), "the rejection must name the missing file");
    }

    @DisplayName("An entry that does not name a music disc is skipped")
    @Test
    void wrongDiscTypeIsSkipped() throws Exception {
        writeMidi("stone.mid");
        writeSongs(songs(entry("stone", "STONE", "stone.mid")));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertTrue(load.songs().isEmpty(), "a disc that is not a music disc must be rejected");
        Assertions.assertEquals(1, load.rejections().size(), "the rejection must be reported once");
    }

    @DisplayName("A second song on the same disc is skipped, the first one stays")
    @Test
    void secondSongOnTheSameDiscIsSkipped() throws Exception {
        writeMidi("one.mid");
        writeMidi("two.mid");
        writeSongs(songs(entry("one", "MUSIC_DISC_CAT", "one.mid"), entry("two", "MUSIC_DISC_CAT", "two.mid")));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertEquals(List.of("one"), load.songs().stream().map(MidiSong::title).toList(), "only the first song on the disc must stay");
        Assertions.assertEquals(List.of("two.mid"), rejectedNames(load), "the second song must be rejected by its file");
    }

    @DisplayName("A MIDI song may use the disc of a vanilla disc, as the menus are separate")
    @Test
    void midiSongMayUseTheDiscOfAVanillaDisc() throws Exception {
        writeMidi("cat.mid");
        writeSongs(songs(entry("cat", "MUSIC_DISC_CAT", "cat.mid")));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertEquals(1, load.songs().size(), "the disc of a vanilla disc must not block a MIDI song");
    }

    @DisplayName("A second entry with the same id is skipped")
    @Test
    void duplicateIdIsSkipped() throws Exception {
        writeMidi("one.mid");
        writeMidi("two.mid");
        writeSongs(songs(entry("same", "MUSIC_DISC_WARD", "one.mid"), entry("same", "MUSIC_DISC_STRAD", "two.mid")));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertEquals(1, load.songs().size(), "the id must be unique within the configuration");
    }

    @DisplayName("A file that is no MIDI file is skipped with a warning that names it")
    @Test
    void brokenFileIsSkippedWithAWarning() throws Exception {
        Files.write(this.radio.resolve("broken.mid"), new byte[]{1, 2, 3, 4});
        writeSongs(songs(entry("broken", "MUSIC_DISC_WAIT", "broken.mid")));

        try (CapturedLog log = new CapturedLog(SongCatalog.class)) {
            SongCatalog.Load load = SongCatalog.load(this.radio);

            Assertions.assertTrue(load.songs().isEmpty(), "a broken file must not be loaded");
            List<String> messages = log.warnings().stream().map(ILoggingEvent::getFormattedMessage).toList();
            Assertions.assertTrue(messages.stream().anyMatch(message -> message.contains("broken.mid")), "the warning must name the file, was " + messages);
        }
    }

    @DisplayName("More than eighteen songs are skipped beyond the eighteenth")
    @Test
    void nineteenthSongIsSkipped() throws Exception {
        String[] discs = {"MUSIC_DISC_13", "MUSIC_DISC_BLOCKS", "MUSIC_DISC_CHIRP", "MUSIC_DISC_FAR", "MUSIC_DISC_MALL", "MUSIC_DISC_MELLOHI", "MUSIC_DISC_STAL", "MUSIC_DISC_STRAD", "MUSIC_DISC_WARD", "MUSIC_DISC_11", "MUSIC_DISC_WAIT", "MUSIC_DISC_OTHERSIDE", "MUSIC_DISC_5", "MUSIC_DISC_PIGSTEP", "MUSIC_DISC_RELIC", "MUSIC_DISC_CREATOR", "MUSIC_DISC_CREATOR_MUSIC_BOX", "MUSIC_DISC_PRECIPICE", "MUSIC_DISC_TEARS"};
        List<String> entries = new ArrayList<>();
        for (int index = 0; index < discs.length; index++) {
            writeMidi("song" + index + ".mid");
            entries.add(entry("song" + index, discs[index], "song" + index + ".mid"));
        }
        writeSongs(songs(entries.toArray(String[]::new)));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertEquals(18, load.songs().size(), "eighteen songs must be loaded");
        Assertions.assertEquals(List.of("song18.mid"), rejectedNames(load), "the nineteenth song must be the one that is skipped");
    }

    @DisplayName("A songs.yaml that is not valid YAML skips the file and keeps the lobby going")
    @Test
    void invalidYamlSkipsTheFile() throws Exception {
        writeSongs("songs: [\n");

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertTrue(load.songs().isEmpty(), "an unreadable songs.yaml must yield no songs");
        Assertions.assertEquals(List.of("songs.yaml"), rejectedNames(load), "the rejection must name songs.yaml");
    }
}
