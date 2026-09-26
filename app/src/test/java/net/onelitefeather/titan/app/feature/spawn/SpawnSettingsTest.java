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

    @DisplayName("parseHeightBounds passes a valid pair through unchanged")
    @Test
    void parseHeightBoundsPassesAValidPairThrough() {
        SpawnSettings.HeightSettings result = SpawnSettings.parseHeightBounds(new SpawnSettings.RawHeightBounds("-64", "310"));

        Assertions.assertEquals(-64, result.minHeight());
        Assertions.assertEquals(310, result.maxHeight());
    }

    @DisplayName("parseHeightBounds rejects a minHeight not less than maxHeight, naming both full keys")
    @Test
    void parseHeightBoundsRejectsAMinHeightNotLessThanMaxHeight() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> SpawnSettings.parseHeightBounds(new SpawnSettings.RawHeightBounds("400", "300")));

        Assertions.assertTrue(thrown.getMessage().contains(SpawnSettings.MIN_HEIGHT_KEY), "the message must name " + SpawnSettings.MIN_HEIGHT_KEY);
        Assertions.assertTrue(thrown.getMessage().contains(SpawnSettings.MAX_HEIGHT_KEY), "the message must name " + SpawnSettings.MAX_HEIGHT_KEY);
    }

    @DisplayName("parseHeightBounds rejects a non-numeric maxHeight, naming its own key")
    @Test
    void parseHeightBoundsRejectsANonNumericMaxHeight() {
        NumberFormatException thrown = Assertions.assertThrows(NumberFormatException.class, () -> SpawnSettings.parseHeightBounds(new SpawnSettings.RawHeightBounds("-64", "abc")));

        Assertions.assertTrue(thrown.getMessage().contains(SpawnSettings.MAX_HEIGHT_KEY), "the message must name " + SpawnSettings.MAX_HEIGHT_KEY);
    }

    @DisplayName("resolve passes a valid height bounds pair through unchanged, without warning")
    @Test
    void resolveHeightBoundsPassesAValidPairThrough() {
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            SpawnSettings.HeightSettings result = freshFallback().resolve(SpawnSettings.HEIGHT_BOUNDS_KEY, new SpawnSettings.RawHeightBounds("-64", "310"), SpawnSettings::parseHeightBounds, () -> new SpawnSettings.HeightSettings(0, 100));

            Assertions.assertEquals(-64, result.minHeight());
            Assertions.assertEquals(310, result.maxHeight());
            Assertions.assertTrue(appender.list.isEmpty(), "a valid pair must never warn");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("resolve falls back to the shipped height bounds and warns once when minHeight is not less than maxHeight")
    @Test
    void resolveHeightBoundsFallsBackAndWarnsWhenNotLessThanMaxHeight() {
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            SpawnSettings.HeightSettings shippedDefault = new SpawnSettings.HeightSettings(-64, 310);
            SpawnSettings.HeightSettings result = freshFallback().resolve(SpawnSettings.HEIGHT_BOUNDS_KEY, new SpawnSettings.RawHeightBounds("400", "300"), SpawnSettings::parseHeightBounds, () -> shippedDefault);

            Assertions.assertEquals(shippedDefault, result, "an invalid pair must fall back to the shipped height bounds");
            Assertions.assertEquals(1, appender.list.size(), "exactly one WARN must be logged for a new invalid pair");
            Assertions.assertEquals(SpawnSettings.HEIGHT_BOUNDS_KEY, appender.list.get(0).getArgumentArray()[0]);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("shippedHeightBounds reads minHeight and maxHeight from the given configuration")
    @Test
    void shippedHeightBoundsReadsFromTheGivenConfiguration() {
        Configuration shipped = Configuration.builder().put(SpawnSettings.MIN_HEIGHT_KEY, "-64").put(SpawnSettings.MAX_HEIGHT_KEY, "310").build();

        SpawnSettings.HeightSettings result = SpawnSettings.shippedHeightBounds(shipped);

        Assertions.assertEquals(-64, result.minHeight());
        Assertions.assertEquals(310, result.maxHeight());
    }

    @DisplayName("resolve passes a valid simulationDistance through unchanged")
    @Test
    void resolveSimulationDistancePassesAValidValueThrough() {
        int result = freshFallback().resolve(SpawnSettings.SIMULATION_DISTANCE_KEY, "2", SpawnSettings::simulationDistance, () -> 2);

        Assertions.assertEquals(2, result);
    }

    @DisplayName("resolve falls back to the shipped simulationDistance and warns once for a zero value")
    @Test
    void resolveSimulationDistanceFallsBackAndWarnsForAZeroValue() {
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            int result = freshFallback().resolve(SpawnSettings.SIMULATION_DISTANCE_KEY, "0", SpawnSettings::simulationDistance, () -> 2);

            Assertions.assertEquals(2, result);
            Assertions.assertEquals(1, appender.list.size(), "exactly one WARN must be logged for a new invalid value");
            Assertions.assertEquals(SpawnSettings.SIMULATION_DISTANCE_KEY, appender.list.get(0).getArgumentArray()[0]);
        } finally {
            logger.detachAppender(appender);
        }
    }
}
