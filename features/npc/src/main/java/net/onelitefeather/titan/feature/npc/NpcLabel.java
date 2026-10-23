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
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta.BillboardConstraints;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.onelitefeather.titan.core.npc.LobbyNpc;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.LabelText;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.PortalLabel;
import net.onelitefeather.titan.core.portal.SourceType;

/**
 * The label above a join NPC: the same text format as a portal label, with the player count of the
 * NPC's source. A source that cannot be read shows its offline text.
 */
final class NpcLabel {

    private static final String UNKNOWN_MAX = "?";

    private NpcLabel() {
    }

    /** The text of the label now; {@code lobbyPlayers} counts the players of this lobby. */
    static Component text(LobbyNpc npc, Optional<PlayerCounts> counts, int lobbyPlayers) {
        PortalLabel label = npc.label();
        LabelSource source = label.source() != null ? label.source() : new LabelSource.Task(npc.task());
        return switch (source) {
            case LabelSource.Local ignored ->
                LabelText.render(label, npc.task(), String.valueOf(lobbyPlayers), UNKNOWN_MAX, false);
            case LabelSource.Task task -> remote(npc, counts, SourceType.TASK, task.name());
            case LabelSource.Group group -> remote(npc, counts, SourceType.GROUP, group.name());
            case LabelSource.Service service ->
                remote(npc, counts, SourceType.SERVICE, service.name());
            case LabelSource.Unknown ignored -> offline(npc);
        };
    }

    /** A text display at the label's position, shown above the figure. */
    static Entity display(Instance lobby, LobbyNpc npc) {
        PortalLabel label = npc.label();
        Entity entity = new Entity(EntityType.TEXT_DISPLAY);
        boolean fixed = label.billboard() == Billboard.FIXED;
        entity.editEntityMeta(TextDisplayMeta.class, meta -> meta.setBillboardRenderConstraints(fixed ? BillboardConstraints.FIXED : BillboardConstraints.CENTER));
        entity.setNoGravity(true);
        entity.setInstance(lobby, new Pos(label.position().x(), label.position().y(), label.position().z(), fixed ? label.yaw() : 0f, 0f)).join();
        return entity;
    }

    static void show(Entity display, Component text) {
        display.editEntityMeta(TextDisplayMeta.class, meta -> meta.setText(text));
    }

    private static Component remote(LobbyNpc npc, Optional<PlayerCounts> counts, SourceType type, String name) {
        if (counts.isEmpty() || name == null || !counts.get().supports(type)) {
            return offline(npc);
        }
        try {
            PlayerCount count = counts.get().count(type, name);
            if (!count.running()) {
                return offline(npc);
            }
            return LabelText.render(npc.label(), npc.task(), String.valueOf(count.online()), String.valueOf(count.max()), false);
        } catch (RuntimeException e) {
            return offline(npc);
        }
    }

    private static Component offline(LobbyNpc npc) {
        return LabelText.render(npc.label(), npc.task(), "0", "0", true);
    }
}
