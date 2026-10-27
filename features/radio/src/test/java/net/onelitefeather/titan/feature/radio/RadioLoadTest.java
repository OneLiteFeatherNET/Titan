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
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletionException;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RadioLoadTest {

    private static final List<MidiNote> NOTES = List.of(new MidiNote(0, RadioInstrument.HARP, 12, 100));

    private static MidiSong song(String id) {
        return new MidiSong(id, id, "Beispiel", Material.MUSIC_DISC_WARD, RadioCategories.OTHER, Path.of(id + ".mid"));
    }

    /** Records what the title's start heard, in order. */
    private static final class Recording implements RadioLoad.Listener {

        private final List<String> events = new ArrayList<>();
        private Throwable failure;

        @Override
        public void loading() {
            this.events.add("loading");
        }

        @Override
        public void ready(List<MidiNote> notes) {
            this.events.add("ready");
        }

        @Override
        public void failed(Throwable cause) {
            this.events.add("failed");
            this.failure = cause;
        }
    }

    private static final class CountingParser implements SongNotesCache.NotesParser {

        private final List<MidiSong> parsed = new ArrayList<>();
        private boolean broken;

        @Override
        public List<MidiNote> parse(MidiSong song) throws SongFormatException, IOException {
            this.parsed.add(song);
            if (this.broken) {
                throw new SongFormatException("broken");
            }
            return NOTES;
        }
    }

    /** Runs the queued tasks until none is left; a task may queue another one. */
    private static void drain(Deque<Runnable> queue) {
        while (!queue.isEmpty()) {
            queue.poll().run();
        }
    }

    @DisplayName("A title whose notes are cached starts at once, without the tick queue")
    @Test
    void cachedTitleStartsWithoutTheTickQueue() {
        CountingParser parser = new CountingParser();
        SongNotesCache cache = new SongNotesCache(Runnable::run, parser);
        cache.acquire(song("sunrise")).release();
        Deque<Runnable> ticks = new ArrayDeque<>();
        RadioLoad load = new RadioLoad(cache, ticks::add);
        Recording listener = new Recording();

        load.begin(song("sunrise"), listener);

        Assertions.assertEquals(List.of("ready"), listener.events, "a cached title must be ready at once");
        Assertions.assertTrue(ticks.isEmpty(), "a cache hit must not go through the tick queue");
    }

    @DisplayName("A title whose notes are not cached waits, and starts on the tick thread once they are read")
    @Test
    void uncachedTitleWaitsThenStartsOnTheTick() {
        Deque<Runnable> io = new ArrayDeque<>();
        Deque<Runnable> ticks = new ArrayDeque<>();
        RadioLoad load = new RadioLoad(new SongNotesCache(io::add, new CountingParser()), ticks::add);
        Recording listener = new Recording();

        load.begin(song("sunrise"), listener);
        Assertions.assertEquals(List.of("loading"), listener.events, "a miss must show the loading state first");
        drain(io);
        Assertions.assertEquals(List.of("loading"), listener.events, "the notes must not start the title on the loader thread");
        drain(ticks);

        Assertions.assertEquals(List.of("loading", "ready"), listener.events, "the title must start on the tick thread once the notes are ready");
    }

    @DisplayName("A second start while the first one loads drops the result of the first one")
    @Test
    void secondStartDropsTheFirstResult() {
        Deque<Runnable> io = new ArrayDeque<>();
        Deque<Runnable> ticks = new ArrayDeque<>();
        RadioLoad load = new RadioLoad(new SongNotesCache(io::add, new CountingParser()), ticks::add);
        Recording first = new Recording();
        Recording second = new Recording();

        load.begin(song("first"), first);
        load.begin(song("second"), second);
        drain(io);
        drain(ticks);

        Assertions.assertEquals(List.of("loading"), first.events, "the first result must be dropped by the generation check");
        Assertions.assertEquals(List.of("loading", "ready"), second.events, "the second title must start");
    }

    @DisplayName("Stop during a load drops the result and releases the title")
    @Test
    void stopDuringLoadDropsTheResult() {
        Deque<Runnable> io = new ArrayDeque<>();
        Deque<Runnable> ticks = new ArrayDeque<>();
        RadioLoad load = new RadioLoad(new SongNotesCache(io::add, new CountingParser()), ticks::add);
        Recording listener = new Recording();
        load.begin(song("sunrise"), listener);

        load.cancel();
        drain(io);
        drain(ticks);

        Assertions.assertEquals(List.of("loading"), listener.events, "a stopped load must never start its title");
        Assertions.assertFalse(load.pending(), "after stop nothing may be pending");
    }

    @DisplayName("Logout during a load drops the result, as it cancels the same load")
    @Test
    void logoutDuringLoadDropsTheResult() {
        Deque<Runnable> io = new ArrayDeque<>();
        Deque<Runnable> ticks = new ArrayDeque<>();
        RadioLoad load = new RadioLoad(new SongNotesCache(io::add, new CountingParser()), ticks::add);
        Recording listener = new Recording();
        load.begin(song("sunrise"), listener);

        load.cancel();
        drain(io);
        drain(ticks);

        Assertions.assertEquals(List.of("loading"), listener.events, "a title whose player logged out must never start");
    }

    @DisplayName("A parse failure is reported once and starts nothing")
    @Test
    void parseFailureIsReportedOnce() {
        Deque<Runnable> io = new ArrayDeque<>();
        Deque<Runnable> ticks = new ArrayDeque<>();
        CountingParser parser = new CountingParser();
        parser.broken = true;
        RadioLoad load = new RadioLoad(new SongNotesCache(io::add, parser), ticks::add);
        Recording listener = new Recording();

        load.begin(song("broken"), listener);
        drain(io);
        drain(ticks);

        Assertions.assertEquals(List.of("loading", "failed"), listener.events, "a failed parse must be reported once and never start");
        Assertions.assertInstanceOf(SongFormatException.class, unwrap(listener.failure), "the reported cause must be the format failure");
        Assertions.assertFalse(load.pending(), "after a failure nothing may be pending");
    }

    @DisplayName("A vanilla disc is ready at once with no notes and touches no cache")
    @Test
    void vanillaDiscNeedsNoNotes() {
        CountingParser parser = new CountingParser();
        Deque<Runnable> ticks = new ArrayDeque<>();
        RadioLoad load = new RadioLoad(new SongNotesCache(Runnable::run, parser), ticks::add);
        Recording listener = new Recording();

        load.begin(VanillaDiscs.ALL.getFirst(), listener);

        Assertions.assertEquals(List.of("ready"), listener.events, "a vanilla disc must start at once");
        Assertions.assertTrue(ticks.isEmpty() && parser.parsed.isEmpty(), "a vanilla disc must neither queue nor parse");
    }

    private static Throwable unwrap(Throwable failure) {
        return failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
    }
}
