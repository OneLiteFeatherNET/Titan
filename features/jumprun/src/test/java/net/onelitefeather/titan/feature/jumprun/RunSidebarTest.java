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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.network.packet.server.play.UpdateScorePacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Which packets {@link RunSidebar} sends when the input changes: only what changed, nothing else.
 */
@ExtendWith(MicrotusExtension.class)
class RunSidebarTest {

    private static final UUID ALEX = new UUID(0, 1);
    private static final UUID STEVE = new UUID(0, 2);
    private static final UUID NOTCH = new UUID(0, 3);
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final String TEXT = "TextUpdate";
    private static final String VALUE = "ValueUpdate";
    private static final String CREATE = "LineCreate";
    private static final String REMOVE = "LineRemove";

    private RunMessages messages;
    private TestConnection connection;
    private RunSidebar sidebar;

    @BeforeEach
    void setUp(Env env) {
        messages = new RunMessages();
        messages.register();
        connection = env.createConnection(new GameProfile(ALEX, "Alex"));
        Player player = connection.connect(JumprunFixture.loadedInstance(env), new Pos(0.5, JumprunFixture.GROUND_Y, 0.5));
        sidebar = new RunSidebar(player, Mode.HARD, new RunSidebarContent(messages));
    }

    @AfterEach
    void tearDown() {
        messages.close();
    }

    private static Optional<TopThree> top(TopEntry... entries) {
        TopThree top = TopThree.EMPTY;
        for (TopEntry entry : entries) {
            top = top.with(entry);
        }
        return Optional.of(top);
    }

    private static TopEntry entry(UUID player, String name, int score) {
        return new TopEntry(player, name, score, T0);
    }

    private static final Optional<TopThree> FULL = top(entry(ALEX, "Alex", 88), entry(STEVE, "Steve", 61), entry(NOTCH, "Notch", 42));

    /** Shows the input and returns the kind of change of every sidebar packet it caused. */
    private List<String> show(int score, OptionalInt best, Optional<TopThree> top) {
        Collector<ServerPacket> sent = connection.trackIncoming();
        sidebar.show(score, best, top);
        return sent.collect().stream().map(RunSidebarTest::kind).filter(kind -> kind != null).toList();
    }

    private static String kind(ServerPacket packet) {
        return switch (packet) {
            case TeamsPacket teams -> switch (teams.action()) {
                case TeamsPacket.CreateTeamAction _ -> CREATE;
                case TeamsPacket.UpdateTeamAction _ -> TEXT;
                case TeamsPacket.RemoveTeamAction _ -> REMOVE;
                default -> null;
            };
            case UpdateScorePacket _ -> VALUE;
            default -> null;
        };
    }

    @Test
    void anUnchangedInputSendsNothing() {
        show(3, OptionalInt.of(5), FULL);

        assertEquals(List.of(), show(3, OptionalInt.of(5), FULL), "same lines, no packet");
    }

    @Test
    void aChangedValueUpdatesOnlyTheNumber() {
        show(3, OptionalInt.of(5), FULL);

        assertEquals(List.of(VALUE), show(4, OptionalInt.of(5), FULL), "the score value changed, its label did not");
    }

    @Test
    void aChangedTextWithTheSameValueUpdatesOnlyTheContent() {
        show(3, OptionalInt.of(5), top(entry(NOTCH, "Notch", 88)));

        assertEquals(List.of(TEXT), show(3, OptionalInt.of(5), top(entry(STEVE, "Steve", 88))), "another name at the same rank and score");
    }

    @Test
    void shrinkingToTwoLinesRemovesTheTailAndGrowingCreatesItAgain() {
        show(3, OptionalInt.of(5), FULL);

        List<String> shrunk = show(3, OptionalInt.of(5), Optional.empty());
        List<String> grown = show(3, OptionalInt.of(5), FULL);

        assertEquals(RunSidebarContent.MAX_LINES - 2, shrunk.stream().filter(REMOVE::equals).count(), "gap, header and the places go");
        assertEquals(List.of(), shrunk.stream().filter(kind -> !REMOVE.equals(kind)).toList(), "nothing else is sent for the two lines that stay");
        assertEquals(RunSidebarContent.MAX_LINES - 2, grown.stream().filter(CREATE::equals).count(), "they come back as new lines");
        assertEquals(List.of(), grown.stream().filter(TEXT::equals).toList(), "no update of a line that did not exist");
    }
}
