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

import java.util.ArrayList;
import java.util.List;
import net.minestom.server.item.Material;
import net.kyori.adventure.sound.Sound;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RadioPlaybackTest {

    private static final long START = 0L;

    private static MidiNote note(int tick, int velocity) {
        return new MidiNote(tick, RadioInstrument.HARP, 12, velocity);
    }

    private static MidiSong song(MidiNote... notes) {
        return new MidiSong("song", "Song", "Test", Material.MUSIC_DISC_CAT, RadioCategories.OTHER, List.of(notes));
    }

    private static List<Integer> velocities(List<Sound> sounds) {
        List<Integer> velocities = new ArrayList<>();
        for (Sound sound : sounds) {
            velocities.add(Math.round(sound.volume() * 127));
        }
        return velocities;
    }

    @DisplayName("A note sounds on the tick it is written for")
    @Test
    void noteSoundsOnItsOwnTick() {
        RecordingRadioSink sink = new RecordingRadioSink();
        RadioPlayback playback = new RadioPlayback(song(note(0, 100), note(2, 80)), sink);

        playback.start(START);
        Assertions.assertEquals(1, sink.notes.size(), "the note on tick 0 must sound at the start");
        playback.tick(START + 1);
        Assertions.assertEquals(1, sink.notes.size(), "nothing is due on tick 1");
        playback.tick(START + 2);
        Assertions.assertEquals(2, sink.notes.size(), "the note on tick 2 must sound on tick 2");
        Assertions.assertEquals(RadioInstrument.HARP.sound(), sink.notes.get(1).name(), "the note must be played by its instrument");
    }

    @DisplayName("A note more than one tick late is dropped instead of played in a burst")
    @Test
    void noteMoreThanOneTickLateIsDropped() {
        RecordingRadioSink sink = new RecordingRadioSink();
        RadioPlayback playback = new RadioPlayback(song(note(2, 100), note(4, 100)), sink);
        playback.start(START);

        PlaybackTick tick = playback.tick(START + 5);

        Assertions.assertEquals(1, tick.droppedNotes(), "the note on tick 2 is three ticks late and must be dropped");
        Assertions.assertEquals(1, sink.notes.size(), "only the note on tick 4, one tick late, must sound");
    }

    @DisplayName("No more than sixteen notes sound on one tick, the loudest first")
    @Test
    void sixteenLoudestNotesSoundPerTick() {
        List<MidiNote> notes = new ArrayList<>();
        for (int velocity = 1; velocity <= 20; velocity++) {
            notes.add(note(0, velocity));
        }
        RecordingRadioSink sink = new RecordingRadioSink();
        RadioPlayback playback = new RadioPlayback(song(notes.toArray(MidiNote[]::new)), sink);

        PlaybackTick tick = playback.start(START);

        Assertions.assertEquals(16, sink.notes.size(), "sixteen notes must sound on one tick");
        Assertions.assertEquals(4, tick.droppedNotes(), "the four quietest notes must be dropped");
        Assertions.assertTrue(velocities(sink.notes).stream().allMatch(velocity -> velocity >= 5), "the dropped notes must be the four quietest, velocities 1 to 4");
    }

    @DisplayName("The song ends on the tick of its last note")
    @Test
    void songEndsWithItsLastNote() {
        RecordingRadioSink sink = new RecordingRadioSink();
        RadioPlayback playback = new RadioPlayback(song(note(0, 100), note(10, 100)), sink);
        playback.start(START);

        Assertions.assertFalse(playback.tick(START + 9).ended(), "the song must go on before its last note");
        Assertions.assertTrue(playback.tick(START + 10).ended(), "the song must end on the tick of its last note");
    }

    @DisplayName("Stop silences the song at once and can be called again")
    @Test
    void stopSilencesTheSongAndIsIdempotent() {
        RecordingRadioSink sink = new RecordingRadioSink();
        RadioPlayback playback = new RadioPlayback(song(note(0, 100), note(5, 100)), sink);
        playback.start(START);

        playback.stop();
        playback.stop();
        playback.tick(START + 5);

        Assertions.assertEquals(1, sink.notes.size(), "no note may sound after stop");
        Assertions.assertTrue(sink.stops.isEmpty(), "a MIDI song never sends a named stop, its notes decay");
    }
}
