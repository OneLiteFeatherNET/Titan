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
package net.onelitefeather.titan.core.npc;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.titan.core.portal.PortalLabel;
import org.jetbrains.annotations.Nullable;

/**
 * A join NPC: a figure in the lobby that sends a player into a CloudNet task on a click. Plain data
 * read from the map file; {@link NpcValidator} decides whether it is usable.
 *
 * @param id            unique within its world
 * @param task          CloudNet task the player is sent to
 * @param position      where the figure stands, with its facing
 * @param skin          the figure's skin, or {@code null} for the default one
 * @param label         text shown above the figure, or {@code null} for none
 * @param lookAtPlayers whether the head follows the nearest player
 * @param permission    permission a player needs to join, or {@code null} for everyone
 */
public record LobbyNpc(String id, String task, Pos position, @Nullable NpcSkin skin,
                       @Nullable PortalLabel label, boolean lookAtPlayers,
                       @Nullable String permission) {
}
