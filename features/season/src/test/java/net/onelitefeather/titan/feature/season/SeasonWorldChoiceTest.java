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

import io.avaje.config.Config;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.ServiceLoader;
import net.onelitefeather.titan.core.module.LobbyWorldChoice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SeasonWorldChoiceTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    @TempDir
    Path worlds;

    private String originalZone;

    @BeforeEach
    void setUp() {
        this.originalZone = Config.get(SeasonSettings.ZONE_KEY);
    }

    @AfterEach
    void tearDown() {
        Config.asProperties().stringPropertyNames().stream().filter(key -> key.startsWith(SeasonSettings.PREFIX)).forEach(Config::clearProperty);
        Config.setProperty(SeasonSettings.ZONE_KEY, this.originalZone);
    }

    private SeasonWorldChoice choiceAt(String utcInstant) {
        return new SeasonWorldChoice(new AdjustableClock(Instant.parse(utcInstant), BERLIN), this.worlds);
    }

    private void configureWinter(boolean withWorld) throws IOException {
        if (withWorld) {
            Path directory = Files.createDirectories(this.worlds.resolve("winter-map"));
            Files.writeString(directory.resolve(SeasonSettings.MAP_FILE_NAME), "{}");
        }
        Config.setProperty(SeasonSettings.key("winter", SeasonSettings.WORLD_FIELD), "winter-map");
        Config.setProperty(SeasonSettings.key("winter", SeasonSettings.FROM_FIELD), "2026-12-01T00:00:00");
        Config.setProperty(SeasonSettings.key("winter", SeasonSettings.TO_FIELD), "2027-01-07T00:00:00");
    }

    @DisplayName("The world of the season active now is chosen")
    @Test
    void activeSeasonsWorldIsChosen() throws IOException {
        configureWinter(true);

        Assertions.assertEquals(Optional.of("winter-map"), choiceAt("2026-12-24T12:00:00Z").worldName());
    }

    @DisplayName("Outside every window the default world is left to the platform")
    @Test
    void noActiveSeasonChoosesNothing() throws IOException {
        configureWinter(true);

        Assertions.assertEquals(Optional.empty(), choiceAt("2026-06-01T12:00:00Z").worldName());
    }

    @DisplayName("Without any season configured nothing is chosen")
    @Test
    void noSeasonsChoosesNothing() {
        Assertions.assertEquals(Optional.empty(), choiceAt("2026-12-24T12:00:00Z").worldName());
    }

    @DisplayName("An invalid enabled season aborts startup, as the column does")
    @Test
    void invalidEnabledSeasonAbortsStartup() throws IOException {
        configureWinter(false);

        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> choiceAt("2026-12-24T12:00:00Z").worldName());

        Assertions.assertTrue(failure.getMessage().contains("seasons.winter.world"), "the message must name the key, was: " + failure.getMessage());
    }

    @DisplayName("The service loader finds the choice")
    @Test
    void serviceLoaderFindsTheChoice() {
        boolean found = ServiceLoader.load(LobbyWorldChoice.class).stream().anyMatch(provider -> provider.type() == SeasonWorldChoice.class);

        Assertions.assertTrue(found, "META-INF/services must register SeasonWorldChoice");
    }
}
