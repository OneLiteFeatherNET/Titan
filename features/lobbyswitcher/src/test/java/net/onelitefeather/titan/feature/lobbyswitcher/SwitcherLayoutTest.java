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
package net.onelitefeather.titan.feature.lobbyswitcher;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class SwitcherLayoutTest {

    private final SwitcherLayout layout = new SwitcherLayout();
    private final Logger layoutLogger = (Logger) LoggerFactory.getLogger(SwitcherLayout.class);
    private final ListAppender<ILoggingEvent> lines = new ListAppender<>();

    @BeforeEach
    void captureLog() {
        this.lines.start();
        this.layoutLogger.addAppender(this.lines);
    }

    @AfterEach
    void releaseLog() {
        this.layoutLogger.detachAppender(this.lines);
        this.lines.stop();
    }

    private static List<SwitcherEntry> entries(int count) {
        // Zero-padded so that name order equals creation order.
        return IntStream.range(0, count).mapToObj(i -> new SwitcherEntry("Lobby-%03d".formatted(i), 1, 50, SwitcherState.JOINABLE)).toList();
    }

    @DisplayName("One to nine entries take one row")
    @Test
    void upToNineIsOneRow() {
        Assertions.assertEquals(1, SwitcherLayout.rows(1), "one entry");
        Assertions.assertEquals(1, SwitcherLayout.rows(9), "nine entries");
    }

    @DisplayName("No entries still take one row")
    @Test
    void emptyIsOneRow() {
        Assertions.assertEquals(1, SwitcherLayout.rows(0));
    }

    @DisplayName("Ten entries take two rows")
    @Test
    void tenIsTwoRows() {
        Assertions.assertEquals(2, SwitcherLayout.rows(10));
    }

    @DisplayName("The row count grows by one per nine entries")
    @Test
    void growsByNine() {
        Assertions.assertEquals(2, SwitcherLayout.rows(18), "eighteen entries");
        Assertions.assertEquals(3, SwitcherLayout.rows(19), "nineteen entries");
    }

    @DisplayName("54 entries take the maximum of six rows")
    @Test
    void fiftyFourIsSixRows() {
        Assertions.assertEquals(6, SwitcherLayout.rows(54));
    }

    @DisplayName("More than 54 entries still take six rows")
    @Test
    void moreThanFiftyFourStaysSixRows() {
        Assertions.assertEquals(6, SwitcherLayout.rows(100));
    }

    @DisplayName("Up to 54 entries are kept and nothing is logged")
    @Test
    void fittingEntriesAreKeptSilently() {
        List<SwitcherEntry> fitted = this.layout.fit(entries(54));

        Assertions.assertEquals(54, fitted.size());
        Assertions.assertTrue(this.lines.list.isEmpty(), "nothing was cut, nothing is logged");
    }

    @DisplayName("More than 54 entries are cut to the first 54 by name")
    @Test
    void excessEntriesAreCutByName() {
        List<SwitcherEntry> shuffled = entries(60).reversed();

        List<SwitcherEntry> fitted = this.layout.fit(shuffled);

        Assertions.assertEquals(54, fitted.size(), "capped at six rows");
        Assertions.assertEquals("Lobby-000", fitted.getFirst().name(), "sorted ascending");
        Assertions.assertEquals("Lobby-053", fitted.getLast().name(), "the last six names are dropped");
    }

    @DisplayName("Cutting entries is reported once per call with the numbers")
    @Test
    void cuttingIsLogged() {
        this.layout.fit(entries(60));

        Assertions.assertEquals(1, this.lines.list.size(), "one line");
        ILoggingEvent line = this.lines.list.getFirst();
        Assertions.assertEquals(Level.WARN, line.getLevel());
        Assertions.assertTrue(line.getFormattedMessage().contains("60"), "names the number of lobbies: " + line.getFormattedMessage());
        Assertions.assertTrue(line.getFormattedMessage().contains("54"), "names the cap: " + line.getFormattedMessage());
    }

    @DisplayName("Cutting entries is reported once, not on every refresh")
    @Test
    void cuttingIsLoggedOnlyOnce() {
        this.layout.fit(entries(60));
        this.layout.fit(entries(60));
        this.layout.fit(entries(61));

        Assertions.assertEquals(1, this.lines.list.size(), "repeated cuts log a single line");
    }

    @DisplayName("Cutting warns again after the list fitted in between")
    @Test
    void cuttingWarnsAgainAfterFitting() {
        this.layout.fit(entries(60));
        this.layout.fit(entries(10));

        this.layout.fit(entries(60));

        Assertions.assertEquals(2, this.lines.list.size(), "a new overflow is news again");
    }
}
