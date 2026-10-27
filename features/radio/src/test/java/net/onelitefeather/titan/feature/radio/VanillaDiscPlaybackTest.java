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
import net.kyori.adventure.sound.Sound;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VanillaDiscPlaybackTest {

    private static final long START = 0L;
    /** Mellohi lasts 96 seconds: its length in ticks, plus the five seconds of buffer. */
    private static final long MELLOHI_END_TICK = (96 + 5) * RadioLimits.TICKS_PER_SECOND;

    private static VanillaDisc mellohi() {
        return VanillaDiscs.ofMaterial(Material.MUSIC_DISC_MELLOHI).orElseThrow();
    }

    @DisplayName("Start sends the disc's record sound once, from the record source at full volume and pitch")
    @Test
    void startPlaysTheRecordSoundOnce() {
        RecordingRadioSink sink = new RecordingRadioSink();
        RadioPlayback playback = new RadioPlayback(mellohi(), List.of(), sink);

        playback.start(START);

        Assertions.assertEquals(1, sink.records.size(), "start must send exactly one record sound");
        Sound sound = sink.records.getFirst();
        Assertions.assertEquals(mellohi().sound().key(), sound.name(), "the sound must be the disc's own record sound");
        Assertions.assertEquals(Sound.Source.RECORD, sound.source(), "the sound must come from the record source");
        Assertions.assertEquals(1.0f, sound.volume(), "the volume must be 1.0");
        Assertions.assertEquals(1.0f, sound.pitch(), "the pitch must be 1.0");
    }

    @DisplayName("Stop sends a named stop for the disc's sound once")
    @Test
    void stopSendsTheNamedStopOnce() {
        RecordingRadioSink sink = new RecordingRadioSink();
        RadioPlayback playback = new RadioPlayback(mellohi(), List.of(), sink);
        playback.start(START);

        playback.stop();
        playback.stop();

        Assertions.assertEquals(List.of(mellohi().sound().key()), sink.stops, "stop must send one named stop for the disc's sound");
    }

    @DisplayName("The disc ends five seconds after its length, with a named stop and the end")
    @Test
    void discEndsAfterItsLengthPlusBuffer() {
        RecordingRadioSink sink = new RecordingRadioSink();
        RadioPlayback playback = new RadioPlayback(mellohi(), List.of(), sink);
        playback.start(START);

        Assertions.assertFalse(playback.tick(START + MELLOHI_END_TICK - 1).ended(), "the disc must still run one tick before the end");
        Assertions.assertTrue(playback.tick(START + MELLOHI_END_TICK).ended(), "the disc must end when its length plus five seconds is over");
        Assertions.assertEquals(1, sink.stops.size(), "the end must send the named stop once");
    }

    @DisplayName("Stop after the end sends nothing")
    @Test
    void stopAfterTheEndSendsNothing() {
        RecordingRadioSink sink = new RecordingRadioSink();
        RadioPlayback playback = new RadioPlayback(mellohi(), List.of(), sink);
        playback.start(START);
        playback.tick(START + MELLOHI_END_TICK);

        playback.stop();

        Assertions.assertEquals(1, sink.stops.size(), "a stop after the end must not send a second named stop");
    }
}
