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
package net.onelitefeather.titan.common.deliver;

import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.common.config.testing.CapturingLoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Unit coverage for {@link DebugDeliver}: the {@link net.onelitefeather.titan.api.deliver.Deliver}
 * standalone (non-CloudNet) runs get from {@link DeliverProvider} instead of the removed
 * no-op. Checks the player-facing chat message it sends and the single {@code INFO} log line it
 * writes for the operator, for both a {@link DeliverComponent.TaskComponent} and a {@link
 * DeliverComponent.ServerDeliverComponent}, and that {@link DebugDeliver#sendPlayer} stays
 * null-safe like {@link MessageChannelDeliver#sendPlayer}.
 *
 * <p>Log lines are asserted through {@link CapturingLoggerFactory}, the SLF4J test binding already
 * registered for the {@code common} module's test sources (see {@code
 * META-INF/services/org.slf4j.spi.SLF4JServiceProvider} in {@code common/src/test/resources}) -
 * adding logback-classic here instead would register a second, competing {@code
 * SLF4JServiceProvider} on the same test classpath and make this test's logging assertions depend
 * on undefined {@code ServiceLoader} ordering.
 */
@ExtendWith(MicrotusExtension.class)
class DebugDeliverTest {

    private final DebugDeliver deliver = new DebugDeliver();

    @BeforeEach
    void clearLog() {
        CapturingLoggerFactory.clear();
    }

    @DisplayName("Delivering a task component tells the player the task by chat and logs one INFO line naming it")
    @Test
    void sendPlayerWithTaskComponentReportsTheTask(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);
        DeliverComponent component = DeliverComponent.taskBuilder().taskName("cygnus").player(player).build();

        this.deliver.sendPlayer(player, component);

        messages.assertSingle(packet -> {
            String text = PlainTextComponentSerializer.plainText().serialize(packet.message());
            Assertions.assertTrue(text.contains("task"), "the message must name the delivery as a task: " + text);
            Assertions.assertTrue(text.contains("cygnus"), "the message must name the target task: " + text);
        });
        String expectedLine = "INFO net.onelitefeather.titan.common.deliver.DebugDeliver - Debug deliver: would send " + player.getUsername() + " (" + player.getUuid() + ") to task cygnus";
        Assertions.assertEquals(List.of(expectedLine), CapturingLoggerFactory.messages(), "exactly one INFO line must be logged for the click");
    }

    @DisplayName("Delivering a server component tells the player the server by chat and logs one INFO line naming it")
    @Test
    void sendPlayerWithServerComponentReportsTheServer(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);
        DeliverComponent component = DeliverComponent.serverBuilder().serverName("survival-1").player(player).build();

        this.deliver.sendPlayer(player, component);

        messages.assertSingle(packet -> {
            String text = PlainTextComponentSerializer.plainText().serialize(packet.message());
            Assertions.assertTrue(text.contains("server"), "the message must name the delivery as a server: " + text);
            Assertions.assertTrue(text.contains("survival-1"), "the message must name the target server: " + text);
        });
        String expectedLine = "INFO net.onelitefeather.titan.common.deliver.DebugDeliver - Debug deliver: would send " + player.getUsername() + " (" + player.getUuid() + ") to server survival-1";
        Assertions.assertEquals(List.of(expectedLine), CapturingLoggerFactory.messages(), "exactly one INFO line must be logged for the click");
    }

    @DisplayName("A target containing MiniMessage-like text is shown literally, never interpreted as MiniMessage")
    @Test
    void sendPlayerWithMiniMessageLikeTargetShowsItLiterally(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);
        DeliverComponent component = DeliverComponent.taskBuilder().taskName("<red>evil").player(player).build();

        this.deliver.sendPlayer(player, component);

        messages.assertSingle(packet -> {
            String text = PlainTextComponentSerializer.plainText().serialize(packet.message());
            Assertions.assertTrue(text.contains("<red>evil"), "a config-supplied target must appear literally, not be interpreted as MiniMessage: " + text);
        });
    }

    @DisplayName("A null player is ignored without logging")
    @Test
    void sendPlayerWithNullPlayerDoesNothing() {
        DeliverComponent component = DeliverComponent.taskBuilder().taskName("cygnus").playerId(UUID.randomUUID()).build();

        Assertions.assertDoesNotThrow(() -> this.deliver.sendPlayer(null, component), "a null player must not blow up the click handler");

        Assertions.assertTrue(CapturingLoggerFactory.messages().isEmpty(), "a null player must not be logged");
    }

    @DisplayName("A null component is ignored without sending a message or logging")
    @Test
    void sendPlayerWithNullComponentDoesNothing(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        Assertions.assertDoesNotThrow(() -> this.deliver.sendPlayer(player, null), "a null component must not blow up the click handler");

        messages.assertEmpty();
        Assertions.assertTrue(CapturingLoggerFactory.messages().isEmpty(), "a null component must not be logged");
    }
}
