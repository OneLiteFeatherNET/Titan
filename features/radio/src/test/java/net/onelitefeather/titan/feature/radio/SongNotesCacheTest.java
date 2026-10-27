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
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SongNotesCacheTest {

    private static final List<MidiNote> NOTES = List.of(new MidiNote(0, RadioInstrument.HARP, 12, 100));

    private static MidiSong song(String id) {
        return new MidiSong(id, id, "Beispiel", Material.MUSIC_DISC_WARD, RadioCategories.OTHER, Path.of(id + ".mid"));
    }

    /** Counts the parses per song. A failure set on it is thrown for every song. */
    private static final class CountingParser implements SongNotesCache.NotesParser {

        private final Map<MidiSong, Integer> parses = new HashMap<>();
        private SongFormatException formatFailure;
        private IOException ioFailure;

        @Override
        public List<MidiNote> parse(MidiSong song) throws SongFormatException, IOException {
            this.parses.merge(song, 1, Integer::sum);
            if (this.formatFailure != null) {
                throw this.formatFailure;
            }
            if (this.ioFailure != null) {
                throw this.ioFailure;
            }
            return NOTES;
        }

        int parses(MidiSong song) {
            return this.parses.getOrDefault(song, 0);
        }
    }

    /** An executor the test drains explicitly, so the parse runs when the test says so. */
    private static final class QueuedExecutor implements java.util.concurrent.Executor {

        private final Deque<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable task) {
            this.tasks.add(task);
        }

        void drain() {
            while (!this.tasks.isEmpty()) {
                this.tasks.poll().run();
            }
        }
    }

    private static void useOnce(SongNotesCache cache, MidiSong song) {
        cache.acquire(song).release();
    }

    @DisplayName("A title that is not cached is parsed once and its notes are delivered")
    @Test
    void missParsesOnceAndDeliversTheNotes() {
        CountingParser parser = new CountingParser();
        SongNotesCache cache = new SongNotesCache(Runnable::run, parser);

        SongNotesCache.Lease lease = cache.acquire(song("sunrise"));

        Assertions.assertEquals(NOTES, lease.notes().join(), "the parsed notes must be delivered");
        Assertions.assertEquals(1, parser.parses(song("sunrise")), "a miss must parse the file once");
    }

    @DisplayName("A second acquire of a title that is cached does not parse it again")
    @Test
    void cachedTitleIsNotParsedAgain() {
        CountingParser parser = new CountingParser();
        SongNotesCache cache = new SongNotesCache(Runnable::run, parser);
        useOnce(cache, song("sunrise"));

        SongNotesCache.Lease again = cache.acquire(song("sunrise"));

        Assertions.assertTrue(again.notes().isDone(), "a hit must return an already completed future");
        Assertions.assertEquals(1, parser.parses(song("sunrise")), "a hit must not parse the file again");
    }

    @DisplayName("Two acquires of a title that is still loading share one parse")
    @Test
    void concurrentAcquiresShareOneParse() {
        CountingParser parser = new CountingParser();
        QueuedExecutor io = new QueuedExecutor();
        SongNotesCache cache = new SongNotesCache(io, parser);

        SongNotesCache.Lease first = cache.acquire(song("sunrise"));
        SongNotesCache.Lease second = cache.acquire(song("sunrise"));
        Assertions.assertEquals(1, io.tasks.size(), "the second acquire must not queue a second parse");
        io.drain();

        Assertions.assertEquals(1, parser.parses(song("sunrise")), "both holders must wait for one parse");
        Assertions.assertEquals(first.notes().join(), second.notes().join(), "both holders must receive the same notes");
    }

    @DisplayName("A running title is never evicted while nine other titles pass through the cache")
    @Test
    void runningTitleIsNeverEvicted() {
        CountingParser parser = new CountingParser();
        SongNotesCache cache = new SongNotesCache(Runnable::run, parser);
        cache.acquire(song("running"));
        for (int index = 1; index <= 9; index++) {
            useOnce(cache, song("other" + index));
        }

        SongNotesCache.Lease again = cache.acquire(song("running"));

        Assertions.assertEquals(1, parser.parses(song("running")), "the running title must still be cached");
        Assertions.assertTrue(again.notes().isDone(), "the running title must still be complete");
    }

    @DisplayName("The eight most recently released titles stay cached and the oldest one is dropped")
    @Test
    void lruKeepsTheLastEightReleasedTitles() {
        CountingParser parser = new CountingParser();
        SongNotesCache cache = new SongNotesCache(Runnable::run, parser);
        for (int index = 0; index < 9; index++) {
            useOnce(cache, song("song" + index));
        }

        useOnce(cache, song("song8"));
        Assertions.assertEquals(1, parser.parses(song("song8")), "the newest released title must still be cached");
        useOnce(cache, song("song0"));

        Assertions.assertEquals(2, parser.parses(song("song0")), "the oldest released title must have been dropped and parsed again");
        Assertions.assertEquals(8, SongNotesCache.IDLE_CAPACITY, "the cache must keep eight released titles");
    }

    @DisplayName("A lease released twice counts as one release, so the title stays held")
    @Test
    void releasingTwiceCountsOnce() {
        CountingParser parser = new CountingParser();
        SongNotesCache cache = new SongNotesCache(Runnable::run, parser);
        SongNotesCache.Lease first = cache.acquire(song("held"));
        cache.acquire(song("held"));

        first.release();
        first.release();
        for (int index = 1; index <= 9; index++) {
            useOnce(cache, song("other" + index));
        }

        cache.acquire(song("held"));
        Assertions.assertEquals(1, parser.parses(song("held")), "the second holder must keep the title cached after a double release");
    }

    @DisplayName("A failed parse stays as a failed entry, and the next acquire fails at once without parsing again")
    @Test
    void failedParseIsKept() {
        CountingParser parser = new CountingParser();
        parser.formatFailure = new SongFormatException("broken");
        SongNotesCache cache = new SongNotesCache(Runnable::run, parser);
        useOnce(cache, song("broken"));

        SongNotesCache.Lease again = cache.acquire(song("broken"));

        Assertions.assertTrue(again.notes().isCompletedExceptionally(), "the second acquire must already be failed");
        Assertions.assertEquals(1, parser.parses(song("broken")), "a failed title must not be parsed again");
    }

    @DisplayName("A format failure completes the notes with a CompletionException whose cause is the SongFormatException")
    @Test
    void formatFailureCompletesWithItsCause() {
        CountingParser parser = new CountingParser();
        parser.formatFailure = new SongFormatException("broken");
        SongNotesCache cache = new SongNotesCache(Runnable::run, parser);

        CompletionException thrown = Assertions.assertThrows(CompletionException.class, () -> cache.acquire(song("broken")).notes().join(), "a failed parse must fail the future");

        Assertions.assertInstanceOf(SongFormatException.class, thrown.getCause(), "the cause must be the format failure");
    }

    @DisplayName("A read failure completes the notes with a CompletionException whose cause is the IOException")
    @Test
    void readFailureCompletesWithItsCause() {
        CountingParser parser = new CountingParser();
        parser.ioFailure = new IOException("unreadable");
        SongNotesCache cache = new SongNotesCache(Runnable::run, parser);

        CompletionException thrown = Assertions.assertThrows(CompletionException.class, () -> cache.acquire(song("gone")).notes().join(), "a failed read must fail the future");

        Assertions.assertInstanceOf(IOException.class, thrown.getCause(), "the cause must be the IOException");
    }
}
