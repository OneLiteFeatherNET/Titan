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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.DisplayScoreboardPacket;
import net.minestom.server.network.packet.server.play.ScoreboardObjectivePacket;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.network.packet.server.play.UpdateScorePacket;
import net.minestom.server.scoreboard.Sidebar;

/** Reads the sidebar out of what a client was sent: the objective, and the text of each line. */
final class SidebarPackets {

    private static final byte CREATE = 0;
    private static final byte REMOVE = 1;

    /** What the plain serializer writes for a player head. */
    static final String HEAD = "[unknown player head]";

    private SidebarPackets() {
    }

    static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    static List<ScoreboardObjectivePacket> objectives(List<ServerPacket> packets) {
        return packets.stream().filter(ScoreboardObjectivePacket.class::isInstance).map(ScoreboardObjectivePacket.class::cast).toList();
    }

    static List<ScoreboardObjectivePacket> created(List<ServerPacket> packets) {
        return objectives(packets).stream().filter(packet -> packet.mode() == CREATE).toList();
    }

    static List<ScoreboardObjectivePacket> removed(List<ServerPacket> packets) {
        return objectives(packets).stream().filter(packet -> packet.mode() == REMOVE).toList();
    }

    static List<DisplayScoreboardPacket> displays(List<ServerPacket> packets) {
        return packets.stream().filter(DisplayScoreboardPacket.class::isInstance).map(DisplayScoreboardPacket.class::cast).toList();
    }

    static List<UpdateScorePacket> scores(List<ServerPacket> packets) {
        return packets.stream().filter(UpdateScorePacket.class::isInstance).map(UpdateScorePacket.class::cast).toList();
    }

    /** Every sidebar packet at all, of any kind. */
    static List<ServerPacket> sidebarPackets(List<ServerPacket> packets) {
        return packets.stream().filter(packet -> packet instanceof ScoreboardObjectivePacket || packet instanceof DisplayScoreboardPacket || packet instanceof UpdateScorePacket || packet instanceof TeamsPacket).toList();
    }

    /** The prefix of every line team that a packet creates or updates, in packet order. */
    static List<Component> written(List<ServerPacket> packets) {
        return packets.stream().filter(TeamsPacket.class::isInstance).map(TeamsPacket.class::cast).map(SidebarPackets::prefixOf).filter(prefix -> prefix != null).toList();
    }

    /**
     * The lines as the client shows them after all packets, top first: the teams in creation order.
     */
    static List<Component> shown(List<ServerPacket> packets) {
        Map<String, Component> lines = new LinkedHashMap<>();
        for (ServerPacket packet : packets) {
            if (!(packet instanceof TeamsPacket teams)) {
                continue;
            }
            switch (teams.action()) {
                case TeamsPacket.CreateTeamAction create ->
                    lines.put(teams.teamName(), create.settings().teamPrefix());
                case TeamsPacket.UpdateTeamAction update ->
                    lines.put(teams.teamName(), update.settings().teamPrefix());
                case TeamsPacket.RemoveTeamAction _ -> lines.remove(teams.teamName());
                default -> {
                }
            }
        }
        return List.copyOf(lines.values());
    }

    // A Minestom upgrade that touches scoreboard packets is fixed here only; the tests read rows.
    /**
     * The lines as the client shows them, top first, as "text|value"; the value is left out for a
     * line without a number. The value is the fixed number format of the line's latest score entry.
     */
    static List<String> shownRows(List<ServerPacket> packets) {
        Map<String, String> entityOfTeam = new LinkedHashMap<>();
        Map<String, Component> textOfTeam = new LinkedHashMap<>();
        Map<String, Component> valueOfEntity = new LinkedHashMap<>();
        for (ServerPacket packet : packets) {
            switch (packet) {
                case TeamsPacket teams -> {
                    switch (teams.action()) {
                        case TeamsPacket.CreateTeamAction create -> {
                            entityOfTeam.put(teams.teamName(), create.entities().getFirst());
                            textOfTeam.put(teams.teamName(), create.settings().teamPrefix());
                        }
                        case TeamsPacket.UpdateTeamAction update ->
                            textOfTeam.put(teams.teamName(), update.settings().teamPrefix());
                        case TeamsPacket.RemoveTeamAction _ -> {
                            valueOfEntity.remove(entityOfTeam.remove(teams.teamName()));
                            textOfTeam.remove(teams.teamName());
                        }
                        default -> {
                        }
                    }
                }
                case UpdateScorePacket score -> {
                    if (!Sidebar.NumberFormat.blank().equals(score.numberFormat())) {
                        valueOfEntity.put(score.entityName(), score.numberFormat().content());
                    } else {
                        valueOfEntity.remove(score.entityName());
                    }
                }
                default -> {
                }
            }
        }
        return textOfTeam.entrySet().stream().map(row -> plain(row.getValue()) + Optional.ofNullable(valueOfEntity.get(entityOfTeam.get(row.getKey()))).map(value -> "|" + plain(value)).orElse("")).toList();
    }

    static List<String> shownText(List<ServerPacket> packets) {
        return shown(packets).stream().map(SidebarPackets::plain).toList();
    }

    private static Component prefixOf(TeamsPacket teams) {
        return switch (teams.action()) {
            case TeamsPacket.CreateTeamAction create -> create.settings().teamPrefix();
            case TeamsPacket.UpdateTeamAction update -> update.settings().teamPrefix();
            default -> null;
        };
    }
}
