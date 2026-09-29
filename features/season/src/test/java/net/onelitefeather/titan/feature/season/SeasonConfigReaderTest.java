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
package net.onelitefeather.titan.feature.season;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.avaje.config.Config;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class SeasonConfigReaderTest {

    private final Logger readerLogger = (Logger) LoggerFactory.getLogger(SeasonConfigReader.class);
    private final ListAppender<ILoggingEvent> logLines = new ListAppender<>();

    @TempDir
    Path worlds;

    private String originalZone;
    private SeasonConfigReader reader;

    @BeforeEach
    void setUp() {
        this.originalZone = Config.get(SeasonSettings.ZONE_KEY);
        this.reader = new SeasonConfigReader(Config.asConfiguration(), this.worlds, new SeasonCalendar());
        this.logLines.start();
        this.readerLogger.addAppender(this.logLines);
    }

    @AfterEach
    void tearDown() {
        this.readerLogger.detachAppender(this.logLines);
        Config.asProperties().stringPropertyNames().stream().filter(key -> key.startsWith(SeasonSettings.PREFIX)).forEach(Config::clearProperty);
        Config.setProperty(SeasonSettings.ZONE_KEY, this.originalZone);
    }

    private void createWorld(String name, boolean withMapFile) throws IOException {
        Path directory = Files.createDirectories(this.worlds.resolve(name));
        if (withMapFile) {
            Files.writeString(directory.resolve(SeasonSettings.MAP_FILE_NAME), "{}");
        }
    }

    private static void configure(String id, String world, String from, String to) {
        set(id, SeasonSettings.WORLD_FIELD, world);
        set(id, SeasonSettings.FROM_FIELD, from);
        set(id, SeasonSettings.TO_FIELD, to);
    }

    private static void set(String id, String field, String value) {
        Config.setProperty(SeasonSettings.key(id, field), value);
    }

    private IllegalStateException startupFailure() {
        return Assertions.assertThrows(IllegalStateException.class, this.reader::readAtStartup);
    }

    private List<ILoggingEvent> warnings() {
        return this.logLines.list.stream().filter(line -> line.getLevel() == Level.WARN).toList();
    }

    @DisplayName("The shipped defaults use Europe/Berlin as the zone")
    @Test
    void shippedDefaultZoneIsBerlin() {
        Assertions.assertEquals(ZoneId.of("Europe/Berlin"), this.reader.readAtStartup().zone());
    }

    @DisplayName("Without any season the list is empty")
    @Test
    void noSeasonsConfigured() {
        Assertions.assertEquals(List.of(), this.reader.readAtStartup().seasons());
    }

    @DisplayName("A configured season is read with its window and world")
    @Test
    void readsAConfiguredSeason() throws IOException {
        createWorld("winter", true);
        configure("winter", "winter", "2026-12-01T00:00:00", "2027-01-07T00:00:00");

        Season expected = new Season("winter", "winter", LocalDateTime.parse("2026-12-01T00:00:00"), LocalDateTime.parse("2027-01-07T00:00:00"), true);
        Assertions.assertEquals(List.of(expected), this.reader.readAtStartup().seasons());
    }

    @DisplayName("A season is enabled unless switched off")
    @Test
    void enabledDefaultsToTrue() throws IOException {
        createWorld("winter", true);
        configure("winter", "winter", "2026-12-01T00:00:00", "2027-01-07T00:00:00");

        Assertions.assertTrue(this.reader.readAtStartup().seasons().getFirst().enabled());
    }

    @DisplayName("A missing world aborts startup and names seasons.<id>.world")
    @Test
    void missingWorldValueAbortsStartup() {
        set("winter", SeasonSettings.FROM_FIELD, "2026-12-01T00:00:00");
        set("winter", SeasonSettings.TO_FIELD, "2027-01-07T00:00:00");

        Assertions.assertTrue(startupFailure().getMessage().contains("seasons.winter.world"));
    }

    @DisplayName("A missing from aborts startup and names seasons.<id>.from")
    @Test
    void missingFromAbortsStartup() throws IOException {
        createWorld("winter", true);
        set("winter", SeasonSettings.WORLD_FIELD, "winter");
        set("winter", SeasonSettings.TO_FIELD, "2027-01-07T00:00:00");

        Assertions.assertTrue(startupFailure().getMessage().contains("seasons.winter.from"));
    }

    @DisplayName("A missing to aborts startup and names seasons.<id>.to")
    @Test
    void missingToAbortsStartup() throws IOException {
        createWorld("winter", true);
        set("winter", SeasonSettings.WORLD_FIELD, "winter");
        set("winter", SeasonSettings.FROM_FIELD, "2026-12-01T00:00:00");

        Assertions.assertTrue(startupFailure().getMessage().contains("seasons.winter.to"));
    }

    @DisplayName("An unreadable date aborts startup and names the key and the value")
    @Test
    void unreadableDateAbortsStartup() throws IOException {
        createWorld("winter", true);
        configure("winter", "winter", "morgen", "2027-01-07T00:00:00");

        String message = startupFailure().getMessage();

        Assertions.assertTrue(message.contains("seasons.winter.from") && message.contains("morgen"), "was: " + message);
    }

    @DisplayName("A window that is not from before to aborts startup and names both keys")
    @Test
    void reversedWindowAbortsStartup() throws IOException {
        createWorld("winter", true);
        configure("winter", "winter", "2027-01-07T00:00:00", "2026-12-01T00:00:00");

        String message = startupFailure().getMessage();

        Assertions.assertTrue(message.contains("seasons.winter.from") && message.contains("seasons.winter.to"), "was: " + message);
    }

    @DisplayName("A window with from equal to to aborts startup")
    @Test
    void emptyWindowAbortsStartup() throws IOException {
        createWorld("winter", true);
        configure("winter", "winter", "2026-12-01T00:00:00", "2026-12-01T00:00:00");

        Assertions.assertTrue(startupFailure().getMessage().contains("seasons.winter.from"));
    }

    @DisplayName("A world directory that does not exist aborts startup and names seasons.<id>.world")
    @Test
    void missingWorldDirectoryAbortsStartup() {
        configure("winter", "winter", "2026-12-01T00:00:00", "2027-01-07T00:00:00");

        String message = startupFailure().getMessage();

        Assertions.assertTrue(message.contains("seasons.winter.world") && message.contains("winter"), "was: " + message);
    }

    @DisplayName("A world directory without map.json aborts startup and names seasons.<id>.world")
    @Test
    void worldWithoutMapFileAbortsStartup() throws IOException {
        createWorld("winter", false);
        configure("winter", "winter", "2026-12-01T00:00:00", "2027-01-07T00:00:00");

        String message = startupFailure().getMessage();

        Assertions.assertTrue(message.contains("seasons.winter.world") && message.contains(SeasonSettings.MAP_FILE_NAME), "was: " + message);
    }

    @DisplayName("A world outside worlds/ aborts startup")
    @Test
    void worldPathEscapingTheWorldsDirectoryAbortsStartup() {
        configure("winter", "../elsewhere", "2026-12-01T00:00:00", "2027-01-07T00:00:00");

        Assertions.assertTrue(startupFailure().getMessage().contains("seasons.winter.world"));
    }

    @DisplayName("An invalid zone aborts startup and names seasons.zone")
    @Test
    void invalidZoneAbortsStartup() {
        Config.setProperty(SeasonSettings.ZONE_KEY, "Mars/Olympus");

        String message = startupFailure().getMessage();

        Assertions.assertTrue(message.contains(SeasonSettings.ZONE_KEY) && message.contains("Mars/Olympus"), "was: " + message);
    }

    @DisplayName("The reserved id zone aborts startup and names seasons.zone")
    @Test
    void reservedIdAbortsStartup() {
        set(SeasonSettings.RESERVED_ID, SeasonSettings.WORLD_FIELD, "winter");

        Assertions.assertTrue(startupFailure().getMessage().contains(SeasonSettings.ZONE_KEY));
    }

    @DisplayName("A disabled season is not validated")
    @Test
    void disabledSeasonIsNotValidated() {
        set("winter", SeasonSettings.WORLD_FIELD, "winter");
        set("winter", SeasonSettings.FROM_FIELD, "morgen");
        set("winter", SeasonSettings.ENABLED_FIELD, "false");

        Assertions.assertEquals(List.of(), this.reader.readAtStartup().seasons());
    }

    @DisplayName("Overlapping seasons log one warning naming both ids and the winner")
    @Test
    void overlapLogsAWarningWithBothIds() throws IOException {
        createWorld("autumn", true);
        createWorld("halloween", true);
        configure("autumn", "autumn", "2026-10-01T00:00:00", "2026-11-15T00:00:00");
        configure("halloween", "halloween", "2026-10-25T00:00:00", "2026-11-02T00:00:00");

        this.reader.readAtStartup();

        Assertions.assertEquals(1, warnings().size(), "got: " + warnings());
        Assertions.assertEquals("Seasons autumn and halloween overlap, autumn wins", warnings().getFirst().getFormattedMessage());
    }

    @DisplayName("Overlapping seasons do not abort startup")
    @Test
    void overlapDoesNotAbortStartup() throws IOException {
        createWorld("autumn", true);
        createWorld("halloween", true);
        configure("autumn", "autumn", "2026-10-01T00:00:00", "2026-11-15T00:00:00");
        configure("halloween", "halloween", "2026-10-25T00:00:00", "2026-11-02T00:00:00");

        Assertions.assertEquals(2, this.reader.readAtStartup().seasons().size());
    }

    @DisplayName("Live, a valid configuration is read like at startup")
    @Test
    void liveReadReturnsAValidConfiguration() throws IOException {
        createWorld("winter", true);
        configure("winter", "winter", "2026-12-01T00:00:00", "2027-01-07T00:00:00");

        Optional<SeasonConfigReader.SeasonConfig> config = this.reader.readLive();

        Assertions.assertEquals(List.of("winter"), config.orElseThrow().seasons().stream().map(Season::id).toList());
    }

    @DisplayName("Live, an enabled season without a world is unusable and warns naming seasons.<id>.world")
    @Test
    void liveEnabledSeasonWithoutWorldWarns() {
        configure("winter", "winter", "2026-12-01T00:00:00", "2027-01-07T00:00:00");

        Optional<SeasonConfigReader.SeasonConfig> config = this.reader.readLive();

        Assertions.assertTrue(config.isEmpty(), "the whole configuration is unusable while a season is broken");
        Assertions.assertEquals(1, warnings().size(), "got: " + warnings());
        Assertions.assertTrue(warnings().getFirst().getFormattedMessage().contains("seasons.winter.world"));
    }

    @DisplayName("Live, the same broken value warns once, a changed broken value warns again")
    @Test
    void liveWarningIsLoggedOncePerValue() throws IOException {
        createWorld("winter", true);
        configure("winter", "winter", "morgen", "2027-01-07T00:00:00");

        this.reader.readLive();
        this.reader.readLive();
        Assertions.assertEquals(1, warnings().size(), "the same value must warn once");

        set("winter", SeasonSettings.FROM_FIELD, "übermorgen");
        this.reader.readLive();
        Assertions.assertEquals(2, warnings().size(), "a different broken value must warn again");
    }

    @DisplayName("Live, a broken value that was fixed and broken again warns again")
    @Test
    void liveWarningRepeatsAfterTheValueWasFixed() throws IOException {
        createWorld("winter", true);
        configure("winter", "winter", "morgen", "2027-01-07T00:00:00");
        this.reader.readLive();

        set("winter", SeasonSettings.FROM_FIELD, "2026-12-01T00:00:00");
        this.reader.readLive();
        set("winter", SeasonSettings.FROM_FIELD, "morgen");
        this.reader.readLive();

        Assertions.assertEquals(2, warnings().size(), "got: " + warnings());
    }

    @DisplayName("Live, an invalid zone is unusable and warns naming seasons.zone")
    @Test
    void liveInvalidZoneWarns() {
        Config.setProperty(SeasonSettings.ZONE_KEY, "Mars/Olympus");

        Optional<SeasonConfigReader.SeasonConfig> config = this.reader.readLive();

        Assertions.assertTrue(config.isEmpty());
        Assertions.assertTrue(warnings().getFirst().getFormattedMessage().contains(SeasonSettings.ZONE_KEY));
    }

    @DisplayName("Live, a disabled broken season is ignored without a warning")
    @Test
    void liveDisabledBrokenSeasonIsSilent() {
        set("winter", SeasonSettings.WORLD_FIELD, "winter");
        set("winter", SeasonSettings.ENABLED_FIELD, "false");

        Assertions.assertTrue(this.reader.readLive().isPresent());
        Assertions.assertTrue(warnings().isEmpty(), "got: " + warnings());
    }
}
