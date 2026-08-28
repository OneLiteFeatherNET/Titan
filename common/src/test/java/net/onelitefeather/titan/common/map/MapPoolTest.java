/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapPoolTest {

    private static List<MapEntry> allDirectories(Stream<Path> paths) {
        return paths.map(MapEntry::new).collect(Collectors.toList());
    }

    private static Path worlds(Path root, String... names) throws IOException {
        Path worlds = Files.createDirectories(root.resolve("worlds"));
        for (String name : names) {
            Files.createDirectories(worlds.resolve(name));
        }
        return worlds;
    }

    private static String selectedName(Path worlds) {
        return new MapPool(worlds, MapPoolTest::allDirectories).getMapEntry().path().getFileName().toString();
    }

    @AfterEach
    void clearProperty() {
        System.clearProperty(MapPool.LOBBY_MAP_PROPERTY);
    }

    @Test
    @DisplayName("The property selects the named world when several worlds exist")
    void testThePropertySelectsTheNamedWorld(@TempDir Path root) throws IOException {
        Path worlds = worlds(root, "world", "halloween", "winter");
        System.setProperty(MapPool.LOBBY_MAP_PROPERTY, "halloween");

        assertEquals("halloween", selectedName(worlds));
    }

    @Test
    @DisplayName("The property is evaluated even when only one world exists (US-1.05)")
    void testThePropertyIsEvaluatedForASingleWorld(@TempDir Path root) throws IOException {
        // The old implementation short-circuited here and took the only entry without ever looking
        // at the property, which made a single-world machine behave differently from production.
        Path worlds = worlds(root, "halloween");
        System.setProperty(MapPool.LOBBY_MAP_PROPERTY, "halloween");
        MapPool pool = new MapPool(worlds, MapPoolTest::allDirectories);

        assertEquals("halloween", pool.getMapEntry().path().getFileName().toString());
        assertTrue(pool.isRequestedMapSelected(), "the only world is the requested one");
    }

    @Test
    @DisplayName("A single world that the property does not name is reported as a fallback (US-1.05)")
    void testASingleUnrequestedWorldIsAFallback(@TempDir Path root) throws IOException {
        // Same selection as before the fix, but no longer a silent one: the property was read and
        // the world it named was not there.
        Path worlds = worlds(root, "winter");
        System.setProperty(MapPool.LOBBY_MAP_PROPERTY, "halloween");
        MapPool pool = new MapPool(worlds, MapPoolTest::allDirectories);

        assertEquals("winter", pool.getMapEntry().path().getFileName().toString());
        assertEquals("halloween", pool.getRequestedMapName());
        assertFalse(pool.isRequestedMapSelected(), "the requested world does not exist");
    }

    @Test
    @DisplayName("A missing named world falls back to the default world instead of failing (US-1.04)")
    void testAMissingNamedWorldFallsBackToTheDefaultWorld(@TempDir Path root) throws IOException {
        Path worlds = worlds(root, "world", "winter");
        System.setProperty(MapPool.LOBBY_MAP_PROPERTY, "halloewen");

        assertEquals(MapPool.DEFAULT_MAP_NAME, selectedName(worlds));
    }

    @Test
    @DisplayName("A missing named world falls back even when the default world is the only one")
    void testAMissingNamedWorldFallsBackWithASingleWorld(@TempDir Path root) throws IOException {
        Path worlds = worlds(root, "world");
        System.setProperty(MapPool.LOBBY_MAP_PROPERTY, "halloween");

        assertEquals(MapPool.DEFAULT_MAP_NAME, selectedName(worlds));
    }

    @Test
    @DisplayName("Without the property the default world is selected")
    void testWithoutThePropertyTheDefaultWorldIsSelected(@TempDir Path root) throws IOException {
        Path worlds = worlds(root, "winter", "world");

        assertEquals(MapPool.DEFAULT_MAP_NAME, selectedName(worlds));
    }

    @Test
    @DisplayName("The world name is matched ignoring case")
    void testTheWorldNameIsMatchedIgnoringCase(@TempDir Path root) throws IOException {
        Path worlds = worlds(root, "world", "Halloween");
        System.setProperty(MapPool.LOBBY_MAP_PROPERTY, "halloween");

        assertEquals("Halloween", selectedName(worlds));
    }

    @Test
    @DisplayName("Without the named and the default world the first found world is used")
    void testWithoutTheNamedAndTheDefaultWorldTheFirstWorldIsUsed(@TempDir Path root) throws IOException {
        Path worlds = worlds(root, "winter");
        System.setProperty(MapPool.LOBBY_MAP_PROPERTY, "halloween");

        assertEquals("winter", selectedName(worlds));
    }

    @Test
    @DisplayName("An empty map directory is still fatal")
    void testAnEmptyMapDirectoryIsFatal(@TempDir Path root) throws IOException {
        Path worlds = worlds(root);

        assertThrows(IllegalArgumentException.class, () -> selectedName(worlds));
    }

    @Test
    @DisplayName("The pool reports every world it found")
    void testThePoolReportsEveryWorldItFound(@TempDir Path root) throws IOException {
        Path worlds = worlds(root, "world", "halloween");
        MapPool pool = new MapPool(worlds, MapPoolTest::allDirectories);

        assertEquals(2, pool.getAvailableMaps().size());
        assertTrue(pool.availableMapNames().containsAll(List.of("world", "halloween")));
    }

    @Test
    @DisplayName("The warning names the searched world and the found ones (US-1.04)")
    void testTheWarningNamesTheSearchedAndTheFoundWorlds() {
        String message = MapPool.describeMissingMap("halloewen", List.of("world", "winter"));

        assertTrue(message.contains("halloewen"), message);
        assertTrue(message.contains("world"), message);
        assertTrue(message.contains("winter"), message);
        assertTrue(message.contains(MapPool.LOBBY_MAP_PROPERTY), message);
    }
}
