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
package net.onelitefeather.titan.app.feature.spawn;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.avaje.config.Configuration;
import net.onelitefeather.titan.common.config.RuntimeConfigFallback;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Plain unit tests for {@link SpawnSettings}: no {@code Config}, no server needed - just the pure
 * parsing and validation functions.
 */
class SpawnSettingsTest {

    @DisplayName("A valid minHeight is returned unchanged")
    @Test
    void aValidMinHeightIsReturnedUnchanged() {
        Assertions.assertEquals(-64, SpawnSettings.minHeight(-64, 310));
    }

    @DisplayName("minHeight equal to maxHeight is rejected, naming both full keys")
    @Test
    void minHeightEqualToMaxHeightIsRejected() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> SpawnSettings.minHeight(100, 100));

        Assertions.assertTrue(thrown.getMessage().contains(SpawnSettings.MIN_HEIGHT_KEY), "the message must name " + SpawnSettings.MIN_HEIGHT_KEY);
        Assertions.assertTrue(thrown.getMessage().contains(SpawnSettings.MAX_HEIGHT_KEY), "the message must name " + SpawnSettings.MAX_HEIGHT_KEY);
    }

    @DisplayName("minHeight above maxHeight is rejected, naming both full keys")
    @Test
    void minHeightAboveMaxHeightIsRejected() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> SpawnSettings.minHeight(400, 300));

        Assertions.assertTrue(thrown.getMessage().contains(SpawnSettings.MIN_HEIGHT_KEY), "the message must name " + SpawnSettings.MIN_HEIGHT_KEY);
        Assertions.assertTrue(thrown.getMessage().contains(SpawnSettings.MAX_HEIGHT_KEY), "the message must name " + SpawnSettings.MAX_HEIGHT_KEY);
    }

    @DisplayName("A valid simulationDistance is returned unchanged")
    @Test
    void aValidSimulationDistanceIsReturnedUnchanged() {
        Assertions.assertEquals(2, SpawnSettings.simulationDistance("2"));
    }

    @DisplayName("A zero simulationDistance is rejected")
    @Test
    void zeroSimulationDistanceIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SpawnSettings.simulationDistance("0"));
    }

    @DisplayName("A negative simulationDistance is rejected")
    @Test
    void negativeSimulationDistanceIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SpawnSettings.simulationDistance("-1"));
    }

    @DisplayName("A non-numeric simulationDistance fails to parse")
    @Test
    void nonNumericSimulationDistanceFailsToParse() {
        Assertions.assertThrows(NumberFormatException.class, () -> SpawnSettings.simulationDistance("abc"));
    }

    private static RuntimeConfigFallback freshFallback() {
        return new RuntimeConfigFallback(Configuration.builder().build());
    }

    @DisplayName("resolveMaxHeight passes a valid value through unchanged")
    @Test
    void resolveMaxHeightPassesAValidValueThrough() {
        Assertions.assertEquals(310, SpawnSettings.resolveMaxHeight("310", 310, freshFallback()));
    }

    @DisplayName("resolveMaxHeight falls back to the shipped default and warns once for a non-numeric value")
    @Test
    void resolveMaxHeightFallsBackAndWarnsForANonNumericValue() {
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            int result = SpawnSettings.resolveMaxHeight("abc", 310, freshFallback());

            Assertions.assertEquals(310, result);
            Assertions.assertEquals(1, appender.list.size(), "exactly one WARN must be logged for a new invalid value");
            Assertions.assertEquals(SpawnSettings.MAX_HEIGHT_KEY, appender.list.get(0).getArgumentArray()[0]);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("resolveMinHeight passes a valid value through unchanged")
    @Test
    void resolveMinHeightPassesAValidValueThrough() {
        Assertions.assertEquals(-64, SpawnSettings.resolveMinHeight("-64", 310, -64, freshFallback()));
    }

    @DisplayName("resolveMinHeight falls back to the shipped default and warns once when not less than maxHeight")
    @Test
    void resolveMinHeightFallsBackAndWarnsWhenNotLessThanMaxHeight() {
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            int result = SpawnSettings.resolveMinHeight("400", 300, -64, freshFallback());

            Assertions.assertEquals(-64, result, "a minHeight above maxHeight must fall back to the shipped default");
            Assertions.assertEquals(1, appender.list.size(), "exactly one WARN must be logged for a new invalid value");
            Assertions.assertEquals(SpawnSettings.MIN_HEIGHT_KEY, appender.list.get(0).getArgumentArray()[0]);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("resolveSimulationDistance passes a valid value through unchanged")
    @Test
    void resolveSimulationDistancePassesAValidValueThrough() {
        Assertions.assertEquals(2, SpawnSettings.resolveSimulationDistance("2", 2, freshFallback()));
    }

    @DisplayName("resolveSimulationDistance falls back to the shipped default and warns once for a zero value")
    @Test
    void resolveSimulationDistanceFallsBackAndWarnsForAZeroValue() {
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            int result = SpawnSettings.resolveSimulationDistance("0", 2, freshFallback());

            Assertions.assertEquals(2, result);
            Assertions.assertEquals(1, appender.list.size(), "exactly one WARN must be logged for a new invalid value");
            Assertions.assertEquals(SpawnSettings.SIMULATION_DISTANCE_KEY, appender.list.get(0).getArgumentArray()[0]);
        } finally {
            logger.detachAppender(appender);
        }
    }
}
