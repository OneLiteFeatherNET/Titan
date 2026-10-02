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
package net.onelitefeather.titan.feature.jumprun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.avaje.config.Configuration;
import java.util.List;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** Start and live reads of a setting, shown on the palettes, on a configuration each test owns. */
class LiveSettingTest {

    private static final String LIME_KEY = JumprunSettings.key(Surface.FULL, "lime_wool");

    private final Logger readerLogger = (Logger) LoggerFactory.getLogger(LiveSetting.class);
    private final ListAppender<ILoggingEvent> lines = new ListAppender<>();

    private Configuration config;
    private LiveSetting<Palettes> reader;

    @BeforeEach
    void setUp() {
        this.config = TestBlocks.shippedConfiguration();
        this.reader = new LiveSetting<>("palettes", () -> JumprunSettings.palettes(this.config));
        this.lines.start();
        this.readerLogger.addAppender(this.lines);
    }

    @AfterEach
    void tearDown() {
        this.readerLogger.detachAppender(this.lines);
        this.lines.stop();
    }

    private List<ILoggingEvent> warnings() {
        return this.lines.list.stream().filter(line -> line.getLevel() == Level.WARN).toList();
    }

    @Test
    void startReadsTheShippedDefaults() {
        assertEquals(33, this.reader.readAtStartup().of(Surface.FULL).blocks().size(), "the full block palette of the defaults");
    }

    @Test
    void anInvalidValueAtStartAbortsWithItsKey() {
        this.config.setProperty(LIME_KEY, "-1");

        IllegalArgumentException abort = assertThrows(IllegalArgumentException.class, this.reader::readAtStartup);

        assertTrue(abort.getMessage().startsWith(LIME_KEY), "the abort names the key: " + abort.getMessage());
    }

    @Test
    void aRunReadsTheValueAsItIsNow() {
        this.reader.readAtStartup();
        this.config.forPath(JumprunSettings.key(Surface.FULL)).keys().forEach(block -> this.config.clearProperty(JumprunSettings.key(Surface.FULL, block)));
        this.config.setProperty(LIME_KEY, "1");

        assertEquals(List.of(Block.LIME_WOOL), this.reader.current().of(Surface.FULL).blocks(), "the edited palette applies without a restart");
        assertEquals(List.of(), warnings(), "a valid edit is not a warning");
    }

    @Test
    void anInvalidLiveValueKeepsTheLastValidPalettesAndWarnsWithTheKey() {
        Palettes valid = this.reader.readAtStartup();
        this.config.setProperty(LIME_KEY, "-1");

        assertSame(valid, this.reader.current(), "the last valid palettes stay");
        assertEquals(1, warnings().size(), "one warning");
        assertTrue(warnings().getFirst().getFormattedMessage().contains(LIME_KEY), "the warning names the key: " + warnings().getFirst().getFormattedMessage());
    }

    @Test
    void theSameInvalidValueWarnsOnlyOnce() {
        this.reader.readAtStartup();
        this.config.setProperty(LIME_KEY, "-1");

        this.reader.current();
        this.reader.current();

        assertEquals(1, warnings().size(), "not once per run");
    }

    @Test
    void aFixedValueIsUsedAgainAndWarnsAgainWhenBrokenAgain() {
        this.reader.readAtStartup();
        this.config.setProperty(LIME_KEY, "-1");
        this.reader.current();
        this.config.setProperty(LIME_KEY, "4");
        this.reader.current();

        this.config.setProperty(LIME_KEY, "-1");
        this.reader.current();

        assertEquals(2, warnings().size(), "the fix reset the warning");
    }

    @Test
    void withoutAValidReadAnInvalidValueStillFails() {
        this.config.setProperty(LIME_KEY, "-1");

        assertThrows(IllegalArgumentException.class, this.reader::current, "there is nothing to fall back to");
    }
}
