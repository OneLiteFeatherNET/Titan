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

import java.util.List;
import java.util.stream.IntStream;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;

/**
 * The sound of a point: a Shepard scale, which the ear hears as a pitch that rises forever. Two
 * plings sound one octave apart, each rising a semitone per point. Minecraft plays pitches from 0.5
 * to 2.0, so the range is 24 semitones; the upper voice fades out before it leaves the range, and
 * the lower one fades in from silence.
 */
final class RunSounds {

    private static final int OCTAVE = 12;
    private static final int RANGE = 2 * OCTAVE;
    private static final int VOICES = 2;
    private static final float SIGNAL_VOLUME = 0.6f;
    private static final int FAIL_STAGGER_TICKS = 3;
    private static final float[] FAIL_PITCHES = {1.0f, 0.84f, 0.67f};

    private RunSounds() {
    }

    /** Plays the tone for {@code score} to the runner alone. */
    static void play(Player runner, int score) {
        tones(score).forEach(runner::playSound);
    }

    /**
     * A steady cue for a landing in the ascent, which scores nothing; the runner alone hears it.
     */
    static void signal(Player runner) {
        runner.playSound(signalTone());
    }

    static Sound signalTone() {
        return Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.PLAYER, SIGNAL_VOLUME, 1.0f);
    }

    /** Three falling bass notes for the runner alone, the first at once and the rest staggered. */
    static void fail(Player runner) {
        List<Sound> tones = failTones();
        runner.playSound(tones.getFirst());
        for (int note = 1; note < tones.size(); note++) {
            Sound tone = tones.get(note);
            // The runner may have left by the time a later note is due.
            runner.scheduler().buildTask(() -> {
                if (runner.isOnline()) {
                    runner.playSound(tone);
                }
            }).delay(TaskSchedule.tick(FAIL_STAGGER_TICKS * note)).schedule();
        }
    }

    static List<Sound> failTones() {
        return IntStream.range(0, FAIL_PITCHES.length).mapToObj(note -> Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_BASS, Sound.Source.PLAYER, 1.0f, FAIL_PITCHES[note])).toList();
    }

    static List<Sound> tones(int score) {
        int step = score % OCTAVE;
        return IntStream.range(0, VOICES).map(voice -> step + OCTAVE * voice).mapToObj(RunSounds::tone).toList();
    }

    private static Sound tone(int semitone) {
        return Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.PLAYER, volume(semitone), pitch(semitone));
    }

    static float pitch(int semitone) {
        return (float) (0.5 * Math.pow(2.0, semitone / (double) OCTAVE));
    }

    static float volume(int semitone) {
        double sine = Math.sin(Math.PI * semitone / RANGE);
        return (float) (sine * sine);
    }
}
