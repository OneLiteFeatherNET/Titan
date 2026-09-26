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
package net.onelitefeather.titan.app.feature.sit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.avaje.config.Configuration;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.config.RuntimeConfigFallback;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

/**
 * Plain unit tests for {@link SitSettings}: no {@code Config}, no server needed - just the pure
 * parsing and validation functions. {@code sit.offset} has no validation function of its own to
 * test here - see {@link SitSettings}'s Javadoc for why - only {@code sit.allowedBlocks}, read and
 * validated by {@link SitModule#enable}.
 *
 * <p>{@link MicrotusExtension} is only needed because {@link SitSettings#parseBlock} resolves a
 * key against Minestom's block registry data (see
 * {@link net.onelitefeather.titan.app.feature.navigator.NavigatorEntryValidationTest}'s Javadoc for
 * the same pattern with {@code Material}).
 */
@ExtendWith(MicrotusExtension.class)
class SitSettingsTest {

    @DisplayName("A valid allowedBlocks list is returned unchanged")
    @Test
    void aValidAllowedBlocksListIsReturnedUnchanged() {
        List<Key> allowedBlocks = List.of(Key.key("minecraft:spruce_stairs"));
        Assertions.assertEquals(allowedBlocks, SitSettings.allowedBlocks(allowedBlocks));
    }

    @DisplayName("A null allowedBlocks list is rejected, naming the full key")
    @Test
    void nullAllowedBlocksIsRejected() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> SitSettings.allowedBlocks(null));
        Assertions.assertTrue(thrown.getMessage().contains(SitSettings.ALLOWED_BLOCKS_KEY), "the message must name " + SitSettings.ALLOWED_BLOCKS_KEY);
    }

    @DisplayName("An empty allowedBlocks list is rejected, naming the full key")
    @Test
    void emptyAllowedBlocksIsRejected() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> SitSettings.allowedBlocks(List.of()));
        Assertions.assertTrue(thrown.getMessage().contains(SitSettings.ALLOWED_BLOCKS_KEY), "the message must name " + SitSettings.ALLOWED_BLOCKS_KEY);
    }

    @DisplayName("A valid block key string parses to the same Key")
    @Test
    void aValidBlockKeyStringParses() {
        Assertions.assertEquals(Key.key("minecraft:spruce_stairs"), SitSettings.parseBlock("minecraft:spruce_stairs"));
    }

    @DisplayName("An invalid block key string is rejected, naming the full key")
    @Test
    void invalidBlockKeyStringIsRejected() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> SitSettings.parseBlock("Not A Valid Key!!"));
        Assertions.assertTrue(thrown.getMessage().contains(SitSettings.ALLOWED_BLOCKS_KEY), "the message must name " + SitSettings.ALLOWED_BLOCKS_KEY);
    }

    @DisplayName("A syntactically valid key that names no known block is rejected, naming the full key")
    @Test
    void unknownBlockKeyIsRejected() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> SitSettings.parseBlock("minecraft:not_a_block"));
        Assertions.assertTrue(thrown.getMessage().contains(SitSettings.ALLOWED_BLOCKS_KEY), "the message must name " + SitSettings.ALLOWED_BLOCKS_KEY);
        Assertions.assertTrue(thrown.getMessage().contains("not_a_block"), "the message must name the offending value, was: " + thrown.getMessage());
    }

    private static RuntimeConfigFallback freshFallback() {
        return new RuntimeConfigFallback(Configuration.builder().build());
    }

    @DisplayName("resolveOffsetComponent passes a valid value through unchanged, without warning")
    @Test
    void resolveOffsetComponentPassesAValidValueThrough() {
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            double result = SitSettings.resolveOffsetComponent(SitSettings.OFFSET_Y_KEY, "0.25", 0.5, freshFallback());

            Assertions.assertEquals(0.25, result, "a valid value must pass through unchanged");
            Assertions.assertTrue(appender.list.isEmpty(), "a valid value must never warn");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("resolveOffsetComponent falls back to the shipped default and warns once for a non-numeric value")
    @Test
    void resolveOffsetComponentFallsBackAndWarnsForANonNumericValue() {
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            double result = SitSettings.resolveOffsetComponent(SitSettings.OFFSET_Y_KEY, "abc", 0.5, freshFallback());

            Assertions.assertEquals(0.5, result, "an invalid value must fall back to the shipped default");
            Assertions.assertEquals(1, appender.list.size(), "exactly one WARN must be logged for a new invalid value");
            ILoggingEvent event = appender.list.get(0);
            Assertions.assertEquals(ch.qos.logback.classic.Level.WARN, event.getLevel());
            Assertions.assertEquals(SitSettings.OFFSET_Y_KEY, event.getArgumentArray()[0], "the first argument must be the full key");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("resolveAllowedBlocks passes a valid list through unchanged")
    @Test
    void resolveAllowedBlocksPassesAValidListThrough() {
        List<Key> shippedDefault = List.of(Key.key("minecraft:spruce_stairs"));

        List<Key> result = SitSettings.resolveAllowedBlocks(List.of("minecraft:oak_stairs"), shippedDefault, freshFallback());

        Assertions.assertEquals(List.of(Key.key("minecraft:oak_stairs")), result);
    }

    @DisplayName("resolveAllowedBlocks falls back to the shipped default list and warns once for an unknown block")
    @Test
    void resolveAllowedBlocksFallsBackAndWarnsForAnUnknownBlock() {
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeConfigFallback.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        List<Key> shippedDefault = List.of(Key.key("minecraft:spruce_stairs"));

        try {
            List<Key> result = SitSettings.resolveAllowedBlocks(List.of("minecraft:not_a_block"), shippedDefault, freshFallback());

            Assertions.assertEquals(shippedDefault, result, "an unknown block must fall the whole list back to the shipped default");
            Assertions.assertEquals(1, appender.list.size(), "exactly one WARN must be logged for a new invalid value");
            ILoggingEvent event = appender.list.get(0);
            Assertions.assertEquals(SitSettings.ALLOWED_BLOCKS_KEY, event.getArgumentArray()[0], "the first argument must be the full key");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("resolveAllowedBlocks falls back to the shipped default list for an empty list")
    @Test
    void resolveAllowedBlocksFallsBackForAnEmptyList() {
        List<Key> shippedDefault = List.of(Key.key("minecraft:spruce_stairs"));

        List<Key> result = SitSettings.resolveAllowedBlocks(List.of(), shippedDefault, freshFallback());

        Assertions.assertEquals(shippedDefault, result, "an empty list must fall back to the shipped default too");
    }
}
