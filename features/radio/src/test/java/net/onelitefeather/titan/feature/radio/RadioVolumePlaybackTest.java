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
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RadioVolumePlaybackTest {

    private static final float EPSILON = 0.0001f;

    /** A volume the test changes between calls, as the player does with the button. */
    private static final class Level {
        private int percent;

        Level(int percent) {
            this.percent = percent;
        }

        int get() {
            return this.percent;
        }

        void set(int percent) {
            this.percent = percent;
        }
    }

    private static MidiSong song() {
        return new MidiSong("song", "Song", "Test", Material.MUSIC_DISC_CAT, RadioCategories.OTHER, Path.of("song.mid"));
    }

    @DisplayName("A MIDI note is as loud as its velocity times the volume of the player")
    @Test
    void noteLoudnessIsVelocityTimesVolume() {
        RecordingRadioSink sink = new RecordingRadioSink();
        Level level = new Level(50);
        RadioPlayback playback = new RadioPlayback(song(), List.of(new MidiNote(0, RadioInstrument.HARP, 12, 127)), sink, level::get);

        playback.start(0);

        Assertions.assertEquals(0.5f, sink.notes.getFirst().volume(), EPSILON, "a full-velocity note at 50 percent must sound at 0.5");
    }

    @DisplayName("A volume change applies to the next MIDI note at once")
    @Test
    void volumeChangeAppliesToTheNextNote() {
        RecordingRadioSink sink = new RecordingRadioSink();
        Level level = new Level(100);
        RadioPlayback playback = new RadioPlayback(song(), List.of(new MidiNote(0, RadioInstrument.HARP, 12, 127), new MidiNote(2, RadioInstrument.HARP, 12, 127)), sink, level::get);
        playback.start(0);
        level.set(25);

        playback.tick(2);

        Assertions.assertEquals(2, sink.notes.size(), "the second note must play");
        Assertions.assertEquals(0.25f, sink.notes.get(1).volume(), EPSILON, "the next note must use the new volume of 25 percent");
    }

    @DisplayName("A vanilla disc sounds at the volume of its start and ignores later changes")
    @Test
    void vanillaDiscKeepsTheVolumeOfItsStart() {
        RecordingRadioSink sink = new RecordingRadioSink();
        Level level = new Level(75);
        RadioPlayback playback = new RadioPlayback(VanillaDiscs.ofMaterial(Material.MUSIC_DISC_MELLOHI).orElseThrow(), List.of(), sink, level::get);
        playback.start(0);
        level.set(25);

        playback.tick(10);

        Assertions.assertEquals(1, sink.records.size(), "a changed volume must not restart the disc");
        Assertions.assertEquals(0.75f, sink.records.getFirst().volume(), EPSILON, "the disc must keep the volume it started with");
    }

    @DisplayName("The next start of a vanilla disc uses the volume that is set then")
    @Test
    void nextVanillaStartUsesTheNewVolume() {
        RecordingRadioSink sink = new RecordingRadioSink();
        Level level = new Level(75);
        VanillaDisc disc = VanillaDiscs.ofMaterial(Material.MUSIC_DISC_MELLOHI).orElseThrow();
        new RadioPlayback(disc, List.of(), sink, level::get).start(0);
        level.set(25);

        new RadioPlayback(disc, List.of(), sink, level::get).start(0);

        Assertions.assertEquals(0.25f, sink.records.getLast().volume(), EPSILON, "a new start must use the volume set before it");
    }
}
