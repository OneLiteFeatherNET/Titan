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
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
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

    private static final String OTHER_WARNING = "no category, using other";
    private static final byte[] MIDI_HEADER = "MThd".getBytes(StandardCharsets.US_ASCII);

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

    private static String inCategory(String id, String disc, String file, String category) {
        return entry(id, disc, file) + "    category: " + category + "\n";
    }

    private static String category(String id, String name, String icon, int order) {
        return "  - id: " + id + "\n    name: " + name + "\n    icon: " + icon + "\n    order: " + order + "\n";
    }

    private static String songs(String... entries) {
        return "songs:\n" + String.join("", entries);
    }

    private static String categories(String... entries) {
        return "categories:\n" + String.join("", entries);
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

    private static List<String> warnings(CapturedLog log) {
        return log.warnings().stream().map(ILoggingEvent::getFormattedMessage).toList();
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

    @DisplayName("The midi field names the source of the MIDI file")
    @Test
    void midiFieldNamesTheSource() throws Exception {
        writeMidi("sunrise.mid");
        writeSongs(songs(entry("sunrise", "MUSIC_DISC_CAT", "sunrise.mid") + "    midi: BitMidi-Upload (unbekannt)\n"));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertEquals("BitMidi-Upload (unbekannt)", load.songs().getFirst().midi(), "the midi field must name the source of the MIDI file");
    }

    @DisplayName("An entry without a midi field has no MIDI source")
    @Test
    void entryWithoutMidiFieldHasNoSource() throws Exception {
        writeMidi("sunrise.mid");
        writeSongs(songs(entry("sunrise", "MUSIC_DISC_CAT", "sunrise.mid")));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertNull(load.songs().getFirst().midi(), "an entry without a midi field must have no MIDI source");
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

    @DisplayName("A second song on the same disc loads too, since the disc is only the menu icon")
    @Test
    void secondSongOnTheSameDiscLoads() throws Exception {
        writeMidi("one.mid");
        writeMidi("two.mid");
        writeSongs(songs(entry("one", "MUSIC_DISC_CAT", "one.mid"), entry("two", "MUSIC_DISC_CAT", "two.mid")));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertEquals(List.of("one", "two"), load.songs().stream().map(MidiSong::title).toList(), "both songs must load, the disc is not a key any more");
        Assertions.assertTrue(load.rejections().isEmpty(), "a shared disc must not be a rejection");
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
            List<String> messages = warnings(log);
            Assertions.assertTrue(messages.stream().anyMatch(message -> message.contains("broken.mid")), "the warning must name the file, was " + messages);
        }
    }

    @DisplayName("A file outside radio/ is skipped")
    @Test
    void fileOutsideTheDirectoryIsSkipped() throws Exception {
        Files.write(this.radio.resolveSibling("outside.mid"), oneNoteMidi());
        writeSongs(songs(entry("outside", "MUSIC_DISC_CAT", "../outside.mid")));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertTrue(load.songs().isEmpty(), "a file that resolves outside radio/ must not be loaded");
        Assertions.assertEquals(List.of("../outside.mid"), rejectedNames(load), "the rejection must name the file");
    }

    @DisplayName("A file with a valid MIDI header and a broken body is loaded, its body is only read when it plays")
    @Test
    void validHeaderWithBrokenBodyIsLoaded() throws Exception {
        Files.write(this.radio.resolve("body.mid"), new byte[]{'M', 'T', 'h', 'd', 9, 9, 9, 9, 9, 9});
        writeSongs(songs(entry("body", "MUSIC_DISC_CAT", "body.mid")));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertEquals(1, load.songs().size(), "the header is the only thing checked at start");
        Assertions.assertTrue(load.rejections().isEmpty(), "a broken body must not be a rejection at start");
    }

    @DisplayName("A file of 50 MB with a valid header is loaded, since there is no size limit")
    @Test
    void fiftyMegabyteFileWithValidHeaderIsLoaded() throws Exception {
        try (RandomAccessFile file = new RandomAccessFile(this.radio.resolve("huge.mid").toFile(), "rw")) {
            file.write(MIDI_HEADER);
            file.setLength(50L * 1024 * 1024);
        }
        writeSongs(songs(entry("huge", "MUSIC_DISC_CAT", "huge.mid")));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertEquals(1, load.songs().size(), "a large file with a valid header must be loaded");
    }

    @DisplayName("Five hundred songs load, as there is no count limit")
    @Test
    void fiveHundredSongsLoad() throws Exception {
        writeMidi("one.mid");
        List<String> entries = new ArrayList<>();
        for (int index = 0; index < 500; index++) {
            entries.add(entry("song" + index, "MUSIC_DISC_CAT", "one.mid"));
        }
        writeSongs(songs(entries.toArray(String[]::new)));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertEquals(500, load.songs().size(), "every one of the 500 songs must be loaded");
        Assertions.assertTrue(load.rejections().isEmpty(), "no song may be rejected by a count");
    }

    @DisplayName("A songs.yaml that is not valid YAML skips the file and keeps the lobby going")
    @Test
    void invalidYamlSkipsTheFile() throws Exception {
        writeSongs("songs: [\n");

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertTrue(load.songs().isEmpty(), "an unreadable songs.yaml must yield no songs");
        Assertions.assertEquals(List.of("songs.yaml"), rejectedNames(load), "the rejection must name songs.yaml");
    }

    @DisplayName("An old songs.yaml with eighteen songs and no categories loads all of them into Other, one warning each")
    @Test
    void oldSongsFileLoadsEverySongIntoOther() throws Exception {
        String[] discs = {"MUSIC_DISC_13", "MUSIC_DISC_BLOCKS", "MUSIC_DISC_CHIRP", "MUSIC_DISC_FAR", "MUSIC_DISC_MALL", "MUSIC_DISC_MELLOHI", "MUSIC_DISC_STAL", "MUSIC_DISC_STRAD", "MUSIC_DISC_WARD", "MUSIC_DISC_11", "MUSIC_DISC_WAIT", "MUSIC_DISC_OTHERSIDE", "MUSIC_DISC_5", "MUSIC_DISC_PIGSTEP", "MUSIC_DISC_RELIC", "MUSIC_DISC_CREATOR", "MUSIC_DISC_CREATOR_MUSIC_BOX", "MUSIC_DISC_PRECIPICE"};
        List<String> entries = new ArrayList<>();
        for (int index = 0; index < discs.length; index++) {
            writeMidi("song" + index + ".mid");
            entries.add(entry("song" + index, discs[index], "song" + index + ".mid"));
        }
        writeSongs(songs(entries.toArray(String[]::new)));

        try (CapturedLog log = new CapturedLog(SongCatalog.class)) {
            SongCatalog.Load load = SongCatalog.load(this.radio);

            Assertions.assertEquals(18, load.songs().size(), "all eighteen old songs must load");
            Assertions.assertTrue(load.songs().stream().allMatch(song -> song.category() == RadioCategories.OTHER), "every song without a category must be in Other");
            Assertions.assertEquals(18, warnings(log).stream().filter(message -> message.contains(OTHER_WARNING)).count(), "each song without a category must log one warning");
        }
    }

    @DisplayName("Unknown fields in a song entry are ignored")
    @Test
    void unknownFieldsAreIgnored() throws Exception {
        writeMidi("one.mid");
        writeSongs(songs(entry("one", "MUSIC_DISC_CAT", "one.mid") + "    bpm: 120\n    note: from a newer version\n"));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertEquals(1, load.songs().size(), "a song with unknown fields must still load");
    }

    @DisplayName("A song without a category goes to Other and logs one warning that says so")
    @Test
    void songWithoutCategoryGoesToOther() throws Exception {
        writeMidi("one.mid");
        writeSongs(songs(entry("one", "MUSIC_DISC_CAT", "one.mid")));

        try (CapturedLog log = new CapturedLog(SongCatalog.class)) {
            SongCatalog.Load load = SongCatalog.load(this.radio);

            Assertions.assertEquals(RadioCategories.OTHER, load.songs().getFirst().category(), "a song without a category must be in Other");
            Assertions.assertEquals(1, warnings(log).stream().filter(message -> message.contains(OTHER_WARNING)).count(), "the missing category must be logged once");
        }
    }

    @DisplayName("A song with an undeclared category goes to Other and the warning names the id")
    @Test
    void unknownCategoryGoesToOtherAndNamesTheId() throws Exception {
        writeMidi("one.mid");
        writeSongs(categories(category("games", "Spiele", "EMERALD", 10)) + songs(inCategory("one", "MUSIC_DISC_CAT", "one.mid", "winter")));

        try (CapturedLog log = new CapturedLog(SongCatalog.class)) {
            SongCatalog.Load load = SongCatalog.load(this.radio);

            Assertions.assertEquals(RadioCategories.OTHER, load.songs().getFirst().category(), "an undeclared category must put the song in Other");
            Assertions.assertTrue(warnings(log).stream().anyMatch(message -> message.contains("winter")), "the warning must name the undeclared id, was " + warnings(log));
        }
    }

    @DisplayName("A song that names a declared category keeps it")
    @Test
    void declaredCategoryIsKeptForTheSong() throws Exception {
        writeMidi("one.mid");
        writeSongs(categories(category("games", "Spiele", "EMERALD", 10)) + songs(inCategory("one", "MUSIC_DISC_CAT", "one.mid", "games")));

        SongCatalog.Load load = SongCatalog.load(this.radio);

        Assertions.assertEquals("games", load.songs().getFirst().category().id(), "the song must keep its declared category");
        Assertions.assertEquals(List.of("games"), load.categories().stream().map(RadioCategory::id).toList(), "the declared category must be loaded");
    }

    @DisplayName("A song that names the built-in Minecraft category is treated as unknown")
    @Test
    void builtInCategoryNameInSongIsTreatedAsUnknown() throws Exception {
        writeMidi("one.mid");
        writeSongs(songs(inCategory("one", "MUSIC_DISC_CAT", "one.mid", "minecraft")));

        try (CapturedLog log = new CapturedLog(SongCatalog.class)) {
            SongCatalog.Load load = SongCatalog.load(this.radio);

            Assertions.assertEquals(RadioCategories.OTHER, load.songs().getFirst().category(), "a song naming minecraft must be in Other");
            Assertions.assertTrue(warnings(log).stream().anyMatch(message -> message.contains("minecraft")), "the warning must name the reserved id");
        }
    }

    @DisplayName("A category with a reserved id is skipped with a warning")
    @Test
    void reservedCategoryIdIsSkipped() throws Exception {
        writeSongs(categories(category("minecraft", "Mine", "EMERALD", 10)));

        try (CapturedLog log = new CapturedLog(SongCatalog.class)) {
            SongCatalog.Load load = SongCatalog.load(this.radio);

            Assertions.assertTrue(load.categories().isEmpty(), "a reserved id must not be declared");
            Assertions.assertTrue(warnings(log).stream().anyMatch(message -> message.contains("minecraft")), "the warning must name the reserved id");
        }
    }

    @DisplayName("A category id that does not match the pattern is skipped with a warning")
    @Test
    void invalidCategoryIdIsSkipped() throws Exception {
        writeSongs(categories(category("Bad Id", "Schlecht", "EMERALD", 10)));

        try (CapturedLog log = new CapturedLog(SongCatalog.class)) {
            SongCatalog.Load load = SongCatalog.load(this.radio);

            Assertions.assertTrue(load.categories().isEmpty(), "an id with a space and capitals must not be declared");
            Assertions.assertFalse(warnings(log).isEmpty(), "the invalid id must be logged");
        }
    }

    @DisplayName("A second declaration of the same category id is skipped with a warning")
    @Test
    void duplicateCategoryIdIsSkipped() throws Exception {
        writeSongs(categories(category("games", "Spiele", "EMERALD", 10), category("games", "Games", "GOLD_INGOT", 20)));

        try (CapturedLog log = new CapturedLog(SongCatalog.class)) {
            SongCatalog.Load load = SongCatalog.load(this.radio);

            Assertions.assertEquals(1, load.categories().size(), "only the first declaration of an id must be kept");
            Assertions.assertEquals("Spiele", load.categories().getFirst().nameDe(), "the first declaration must win");
            Assertions.assertFalse(warnings(log).isEmpty(), "the duplicate must be logged");
        }
    }

    @DisplayName("The twenty-first declared category is skipped with a warning")
    @Test
    void twentyFirstCategoryIsSkipped() throws Exception {
        List<String> entries = new ArrayList<>();
        for (int index = 0; index <= RadioCategories.MAX_DECLARED; index++) {
            entries.add(category("c" + index, "Kategorie " + index, "EMERALD", index));
        }
        writeSongs(categories(entries.toArray(String[]::new)));

        try (CapturedLog log = new CapturedLog(SongCatalog.class)) {
            SongCatalog.Load load = SongCatalog.load(this.radio);

            Assertions.assertEquals(RadioCategories.MAX_DECLARED, load.categories().size(), "no more than twenty categories must be declared");
            Assertions.assertEquals("c19", load.categories().getLast().id(), "the category beyond the twentieth must be the one skipped");
            Assertions.assertEquals(1, warnings(log).size(), "the skipped category must be logged once");
        }
    }

    @DisplayName("A category without a name takes its id as name and logs a warning")
    @Test
    void categoryWithoutNameUsesItsId() throws Exception {
        writeSongs("categories:\n  - id: anime\n    icon: EMERALD\n    order: 1\n");

        try (CapturedLog log = new CapturedLog(SongCatalog.class)) {
            SongCatalog.Load load = SongCatalog.load(this.radio);

            RadioCategory anime = load.categories().getFirst();
            Assertions.assertEquals("anime", anime.nameDe(), "the German name must fall back to the id");
            Assertions.assertEquals("anime", anime.nameEn(), "the English name must fall back to the id");
            Assertions.assertFalse(warnings(log).isEmpty(), "the missing name must be logged");
        }
    }

    @DisplayName("A category name given as text is used in both languages")
    @Test
    void textNameIsUsedInBothLanguages() throws Exception {
        writeSongs(categories(category("anime", "Anime", "FIREWORK_ROCKET", 20)));

        RadioCategory anime = SongCatalog.load(this.radio).categories().getFirst();

        Assertions.assertEquals("Anime", anime.nameDe(), "the German name must be the text");
        Assertions.assertEquals("Anime", anime.nameEn(), "the English name must be the text");
    }

    @DisplayName("A category name given per language keeps both names")
    @Test
    void mapNameKeepsBothLanguages() throws Exception {
        writeSongs("categories:\n  - id: games\n    name: { de: Spiele, en: Games }\n    icon: EMERALD\n    order: 10\n");

        RadioCategory games = SongCatalog.load(this.radio).categories().getFirst();

        Assertions.assertEquals("Spiele", games.nameDe(), "the German name must come from de");
        Assertions.assertEquals("Games", games.nameEn(), "the English name must come from en");
    }

    @DisplayName("An icon that is no vanilla material becomes paper, with a warning")
    @Test
    void unknownIconBecomesPaper() throws Exception {
        writeSongs(categories(category("games", "Spiele", "NOT_AN_ITEM", 10)));

        try (CapturedLog log = new CapturedLog(SongCatalog.class)) {
            RadioCategory games = SongCatalog.load(this.radio).categories().getFirst();

            Assertions.assertEquals(Material.PAPER, games.icon(), "an unknown icon must become paper");
            Assertions.assertTrue(warnings(log).stream().anyMatch(message -> message.contains("NOT_AN_ITEM")), "the warning must name the unknown icon");
        }
    }

    @DisplayName("A category without an icon becomes paper, with a warning")
    @Test
    void missingIconBecomesPaper() throws Exception {
        writeSongs("categories:\n  - id: games\n    name: Spiele\n    order: 10\n");

        try (CapturedLog log = new CapturedLog(SongCatalog.class)) {
            RadioCategory games = SongCatalog.load(this.radio).categories().getFirst();

            Assertions.assertEquals(Material.PAPER, games.icon(), "a missing icon must become paper");
            Assertions.assertFalse(warnings(log).isEmpty(), "the missing icon must be logged");
        }
    }
}
