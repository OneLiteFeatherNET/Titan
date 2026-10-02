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
package net.onelitefeather.titan.feature.jumprun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.sound.SoundEvent;
import org.junit.jupiter.api.Test;

class RunSoundsTest {

    private static final double SEMITONE = Math.pow(2.0, 1.0 / 12.0);
    private static final float DELTA = 1e-5f;

    @Test
    void everyScoreSoundsTwoPlingsForThePlayerSource() {
        List<Sound> tones = RunSounds.tones(7);

        assertEquals(2, tones.size(), "two simultaneous components");
        tones.forEach(tone -> {
            assertEquals(SoundEvent.BLOCK_NOTE_BLOCK_PLING.key(), tone.name());
            assertEquals(Sound.Source.PLAYER, tone.source());
        });
    }

    @Test
    void eachComponentRisesOneSemitonePerPointExceptAtTheWrap() {
        for (int score = 0; score < 11; score++) {
            List<Sound> now = RunSounds.tones(score);
            List<Sound> next = RunSounds.tones(score + 1);

            for (int component = 0; component < 2; component++) {
                assertEquals(now.get(component).pitch() * SEMITONE, next.get(component).pitch(), 1e-4, "component " + component + " from score " + score);
            }
        }
    }

    @Test
    void theLowerComponentBecomesTheUpperOneAtTheWrap() {
        assertEquals(RunSounds.tones(11).getFirst().pitch() * SEMITONE, RunSounds.tones(12).get(1).pitch(), 1e-4, "the same voice continues one semitone higher");
    }

    @Test
    void theRangeEndsAreSilent() {
        assertEquals(0.0f, RunSounds.volume(0), DELTA, "h = 0");
        assertEquals(0.0f, RunSounds.volume(24), DELTA, "h = 24");
    }

    @Test
    void theLowestPitchIsHalfAndTheHighestIsDouble() {
        assertEquals(0.5f, RunSounds.pitch(0), DELTA);
        assertEquals(2.0f, RunSounds.pitch(24), DELTA);
    }

    @Test
    void theVolumesAddUpToTheSameForAllTwelveSteps() {
        for (int score = 0; score < 12; score++) {
            List<Sound> tones = RunSounds.tones(score);

            assertEquals(1.0f, tones.get(0).volume() + tones.get(1).volume(), DELTA, "step " + score);
        }
    }

    @Test
    void theStepRepeatsEveryTwelvePoints() {
        assertEquals(RunSounds.tones(5), RunSounds.tones(29));
    }

    @Test
    void theSignalIsAQuietHatForThePlayerSource() {
        Sound signal = RunSounds.signalTone();

        assertEquals(SoundEvent.BLOCK_NOTE_BLOCK_HAT.key(), signal.name());
        assertEquals(Sound.Source.PLAYER, signal.source());
        assertEquals(0.6f, signal.volume(), DELTA);
        assertEquals(1.0f, signal.pitch(), DELTA);
    }

    @Test
    void theFailureIsThreeFallingBassNotes() {
        List<Sound> notes = RunSounds.failTones();

        assertEquals(List.of(1.0f, 0.84f, 0.67f), notes.stream().map(Sound::pitch).toList());
        notes.forEach(note -> {
            assertEquals(SoundEvent.BLOCK_NOTE_BLOCK_BASS.key(), note.name());
            assertEquals(Sound.Source.PLAYER, note.source());
        });
    }

    @Test
    void aRecordIsTheLevelUpForThePlayerSource() {
        Sound record = RunSounds.recordTone();

        assertEquals(SoundEvent.ENTITY_PLAYER_LEVELUP.key(), record.name());
        assertEquals(Sound.Source.PLAYER, record.source());
        assertEquals(1.0f, record.volume(), DELTA);
        assertEquals(1.0f, record.pitch(), DELTA);
    }
}
