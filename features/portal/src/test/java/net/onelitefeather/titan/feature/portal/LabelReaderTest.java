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
package net.onelitefeather.titan.feature.portal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import net.onelitefeather.titan.core.portal.SourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LabelReaderTest {

    private static final Box BOX = new Box(new Vec(10, 64, 10), new Vec(14, 68, 11));

    private final Logger readerLogger = (Logger) LoggerFactory.getLogger(LabelReader.class);
    private final ListAppender<ILoggingEvent> lines = new ListAppender<>();

    @BeforeEach
    void captureLog() {
        this.lines.start();
        this.readerLogger.addAppender(this.lines);
    }

    @AfterEach
    void releaseLog() {
        this.readerLogger.detachAppender(this.lines);
        this.lines.stop();
    }

    private static Portal portal(String id, LabelSource source) {
        return new Portal(id, BOX, "Survival", null, new PortalLabel(new Vec(12.5, 66, -3.5), "<gray>Survival", null, source, Billboard.CENTER, 0));
    }

    private static PlayerCounts providerFor(SourceType served, PlayerCount answer) {
        return new PlayerCounts() {
            @Override
            public boolean supports(SourceType type) {
                return type == served;
            }

            @Override
            public PlayerCount count(SourceType type, String name) {
                return answer;
            }
        };
    }

    private static LabelReader reader(PlayerCounts counts) {
        return new LabelReader(counts, () -> 7);
    }

    private List<ILoggingEvent> warnings() {
        return this.lines.list.stream().filter(line -> line.getLevel() == Level.WARN).toList();
    }

    @DisplayName("A label without a source reads the count of the portal's task from the provider")
    @Test
    void missingSourceReadsPortalTask() {
        PlayerCounts counts = (type, name) -> type == SourceType.TASK && name.equals("Survival") ? new PlayerCount(3, 20, true) : PlayerCount.NOT_RUNNING;

        assertEquals(new LabelReading.Remote(new PlayerCount(3, 20, true)), reader(counts).read(portal("survival", null)), "the portal's task must be asked for when the label names no source");
    }

    @DisplayName("Group and service sources are asked from the provider with their own type")
    @Test
    void groupAndServiceUseTheirType() {
        PlayerCounts counts = (type, name) -> new PlayerCount(type.ordinal(), name.length(), true);
        LabelReader reader = reader(counts);

        assertEquals(new LabelReading.Remote(new PlayerCount(1, 5, true)), reader.read(portal("a", new LabelSource.Group("Lobby"))), "group");
        assertEquals(new LabelReading.Remote(new PlayerCount(2, 7, true)), reader.read(portal("b", new LabelSource.Service("Lobby-1"))), "service");
    }

    @DisplayName("The local source reads this lobby and never asks the provider")
    @Test
    void localReadsConnections() {
        PlayerCounts failing = (type, name) -> {
            throw new AssertionError("local must not reach the provider");
        };

        assertEquals(new LabelReading.Local(7), reader(failing).read(portal("a", new LabelSource.Local())), "local count");
    }

    @DisplayName("A provider that does not support the type yields an offline reading and one warning per portal and source")
    @Test
    void unsupportedTypeWarnsOnce() {
        LabelReader reader = reader(providerFor(SourceType.GROUP, new PlayerCount(3, 20, true)));
        Portal portal = portal("survival", new LabelSource.Task("Survival"));

        assertEquals(new LabelReading.Remote(PlayerCount.NOT_RUNNING), reader.read(portal), "first read is offline");
        reader.read(portal);
        reader.read(portal);

        assertEquals(1, warnings().size(), "the warning must be logged once, not on every refresh");
        assertEquals("Portal label source task 'Survival' of portal 'survival' is unavailable", warnings().getFirst().getFormattedMessage(), "warning text");
    }

    @DisplayName("Each portal and source warns separately")
    @Test
    void differentPortalsWarnSeparately() {
        LabelReader reader = reader(providerFor(SourceType.GROUP, new PlayerCount(3, 20, true)));

        reader.read(portal("a", new LabelSource.Task("Survival")));
        reader.read(portal("b", new LabelSource.Task("Survival")));
        reader.read(portal("a", new LabelSource.Service("Survival-1")));

        assertEquals(3, warnings().size(), "another portal or another source is a new warning");
    }

    @DisplayName("A supported source never warns")
    @Test
    void supportedSourceDoesNotWarn() {
        reader(providerFor(SourceType.TASK, PlayerCount.NOT_RUNNING)).read(portal("a", new LabelSource.Task("Survival")));

        assertEquals(0, warnings().size(), "a task that simply is not running is not a configuration problem");
    }
}
