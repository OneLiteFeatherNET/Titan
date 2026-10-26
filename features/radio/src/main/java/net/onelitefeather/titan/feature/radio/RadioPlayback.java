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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.sound.Sound;

/**
 * One title playing for one player. The lifetime is the same for both kinds: start, tick, stop.
 * A vanilla disc sends its record sound once and ends after its length plus a buffer; a MIDI song
 * sends its notes tick by tick and ends on its last note.
 *
 * <p>Time is the caller's server tick, passed to every call, so the playback never reads a clock.
 */
final class RadioPlayback {

    private final RadioSong song;
    private final RadioSink sink;
    private final List<MidiNote> notes;
    private int cursor;
    private long startTick;
    private boolean started;
    private boolean closed;

    RadioPlayback(RadioSong song, RadioSink sink) {
        this.song = Objects.requireNonNull(song, "song must not be null");
        this.sink = Objects.requireNonNull(sink, "sink must not be null");
        this.notes = song instanceof MidiSong midi ? midi.notes() : List.of();
    }

    RadioSong song() {
        return this.song;
    }

    /** Starts the title at {@code now}; a second start does nothing. */
    PlaybackTick start(long now) {
        if (this.started || this.closed) {
            return PlaybackTick.NOTHING;
        }
        this.started = true;
        this.startTick = now;
        if (this.song instanceof VanillaDisc disc) {
            this.sink.playRecord(Sound.sound(disc.sound(), Sound.Source.RECORD, 1.0f, 1.0f));
            return PlaybackTick.NOTHING;
        }
        return new PlaybackTick(false, playDue(0));
    }

    /** One step at server tick {@code now}: the notes due now, or the end of the title. */
    PlaybackTick tick(long now) {
        if (!this.started || this.closed) {
            return PlaybackTick.NOTHING;
        }
        long elapsed = now - this.startTick;
        if (this.song instanceof VanillaDisc disc) {
            return tickDisc(disc, elapsed);
        }
        int dropped = playDue(elapsed);
        boolean ended = this.cursor == this.notes.size() && elapsed >= ((MidiSong) this.song).lengthTicks();
        if (ended) {
            this.closed = true;
        }
        return new PlaybackTick(ended, dropped);
    }

    /**
     * Ends the title now. A vanilla disc gets its named stop; a MIDI song has nothing to stop, its
     * notes decay. Idempotent, and nothing happens after the end.
     */
    void stop() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.started && this.song instanceof VanillaDisc disc) {
            this.sink.stopRecord(disc.sound().key());
        }
    }

    private PlaybackTick tickDisc(VanillaDisc disc, long elapsed) {
        if (elapsed < disc.lengthTicks() + RadioLimits.DISC_END_BUFFER_TICKS) {
            return PlaybackTick.NOTHING;
        }
        this.closed = true;
        this.sink.stopRecord(disc.sound().key());
        return new PlaybackTick(true, 0);
    }

    // A note more than one tick late is dropped, not played late; of the rest, the loudest sixteen sound.
    private int playDue(long elapsed) {
        int dropped = 0;
        List<MidiNote> due = new ArrayList<>();
        while (this.cursor < this.notes.size() && this.notes.get(this.cursor).tick() <= elapsed) {
            MidiNote note = this.notes.get(this.cursor++);
            if (note.tick() < elapsed - 1) {
                dropped++;
            } else {
                due.add(note);
            }
        }
        due.sort(Comparator.comparingInt(MidiNote::velocity).reversed());
        for (int index = 0; index < due.size(); index++) {
            if (index < RadioLimits.MAX_NOTES_PER_TICK) {
                this.sink.note(noteSound(due.get(index)));
            } else {
                dropped++;
            }
        }
        return dropped;
    }

    private static Sound noteSound(MidiNote note) {
        return Sound.sound(note.instrument().sound(), Sound.Source.RECORD, note.volume(), Pitch.of(note.semitone()));
    }
}
