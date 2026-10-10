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
package net.onelitefeather.titan.setup.portal.editor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.setup.portal.InMemoryPortalSources;
import net.onelitefeather.titan.setup.portal.InMemoryPortalStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalCopierLoggingTest {

    private static final UUID PLAYER = new UUID(0xCAFEL, 0xBABEL);
    private static final Portal SURVIVAL = new Portal("survival", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "Survival", null);
    private static final Portal CREATIVE = new Portal("creative", new Box(new Vec(2, 2, 2), new Vec(3, 3, 3)), "Creative", null);

    private Logger logger;
    private Level previousLevel;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(PortalCopier.class);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(previousLevel);
    }

    private List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    @DisplayName("Copying logs one INFO line with the number of portals and both worlds")
    void copyLogsOneInfoLine() {
        PortalEditor editor = new PortalEditor(new InMemoryPortalStore());
        InMemoryPortalSources sources = new InMemoryPortalSources("winter").world("lobby", SURVIVAL, CREATIVE);

        new PortalCopier(editor, sources, new InMemoryPortalStore()).copy(PLAYER, "lobby");

        assertEquals(List.of("Copied 2 portals from world lobby into drafts in world test-world"), messages(), "exactly one line for the copy");
        assertEquals(Level.INFO, appender.list.getFirst().getLevel(), "at INFO level");
    }

    @Test
    @DisplayName("An unreadable source logs one WARN line with the world")
    void unreadableSourceLogsOneWarnLine() {
        PortalEditor editor = new PortalEditor(new InMemoryPortalStore());
        InMemoryPortalSources sources = new InMemoryPortalSources("winter").unreadable("broken");

        new PortalCopier(editor, sources, new InMemoryPortalStore()).copy(PLAYER, "broken");

        assertEquals(1, appender.list.size(), "exactly one line for the failed read");
        assertEquals(Level.WARN, appender.list.getFirst().getLevel(), "a failed read is a warning");
        assertTrue(messages().getFirst().contains("broken"), "the line names the world: " + messages().getFirst());
    }

    @Test
    @DisplayName("Copy logs never carry the player")
    void noLineCarriesThePlayer() {
        PortalEditor editor = new PortalEditor(new InMemoryPortalStore());
        InMemoryPortalSources sources = new InMemoryPortalSources("winter").world("lobby", SURVIVAL).unreadable("broken");
        PortalCopier copier = new PortalCopier(editor, sources, new InMemoryPortalStore());

        copier.copy(PLAYER, "lobby");
        copier.copy(PLAYER, "broken");

        assertEquals(2, messages().size(), "both copies were logged, so the check below is not vacuous");
        assertFalse(messages().stream().anyMatch(line -> line.contains(PLAYER.toString())), "the player is never logged");
    }
}
