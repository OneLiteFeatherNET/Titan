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
package net.onelitefeather.titan.common.season;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.titan.common.feature.ReleaseStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers US-4.01, US-4.03 and US-4.04: a season is values in a file, and a file that cannot be
 * read is a startup failure that names what is wrong with it.
 */
class SeasonLoaderTest {

    private final SeasonLoader loader = SeasonLoader.create(SeasonFixtures.BERLIN);

    @Test
    @DisplayName("a season is read out of a file, values and all")
    void readsASeasonFromAFile(@TempDir Path directory) throws IOException {
        Path file = SeasonFixtures.write(directory, "lanterns", """
                {
                  "id": "lanterns",
                  "priority": 42,
                  "stage": "lite",
                  "world": "lantern-nights",
                  "window": { "from": "2026-10-01", "to": "2026-11-05T04:00", "zone": "Europe/Berlin" },
                  "effects": [
                    { "type": "place_decoration", "position": { "x": 1.5, "y": 65, "z": -3.5 }, "block": "minecraft:jack_o_lantern" },
                    { "type": "message_prefix", "prefix": "<gold>[Lanterns] " }
                  ]
                }
                """);

        SeasonDefinition definition = this.loader.load(file);

        assertEquals("lanterns", definition.id());
        assertEquals(42, definition.priority());
        assertEquals(ReleaseStage.LITE, definition.stage());
        assertEquals("lantern-nights", definition.world());
        assertTrue(definition.enabled(), "a file that does not say otherwise is switched on");
        assertEquals(LocalDateTime.parse("2026-10-01T00:00"), definition.window().from());
        assertEquals(LocalDateTime.parse("2026-11-05T04:00"), definition.window().to());
        assertEquals(SeasonFixtures.BERLIN, definition.window().zone());
        assertEquals(2, definition.effects().size());
        assertEquals(new Pos(1.5, 65, -3.5), ((SeasonEffect.PlaceDecoration) definition.effects().getFirst()).position());
    }

    @Test
    @DisplayName("an unknown effect type fails at load and the message names it")
    void unknownEffectTypeFailsAtLoad(@TempDir Path directory) throws IOException {
        Path file = SeasonFixtures.write(directory, "broken", """
                {
                  "id": "broken",
                  "effects": [ { "type": "summon_pumpkin_king", "position": { "x": 0, "y": 0, "z": 0 } } ]
                }
                """);

        SeasonConfigurationException exception = assertThrows(SeasonConfigurationException.class, () -> this.loader.load(file));

        assertTrue(exception.getMessage().contains("summon_pumpkin_king"), "the message must name the type that was written: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("place_decoration"), "the message must list the types that do exist: " + exception.getMessage());
        assertEquals("broken.json", exception.source());
    }

    @Test
    @DisplayName("an effect with no type at all is refused just as loudly")
    void missingEffectTypeFailsAtLoad(@TempDir Path directory) throws IOException {
        Path file = SeasonFixtures.write(directory, "typeless", """
                { "id": "typeless", "effects": [ { "prefix": "<gold>hi" } ] }
                """);

        SeasonConfigurationException exception = assertThrows(SeasonConfigurationException.class, () -> this.loader.load(file));

        assertTrue(exception.getMessage().contains("no type"), exception.getMessage());
    }

    @Test
    @DisplayName("a misspelt block is caught while reading the file, not while placing it")
    void unknownBlockFailsAtLoad(@TempDir Path directory) throws IOException {
        Path file = SeasonFixtures.write(directory, "typo", """
                {
                  "id": "typo",
                  "effects": [ { "type": "place_decoration", "position": { "x": 0, "y": 0, "z": 0 }, "block": "minecraft:jack_o_lanturn" } ]
                }
                """);

        SeasonConfigurationException exception = assertThrows(SeasonConfigurationException.class, () -> this.loader.load(file));

        assertTrue(exception.getMessage().contains("jack_o_lanturn"), exception.getMessage());
    }

    @Test
    @DisplayName("an effect missing a required value is refused, naming the effect")
    void incompleteEffectFailsAtLoad(@TempDir Path directory) throws IOException {
        Path file = SeasonFixtures.write(directory, "half", """
                {
                  "id": "half",
                  "effects": [ { "type": "place_decoration", "position": { "x": 0, "y": 0, "z": 0 } } ]
                }
                """);

        SeasonConfigurationException exception = assertThrows(SeasonConfigurationException.class, () -> this.loader.load(file));

        assertTrue(exception.getMessage().contains("place_decoration needs a block"), exception.getMessage());
    }

    @Test
    @DisplayName("an unreadable date is refused rather than turned into an open season")
    void unreadableWindowFailsAtLoad(@TempDir Path directory) throws IOException {
        Path file = SeasonFixtures.write(directory, "whenever", """
                { "id": "whenever", "window": { "from": "1. Oktober" } }
                """);

        SeasonConfigurationException exception = assertThrows(SeasonConfigurationException.class, () -> this.loader.load(file));

        assertTrue(exception.getMessage().contains("1. Oktober"), exception.getMessage());
    }

    @Test
    @DisplayName("a window that ends before it starts is refused")
    void backwardsWindowFailsAtLoad(@TempDir Path directory) throws IOException {
        Path file = SeasonFixtures.write(directory, "backwards", """
                { "id": "backwards", "window": { "from": "2026-12-01", "to": "2026-11-01" } }
                """);

        assertThrows(SeasonConfigurationException.class, () -> this.loader.load(file));
    }

    @Test
    @DisplayName("an unknown stage is refused, not silently narrowed")
    void unknownStageFailsAtLoad(@TempDir Path directory) throws IOException {
        Path file = SeasonFixtures.write(directory, "stagey", """
                { "id": "stagey", "stage": "intern" }
                """);

        SeasonConfigurationException exception = assertThrows(SeasonConfigurationException.class, () -> this.loader.load(file));

        assertTrue(exception.getMessage().contains("intern"), exception.getMessage());
    }

    @Test
    @DisplayName("a season with no window at all is always within it")
    void aSeasonWithoutAWindowIsAlwaysOpen(@TempDir Path directory) throws IOException {
        Path file = SeasonFixtures.write(directory, "eternal", """
                { "id": "eternal", "stage": "ga" }
                """);

        SeasonDefinition definition = this.loader.load(file);

        assertNull(definition.window().from());
        assertNull(definition.window().to());
        assertFalse(definition.window().hasEnd());
        assertEquals(SeasonFixtures.BERLIN, definition.window().zone());
        assertEquals(ReleaseStage.GA, definition.stage());
    }

    @Test
    @DisplayName("no season directory is not an error - the lobby runs without seasons")
    void anAbsentDirectoryLoadsNothing(@TempDir Path directory) {
        assertEquals(List.of(), this.loader.loadAll(directory.resolve("not-there")));
    }

    @Test
    @DisplayName("two files claiming the same id are refused, naming both")
    void duplicateIdsAreRefused(@TempDir Path directory) throws IOException {
        SeasonFixtures.write(directory, "one", SeasonFixtures.seasonJson("twin", 1, null, null, "minecraft:stone"));
        SeasonFixtures.write(directory, "two", SeasonFixtures.seasonJson("twin", 2, null, null, "minecraft:dirt"));

        SeasonConfigurationException exception = assertThrows(SeasonConfigurationException.class, () -> this.loader.loadAll(directory));

        assertTrue(exception.getMessage().contains("twin"), exception.getMessage());
    }

    @Test
    @DisplayName("a window that names a season needs a resolver, and says so when there is none")
    void aNamedWindowWithoutAResolverFailsWithAnExplanation(@TempDir Path directory) throws IOException {
        Path file = SeasonFixtures.write(directory, "wintry", """
                { "id": "wintry", "window": { "named": "WINTER", "year": 2026 } }
                """);

        SeasonConfigurationException exception = assertThrows(SeasonConfigurationException.class, () -> this.loader.load(file));

        assertTrue(exception.getMessage().contains("WINTER"), exception.getMessage());
        assertTrue(exception.getMessage().contains("NamedWindowResolver"), "the message has to point at the seam: " + exception.getMessage());
    }

    @Test
    @DisplayName("the seam works: a resolver turns a named window into dates")
    void aNamedWindowIsResolvedByTheInstalledResolver(@TempDir Path directory) throws IOException {
        // Stands in for what spec stage 2 will install: a SeasonBoundaryStrategy answering where
        // winter starts and ends in a given year. Nothing in the season package knows those types.
        NamedWindowResolver resolver = (name, year, zone) -> !"WINTER".equals(name) ? Optional.empty() : Optional.of(new SeasonWindow(LocalDateTime.of(year, 12, 21, 0, 0), LocalDateTime.of(year + 1, 3, 20, 0, 0), zone));
        Path file = SeasonFixtures.write(directory, "wintry", """
                { "id": "wintry", "window": { "named": "WINTER", "year": 2026, "zone": "Europe/Berlin" } }
                """);

        SeasonDefinition definition = SeasonLoader.create(SeasonFixtures.BERLIN, resolver).load(file);

        assertEquals(LocalDateTime.of(2026, 12, 21, 0, 0), definition.window().from());
        assertEquals(LocalDateTime.of(2027, 3, 20, 0, 0), definition.window().to());
    }

    @Test
    @DisplayName("a window cannot both name a season and spell itself out")
    void aNamedWindowAndExplicitDatesAreRefused(@TempDir Path directory) throws IOException {
        Path file = SeasonFixtures.write(directory, "both", """
                { "id": "both", "window": { "named": "WINTER", "year": 2026, "from": "2026-12-01" } }
                """);

        assertThrows(SeasonConfigurationException.class, () -> this.loader.load(file));
    }

    @Test
    @DisplayName("files that are not seasons are ignored, not refused")
    void nonSeasonFilesAreIgnored(@TempDir Path directory) throws IOException {
        SeasonFixtures.write(directory, "real", SeasonFixtures.seasonJson("real", 0, null, null, "minecraft:stone"));
        Files.writeString(directory.resolve("README.md"), "not a season");

        assertEquals(1, this.loader.loadAll(directory).size());
    }
}
