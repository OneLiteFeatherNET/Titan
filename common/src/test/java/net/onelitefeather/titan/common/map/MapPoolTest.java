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
package net.onelitefeather.titan.common.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The no-name cases rely on the default lobby world being called {@code world}, i.e. on
 * {@code -DTITAN_LOBBY_MAP} being unset in the test JVM.
 */
class MapPoolTest {

    @TempDir
    Path worlds;

    private void world(String name) throws IOException {
        Path dir = Files.createDirectory(worlds.resolve(name));
        Files.writeString(dir.resolve(MapEntry.MAP_FILE_NAME), "{}");
    }

    private static List<MapEntry> withMapFile(Stream<Path> paths) {
        return paths.map(MapEntry::new).filter(MapEntry::hasMapFile).collect(Collectors.toList());
    }

    private MapPool pool() {
        return new MapPool(worlds, MapPoolTest::withMapFile);
    }

    private MapPool pool(String worldName) {
        return new MapPool(worlds, MapPoolTest::withMapFile, Optional.of(worldName));
    }

    private static String selectedName(MapPool pool) {
        return pool.getMapEntry().path().getFileName().toString();
    }

    @Test
    void picksTheDefaultWorldWhenSeveralExist() throws IOException {
        world("world");
        world("winter");

        assertEquals("world", selectedName(pool()), "the default world wins among several");
    }

    @Test
    void picksTheOnlyWorldWhateverItIsCalled() throws IOException {
        world("winter");

        assertEquals("winter", selectedName(pool()), "a single world is used without a name match");
    }

    @Test
    void throwsWhenSeveralWorldsExistButNoneIsTheDefault() throws IOException {
        world("winter");
        world("summer");

        assertThrows(NoSuchElementException.class, this::pool);
    }

    @Test
    void throwsWhenNoWorldExists() {
        assertThrows(IllegalArgumentException.class, this::pool);
    }

    @Test
    void picksTheNamedWorldAmongSeveral() throws IOException {
        world("world");
        world("winter");

        assertEquals("winter", selectedName(pool("winter")), "the named world beats the default");
    }

    @Test
    void picksTheNamedWorldWhenItIsTheOnlyOne() throws IOException {
        world("winter");

        assertEquals("winter", selectedName(pool("winter")));
    }

    @Test
    void throwsWithTheNameWhenTheNamedWorldIsMissing() throws IOException {
        world("world");
        world("summer");

        var exception = assertThrows(IllegalArgumentException.class, () -> pool("winter"));

        assertTrue(exception.getMessage().contains("winter"), "message names the missing world: " + exception.getMessage());
    }

    @Test
    void throwsInsteadOfFallingBackWhenTheOnlyWorldIsNotTheNamedOne() throws IOException {
        world("world");

        assertThrows(IllegalArgumentException.class, () -> pool("winter"));
    }

    @Test
    void behavesAsWithoutANameWhenTheNameIsEmpty() throws IOException {
        world("world");
        world("winter");

        var pool = new MapPool(worlds, MapPoolTest::withMapFile, Optional.empty());

        assertEquals("world", selectedName(pool));
    }
}
