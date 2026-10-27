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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MidiSongParserTest {

    private static final int TICKS_PER_QUARTER = 24;
    private static final int TEMPO_120_BPM = 500_000;
    private static final int TEMPO_60_BPM = 1_000_000;
    private static final int MAX_FILE_BYTES = 512 * 1024;

    /** A type 0 file with one track: the track and its events are what the test builds. */
    private static final class Score {

        private final Sequence sequence;
        private final Track track;

        Score() throws Exception {
            this.sequence = new Sequence(Sequence.PPQ, TICKS_PER_QUARTER);
            this.track = this.sequence.createTrack();
        }

        Score note(long tick, int key, int velocity) throws Exception {
            this.track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, key, velocity), tick));
            return this;
        }

        Score noteOff(long tick, int key) throws Exception {
            this.track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, key, 0), tick));
            return this;
        }

        Score tempo(long tick, int microsPerQuarter) throws Exception {
            byte[] data = {(byte) (microsPerQuarter >> 16), (byte) (microsPerQuarter >> 8), (byte) microsPerQuarter};
            this.track.add(new MidiEvent(new MetaMessage(0x51, data, data.length), tick));
            return this;
        }

        byte[] bytes() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MidiSystem.write(this.sequence, 0, out);
            return out.toByteArray();
        }
    }

    private static MidiSong parse(byte[] bytes) throws SongFormatException {
        return MidiSongParser.parse("sunrise", "Sunrise", "Beispiel", Material.MUSIC_DISC_CAT, bytes);
    }

    @DisplayName("At 120 BPM a quarter note of 24 ticks lands on game tick 10 and two quarters on tick 20")
    @Test
    void ticksFollowTheDefaultTempo() throws Exception {
        MidiSong song = parse(new Score().note(24, 66, 100).note(48, 66, 100).bytes());

        Assertions.assertEquals(List.of(10, 20), song.notes().stream().map(MidiNote::tick).toList(), "120 BPM must map a quarter note to 10 game ticks");
    }

    @DisplayName("A tempo change is taken into account for the notes after it")
    @Test
    void tempoChangeIsTakenIntoAccount() throws Exception {
        MidiSong song = parse(new Score().tempo(24, TEMPO_60_BPM).note(48, 66, 100).bytes());

        Assertions.assertEquals(30, song.notes().getFirst().tick(), "after the switch to 60 BPM, 500 ms before the note plus 1000 ms after it must be game tick 30");
    }

    @DisplayName("Note-off events and note-ons with velocity 0 are not played")
    @Test
    void noteOffAndZeroVelocityAreIgnored() throws Exception {
        MidiSong song = parse(new Score().note(24, 66, 0).noteOff(30, 66).note(48, 66, 90).bytes());

        Assertions.assertEquals(1, song.notes().size(), "only the note with a velocity above 0 must remain");
        Assertions.assertEquals(20, song.notes().getFirst().tick(), "the remaining note must be the one at tick 48");
    }

    @DisplayName("Broken bytes are rejected with a reason")
    @Test
    void brokenBytesAreRejectedWithAReason() {
        SongFormatException rejected = Assertions.assertThrows(SongFormatException.class, () -> parse(new byte[]{1, 2, 3}), "bytes that are no MIDI file must be rejected");

        Assertions.assertFalse(rejected.getMessage().isBlank(), "a rejection must name its reason");
    }

    @DisplayName("A file larger than 512 KiB is rejected before it is read")
    @Test
    void fileLargerThanTheLimitIsRejected() {
        byte[] huge = new byte[MAX_FILE_BYTES + 1];

        Assertions.assertThrows(SongFormatException.class, () -> parse(huge), "a file above 512 KiB must be rejected");
    }

    @DisplayName("More than 20 000 notes reject the song")
    @Test
    void tooManyNotesAreRejected() throws Exception {
        Score score = new Score();
        for (int i = 0; i <= 20_000; i++) {
            score.note(i, 66, 100);
        }

        Assertions.assertThrows(SongFormatException.class, () -> parse(score.bytes()), "a song with 20 001 notes must be rejected");
    }

    @DisplayName("A song longer than 15 minutes is rejected")
    @Test
    void tooLongSongIsRejected() throws Exception {
        byte[] bytes = new Score().note(43_300, 66, 100).bytes();

        Assertions.assertThrows(SongFormatException.class, () -> parse(bytes), "a note after 15 minutes must reject the song");
    }
}
