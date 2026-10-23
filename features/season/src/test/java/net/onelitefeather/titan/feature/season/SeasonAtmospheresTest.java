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
import java.util.Optional;
import net.onelitefeather.titan.core.module.LobbyAtmosphere;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class SeasonAtmospheresTest {

    private static final String HALLOWEEN_ATMOSPHERE = SeasonSettings.key("halloween", SeasonSettings.ATMOSPHERE_FIELD);

    private final Logger atmospheresLogger = (Logger) LoggerFactory.getLogger(SeasonAtmospheres.class);
    private final ListAppender<ILoggingEvent> logLines = new ListAppender<>();

    private String originalZone;

    @BeforeEach
    void setUp() {
        this.originalZone = Config.get(SeasonSettings.ZONE_KEY);
        this.logLines.start();
        this.atmospheresLogger.addAppender(this.logLines);
    }

    @AfterEach
    void tearDown() {
        this.atmospheresLogger.detachAppender(this.logLines);
        Config.asProperties().stringPropertyNames().stream().filter(key -> key.startsWith(SeasonSettings.PREFIX)).forEach(Config::clearProperty);
        Config.setProperty(SeasonSettings.ZONE_KEY, this.originalZone);
    }

    private static Optional<LobbyAtmosphere> readAtmosphere() {
        return new SeasonAtmospheres(Config.asConfiguration()).of("halloween");
    }

    @DisplayName("A season without an atmosphere has none")
    @Test
    void missingAtmosphereIsNone() {
        Assertions.assertEquals(Optional.empty(), readAtmosphere(), "a season without seasons.halloween.atmosphere must run without one");
        Assertions.assertTrue(this.logLines.list.isEmpty(), "a missing atmosphere is not a problem and must not be logged");
    }

    @DisplayName("The key slender-fog selects the slender fog")
    @Test
    void slenderFogIsSelected() {
        Config.setProperty(HALLOWEEN_ATMOSPHERE, "slender-fog");

        Assertions.assertEquals(Optional.of(LobbyAtmosphere.SLENDER_FOG), readAtmosphere(), "slender-fog must select SLENDER_FOG");
    }

    @DisplayName("Surrounding whitespace in the value is ignored")
    @Test
    void surroundingWhitespaceIsIgnored() {
        Config.setProperty(HALLOWEEN_ATMOSPHERE, "  slender-fog ");

        Assertions.assertEquals(Optional.of(LobbyAtmosphere.SLENDER_FOG), readAtmosphere(), "padding around the value must not make it unknown");
    }

    @DisplayName("An empty value means no atmosphere, without a warning")
    @Test
    void emptyValueIsNone() {
        Config.setProperty(HALLOWEEN_ATMOSPHERE, "   ");

        Assertions.assertEquals(Optional.empty(), readAtmosphere(), "a blank value must count as absent");
        Assertions.assertTrue(this.logLines.list.isEmpty(), "a blank value must not be logged");
    }

    @DisplayName("An unknown value runs the season without an atmosphere and logs one warning naming the key")
    @Test
    void unknownValueWarnsAndIsNone() {
        Config.setProperty(HALLOWEEN_ATMOSPHERE, "fog");

        Assertions.assertEquals(Optional.empty(), readAtmosphere(), "an unknown atmosphere must not be applied");
        Assertions.assertEquals(1, this.logLines.list.size(), "exactly one line must be logged for the unknown value");
        ILoggingEvent line = this.logLines.list.getFirst();
        Assertions.assertEquals(Level.WARN, line.getLevel(), "the unknown value must be logged as a warning");
        Assertions.assertTrue(line.getFormattedMessage().contains(HALLOWEEN_ATMOSPHERE), "the warning must name the key, was: " + line.getFormattedMessage());
        Assertions.assertTrue(line.getFormattedMessage().contains("slender-fog"), "the warning must name the known values, was: " + line.getFormattedMessage());
    }
}
