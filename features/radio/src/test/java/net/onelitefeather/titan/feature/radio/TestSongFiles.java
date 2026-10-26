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
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * Writes a small MIDI song and the {@code songs.yaml} that names it, into a test's own directory.
 */
final class TestSongFiles {

    private TestSongFiles() {
    }

    /**
     * A two-note file, on game ticks 10 and 20 at 120 BPM, named {@code fileName}, and a songs.yaml
     * with one entry for it: id {@code sunrise}, on disc {@code MUSIC_DISC_CAT}.
     */
    static void writeSunrise(Path directory, String fileName) throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, 24);
        Track track = sequence.createTrack();
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, 66, 100), 24));
        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, 69, 100), 48));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MidiSystem.write(sequence, 0, out);
        Files.write(directory.resolve(fileName), out.toByteArray());
        Files.writeString(directory.resolve("songs.yaml"), "songs:\n  - id: sunrise\n    title: Sunrise\n    artist: Beispiel\n    disc: MUSIC_DISC_CAT\n    file: " + fileName + "\n");
    }
}
