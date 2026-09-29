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
package net.onelitefeather.titan.setup.portal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.minestom.server.coordinate.Pos;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Portal;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PortalEditorLoggingTest {

    private static final UUID PLAYER = new UUID(0xCAFEL, 0xBABEL);

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(PortalEditor.class);
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        appender.stop();
    }

    private List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    @DisplayName("Saving logs one INFO line with the id and the world")
    void saveLogsOneInfoLine() {
        PortalEditor editor = new PortalEditor(new InMemoryPortalStore());
        editor.corner1(PLAYER, "survival", new Pos(0, 0, 0));
        editor.corner2(PLAYER, "survival", new Pos(1, 1, 1));
        editor.task(PLAYER, "survival", "Survival");

        editor.save(PLAYER, "survival");

        assertEquals(List.of("Saved portal survival in world test-world"), messages(), "exactly one line for the save");
        assertEquals(Level.INFO, appender.list.getFirst().getLevel(), "at INFO level");
    }

    @Test
    @DisplayName("Removing logs one INFO line")
    void removeLogsOneInfoLine() {
        Portal saved = new Portal("survival", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "Survival", null);
        PortalEditor editor = new PortalEditor(new InMemoryPortalStore(saved));

        editor.remove(PLAYER, "survival");

        assertEquals(List.of("Removed portal survival from world test-world"), messages(), "exactly one line for the removal");
    }

    @Test
    @DisplayName("Editing, cancelling and rejected saves log nothing, and no line carries the player")
    void nothingElseIsLogged() {
        PortalEditor editor = new PortalEditor(new InMemoryPortalStore());
        editor.corner1(PLAYER, "survival", new Pos(0, 0, 0));
        editor.save(PLAYER, "survival");
        editor.cancel(PLAYER, "survival");
        editor.remove(PLAYER, "unknown");

        assertEquals(List.of(), messages(), "only writes to the map are logged");
        assertFalse(messages().stream().anyMatch(line -> line.contains(PLAYER.toString())), "the player is never logged");
    }
}
