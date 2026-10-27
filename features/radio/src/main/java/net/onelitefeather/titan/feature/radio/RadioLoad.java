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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * The load of one player's title: it holds the notes lease of the title that starts or plays, and
 * hands the result back to the tick thread. A generation counter drops every result that arrives
 * after a newer start, a stop or a logout.
 *
 * <p>The tick executor is injected, so a test drains it explicitly. A cache hit completes at once,
 * on the caller's thread, and never touches the tick executor.
 */
final class RadioLoad {

    /**
     * What the start of one title hears: that it waits, that its notes are ready, or that they
     * cannot be read.
     */
    interface Listener {

        /** The notes are not cached; the player waits. */
        void loading();

        /** The title may start with these notes. The lease stays held until {@link #cancel()}. */
        void ready(List<MidiNote> notes);

        /** The notes cannot be read, so nothing starts. */
        void failed(Throwable cause);
    }

    private final SongNotesCache cache;
    private final Executor tick;
    private long generation;
    private SongNotesCache.Lease lease;
    private boolean pending;

    RadioLoad(SongNotesCache cache, Executor tick) {
        this.cache = Objects.requireNonNull(cache, "cache must not be null");
        this.tick = Objects.requireNonNull(tick, "tick must not be null");
    }

    /**
     * Starts {@code song}, and drops what the player held before. A vanilla disc needs no notes and
     * is ready at once; a MIDI song is ready at once when its notes are cached.
     */
    void begin(RadioSong song, Listener listener) {
        cancel();
        long started = this.generation;
        if (!(song instanceof MidiSong midi)) {
            listener.ready(List.of());
            return;
        }
        this.lease = this.cache.acquire(midi);
        CompletableFuture<List<MidiNote>> notes = this.lease.notes();
        if (notes.isDone()) {
            settle(started, listener, notes);
            return;
        }
        this.pending = true;
        listener.loading();
        notes.whenComplete((ready, failure) -> this.tick.execute(() -> settle(started, listener, notes)));
    }

    /**
     * Ends the current load or title: advances the generation and releases the notes lease.
     * Idempotent.
     */
    void cancel() {
        this.generation++;
        this.pending = false;
        releaseLease();
    }

    /** Whether a title waits for its notes. */
    boolean pending() {
        return this.pending;
    }

    // A result from an older generation is dropped; its lease was released by cancel.
    private void settle(long started, Listener listener, CompletableFuture<List<MidiNote>> notes) {
        if (started != this.generation) {
            return;
        }
        this.pending = false;
        if (notes.isCompletedExceptionally()) {
            releaseLease();
            listener.failed(failureOf(notes));
            return;
        }
        listener.ready(notes.join());
    }

    private void releaseLease() {
        if (this.lease != null) {
            this.lease.release();
            this.lease = null;
        }
    }

    private static Throwable failureOf(CompletableFuture<List<MidiNote>> notes) {
        Throwable failure = notes.handle((value, error) -> error).join();
        if (failure instanceof CompletionException && failure.getCause() != null) {
            return failure.getCause();
        }
        return failure;
    }
}
