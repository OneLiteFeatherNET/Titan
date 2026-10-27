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
import java.util.function.IntSupplier;
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
    private final IntSupplier volume;
    private final List<MidiNote> notes;
    /** The tick of the last note of a MIDI song; a MIDI song ends there. */
    private final long lastNoteTick;
    private int cursor;
    private long startTick;
    private boolean started;
    private boolean closed;

    /**
     * @param song  the title to play
     * @param notes the notes of a MIDI song, sorted by tick and not empty; a vanilla disc takes
     *              none
     * @param sink  receives the sounds
     */
    RadioPlayback(RadioSong song, List<MidiNote> notes, RadioSink sink, IntSupplier volume) {
        this.song = Objects.requireNonNull(song, "song must not be null");
        this.sink = Objects.requireNonNull(sink, "sink must not be null");
        this.volume = Objects.requireNonNull(volume, "volume must not be null");
        this.notes = List.copyOf(Objects.requireNonNull(notes, "notes must not be null"));
        if (song instanceof MidiSong && this.notes.isEmpty()) {
            throw new IllegalArgumentException("a MIDI song needs at least one note");
        }
        this.lastNoteTick = this.notes.isEmpty() ? 0 : this.notes.getLast().tick();
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
            this.sink.playRecord(Sound.sound(disc.sound(), Sound.Source.RECORD, Volume.factor(this.volume.getAsInt()), 1.0f));
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
        boolean ended = this.cursor == this.notes.size() && elapsed >= this.lastNoteTick;
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

    private Sound noteSound(MidiNote note) {
        return Sound.sound(note.instrument().sound(), Sound.Source.RECORD, note.volume() * Volume.factor(this.volume.getAsInt()), Pitch.of(note.semitone()));
    }
}
