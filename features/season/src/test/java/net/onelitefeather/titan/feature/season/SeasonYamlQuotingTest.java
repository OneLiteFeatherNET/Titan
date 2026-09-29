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
import io.avaje.config.Configuration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

/**
 * Loads real YAML through avaje-config with SnakeYAML on the classpath, as production does. That
 * loader drops unquoted date-times, so the reader must say that they need quotes.
 */
class SeasonYamlQuotingTest {

    private static final String FROM_HINT = "(quote date-times: from: \"2026-12-01T00:00:00\")";
    private static final String TO_HINT = "(quote date-times: to: \"2026-12-01T00:00:00\")";

    private final Logger readerLogger = (Logger) LoggerFactory.getLogger(SeasonConfigReader.class);
    private final ListAppender<ILoggingEvent> logLines = new ListAppender<>();

    @TempDir
    Path directory;

    @BeforeEach
    void setUp() {
        this.logLines.start();
        this.readerLogger.addAppender(this.logLines);
    }

    @AfterEach
    void tearDown() {
        this.readerLogger.detachAppender(this.logLines);
    }

    private SeasonConfigReader readerFor(String from, String to) throws IOException {
        Path worlds = Files.createDirectories(this.directory.resolve("worlds"));
        Files.writeString(Files.createDirectories(worlds.resolve("winter")).resolve(SeasonSettings.MAP_FILE_NAME), "{}");
        Path file = this.directory.resolve("season.yaml");
        Files.writeString(file, """
                seasons:
                  zone: Europe/Berlin
                  winter:
                    world: winter
                    from: %s
                    to: %s
                """.formatted(from, to));
        return new SeasonConfigReader(Configuration.builder().load(file.toFile()).build(), worlds, new SeasonCalendar());
    }

    private List<ILoggingEvent> warnings() {
        return this.logLines.list.stream().filter(line -> line.getLevel() == Level.WARN).toList();
    }

    @DisplayName("Quoted date-times in YAML are read as the season window")
    @Test
    void quotedDateTimesAreRead() throws IOException {
        SeasonConfigReader reader = readerFor("\"2026-12-01T00:00:00\"", "\"2027-01-07T00:00:00\"");

        Season expected = new Season("winter", "winter", LocalDateTime.parse("2026-12-01T00:00:00"), LocalDateTime.parse("2027-01-07T00:00:00"), true);
        Assertions.assertEquals(List.of(expected), reader.readAtStartup().seasons());
    }

    @DisplayName("An unquoted from aborts startup naming seasons.<id>.from and the quoting hint")
    @Test
    void unquotedFromAbortsStartupWithHint() throws IOException {
        SeasonConfigReader reader = readerFor("2026-12-01T00:00:00", "\"2027-01-07T00:00:00\"");

        String message = Assertions.assertThrows(IllegalStateException.class, reader::readAtStartup).getMessage();

        Assertions.assertTrue(message.contains("seasons.winter.from") && message.contains(FROM_HINT), "was: " + message);
    }

    @DisplayName("An unquoted to aborts startup naming seasons.<id>.to and the quoting hint")
    @Test
    void unquotedToAbortsStartupWithHint() throws IOException {
        SeasonConfigReader reader = readerFor("\"2026-12-01T00:00:00\"", "2027-01-07T00:00:00");

        String message = Assertions.assertThrows(IllegalStateException.class, reader::readAtStartup).getMessage();

        Assertions.assertTrue(message.contains("seasons.winter.to") && message.contains(TO_HINT), "was: " + message);
    }

    @DisplayName("Live, an unquoted date-time yields no configuration and warns once with the quoting hint")
    @Test
    void liveUnquotedDateTimeWarnsOnceWithHint() throws IOException {
        SeasonConfigReader reader = readerFor("2026-12-01T00:00:00", "\"2027-01-07T00:00:00\"");

        boolean firstEmpty = reader.readLive().isEmpty();
        reader.readLive();

        Assertions.assertTrue(firstEmpty, "an unquoted value counts as missing");
        Assertions.assertEquals(1, warnings().size(), "got: " + warnings());
        String message = warnings().getFirst().getFormattedMessage();
        Assertions.assertTrue(message.contains("seasons.winter.from") && message.contains(FROM_HINT), "was: " + message);
    }
}
