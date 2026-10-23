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
package net.onelitefeather.titan.feature.npc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.ambience.AmbienceZone;
import net.onelitefeather.titan.core.npc.LobbyNpc;
import net.onelitefeather.titan.core.portal.Box;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** A bad entry is skipped with a warning naming it; the good ones still start. */
class MapDataSkipTest {

    private ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender;
    private Logger npcLog;
    private Logger ambienceLog;

    @BeforeEach
    void attachAppender() {
        this.appender = new ListAppender<>();
        this.appender.start();
        this.npcLog = (Logger) LoggerFactory.getLogger(NpcModule.class);
        this.ambienceLog = (Logger) LoggerFactory.getLogger(AmbienceModule.class);
        this.npcLog.addAppender(this.appender);
        this.ambienceLog.addAppender(this.appender);
        this.npcLog.setLevel(Level.DEBUG);
        this.ambienceLog.setLevel(Level.DEBUG);
    }

    @AfterEach
    void detachAppender() {
        this.npcLog.detachAppender(this.appender);
        this.ambienceLog.detachAppender(this.appender);
    }

    private List<String> warnings() {
        return this.appender.list.stream().filter(event -> event.getLevel() == Level.WARN).map(event -> event.getFormattedMessage()).toList();
    }

    @DisplayName("An NPC without a task is skipped with a warning and the others start")
    @Test
    void invalidNpcIsSkippedWithWarning() {
        LobbyNpc valid = new LobbyNpc("slender", "Slender", new Pos(76.5, 80, 157.5), null, null, false, null);
        LobbyNpc broken = new LobbyNpc("guide", null, new Pos(0, 66, 0), null, null, false, null);
        List<LobbyNpc> usable = NpcModule.usable(List.of(broken, valid));
        Assertions.assertEquals(List.of(valid), usable, "only the complete NPC starts");
        Assertions.assertTrue(warnings().stream().anyMatch(line -> line.contains("guide") && line.contains("task")), "the warning names the NPC and its missing field: " + warnings());
    }

    @DisplayName("An ambience zone with an inverted box is skipped with a warning")
    @Test
    void invalidZoneIsSkippedWithWarning() {
        AmbienceZone inverted = new AmbienceZone("tunnel", new Box(new Vec(123, 78, 152), new Vec(82, 90, 163)), null, null, List.of(), List.of(), List.of(), null);
        Assertions.assertTrue(AmbienceModule.usable(List.of(inverted)).isEmpty(), "a zone with an inverted box is not started");
        Assertions.assertTrue(warnings().stream().anyMatch(line -> line.contains("tunnel")), "the warning names the zone: " + warnings());
    }

    @DisplayName("A sign with an unknown text key is dropped alone, the zone still starts")
    @Test
    void unknownSignKeyDropsOnlyTheSign() {
        AmbienceZone zone = new AmbienceZone("tunnel", new Box(new Vec(82, 78, 152), new Vec(123, 90, 163)), null, null, List.of(new AmbienceZone.Sign(new Vec(98, 82, 152), "titan.ambience.sign.unknown"), new AmbienceZone.Sign(new Vec(114, 82, 152), "titan.ambience.sign.pages")), List.of(), List.of(), null);
        List<AmbienceZone> usable = AmbienceModule.usable(List.of(zone));
        Assertions.assertEquals(1, usable.size(), "the zone starts");
        Assertions.assertEquals(1, usable.getFirst().signs().size(), "only the sign with a known key stays");
        Assertions.assertTrue(warnings().stream().anyMatch(line -> line.contains("titan.ambience.sign.unknown")), "the warning names the key: " + warnings());
    }
}
