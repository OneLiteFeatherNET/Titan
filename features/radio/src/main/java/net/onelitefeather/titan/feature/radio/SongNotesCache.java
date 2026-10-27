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

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * The notes of MIDI songs, read on demand. A title that plays holds a lease on its notes; when the
 * last holder releases it, the notes stay as an idle entry, and the {@link #IDLE_CAPACITY} most
 * recently released entries are kept. A held entry is never evicted.
 *
 * <p>The map and the idle list share one lock; the parse runs on the executor outside it. A failed
 * parse stays as a failed entry, so the same file is not read again and again.
 */
final class SongNotesCache {

    /** How many released, not running titles are kept; running titles are never counted. */
    static final int IDLE_CAPACITY = 8;

    /** Reads the notes of a song; the cache calls it on the executor, never on the tick thread. */
    @FunctionalInterface
    interface NotesParser {

        List<MidiNote> parse(MidiSong song) throws SongFormatException, IOException;
    }

    private final Object lock = new Object();
    private final Executor io;
    private final NotesParser parser;
    private final Map<MidiSong, Entry> entries = new HashMap<>();
    /** The released entries, least recently released first. */
    private final LinkedHashMap<MidiSong, Entry> idle = new LinkedHashMap<>();

    /**
     * @param io     runs the parses; a test passes {@code Runnable::run}
     * @param parser reads the notes of one song
     */
    SongNotesCache(Executor io, NotesParser parser) {
        this.io = Objects.requireNonNull(io, "io must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
    }

    /**
     * Holds the notes of {@code song}. A cached song completes at once; a song that is not cached
     * starts its parse on the executor. Two acquires of the same song share one parse.
     */
    Lease acquire(MidiSong song) {
        Objects.requireNonNull(song, "song must not be null");
        Entry entry;
        boolean parse = false;
        synchronized (this.lock) {
            entry = this.entries.get(song);
            if (entry == null) {
                entry = new Entry();
                this.entries.put(song, entry);
                parse = true;
            } else {
                this.idle.remove(song);
            }
            entry.holders++;
        }
        if (parse) {
            startParse(song, entry);
        }
        return new Lease(song, entry);
    }

    private void startParse(MidiSong song, Entry entry) {
        try {
            this.io.execute(() -> parse(song, entry));
        } catch (RejectedExecutionException e) {
            entry.notes.completeExceptionally(new IOException("the radio's loader has stopped", e));
        }
    }

    private void parse(MidiSong song, Entry entry) {
        try {
            entry.notes.complete(List.copyOf(this.parser.parse(song)));
        } catch (SongFormatException | IOException e) {
            entry.notes.completeExceptionally(e);
        } catch (RuntimeException e) {
            entry.notes.completeExceptionally(new IOException("the file cannot be read", e));
        }
    }

    private void release(MidiSong song, Entry entry) {
        synchronized (this.lock) {
            entry.holders--;
            if (entry.holders == 0) {
                this.idle.put(song, entry);
                evictBeyondCapacity();
            }
        }
    }

    // Called under the lock. Only idle entries are in the list, so a held title is never dropped.
    private void evictBeyondCapacity() {
        while (this.idle.size() > IDLE_CAPACITY) {
            this.entries.remove(this.idle.pollFirstEntry().getKey());
        }
    }

    /** One entry of the cache: the notes, and how many leases hold them. */
    private static final class Entry {

        private final CompletableFuture<List<MidiNote>> notes = new CompletableFuture<>();
        private int holders;
    }

    /**
     * A holder's claim on the notes of one song. {@link #notes()} completes with the notes, or
     * exceptionally with the {@link SongFormatException} or {@link IOException} of a failed parse,
     * wrapped in a {@link java.util.concurrent.CompletionException} by {@code join()}. Releasing
     * is idempotent.
     */
    final class Lease {

        private final MidiSong song;
        private final Entry entry;
        private boolean released;

        private Lease(MidiSong song, Entry entry) {
            this.song = song;
            this.entry = entry;
        }

        CompletableFuture<List<MidiNote>> notes() {
            return this.entry.notes;
        }

        /** Gives the hold back; a second call does nothing. */
        void release() {
            synchronized (lock) {
                if (this.released) {
                    return;
                }
                this.released = true;
                SongNotesCache.this.release(this.song, this.entry);
            }
        }
    }
}
