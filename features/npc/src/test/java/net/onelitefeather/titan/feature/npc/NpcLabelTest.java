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

import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.onelitefeather.titan.core.npc.LobbyNpc;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.PortalLabel;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NpcLabelTest {

    private static LobbyNpc npc(LabelSource source) {
        PortalLabel label = new PortalLabel(new Vec(76.5, 82, 157.5), "<gold>Slender<newline><gray><online>/<max> Online", "<red>Slender offline", source, Billboard.CENTER, 0f);
        return new LobbyNpc("slender", "Slender", new Pos(76.5, 80, 157.5), null, label, false, null);
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @DisplayName("The label shows the online count and the maximum of the task")
    @Test
    void showsOnlineAndMax() {
        FakePlayerCounts counts = new FakePlayerCounts().task("Slender", new ServiceCount("Slender-1", 7, 20), new ServiceCount("Slender-2", 3, 20));
        Assertions.assertEquals("Slender\n10/40 Online", plain(NpcLabel.text(npc(null), Optional.of(counts), 0)), "the sums of the services");
    }

    @DisplayName("A task that runs nowhere shows the offline text")
    @Test
    void offlineWhenNothingRuns() {
        Assertions.assertEquals("Slender offline", plain(NpcLabel.text(npc(null), Optional.of(new FakePlayerCounts()), 0)), "no service means the offline text");
    }

    @DisplayName("A source that cannot be read shows the offline text")
    @Test
    void offlineWhenUnreadable() {
        Assertions.assertEquals("Slender offline", plain(NpcLabel.text(npc(null), Optional.empty(), 0)), "without a provider the label cannot count");
    }

    @DisplayName("A provider that throws shows the offline text instead of failing")
    @Test
    void offlineWhenProviderThrows() {
        FakePlayerCounts counts = new FakePlayerCounts().failingWith(new IllegalStateException("down"));
        Assertions.assertEquals("Slender offline", plain(NpcLabel.text(npc(null), Optional.of(counts), 0)), "one failing provider must not break the label");
    }

    @DisplayName("A local source counts the players of this lobby")
    @Test
    void localCountsLobbyPlayers() {
        Assertions.assertEquals("Slender\n5/? Online", plain(NpcLabel.text(npc(new LabelSource.Local()), Optional.empty(), 5)), "the lobby count, without a maximum");
    }
}
