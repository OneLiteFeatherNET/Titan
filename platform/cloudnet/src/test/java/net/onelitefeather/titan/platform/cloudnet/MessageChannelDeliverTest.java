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
package net.onelitefeather.titan.platform.cloudnet;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.api.deliver.DeliverComponent;
import net.onelitefeather.titan.common.deliver.ServerConnector;
import net.onelitefeather.titan.common.deliver.TitanServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

/**
 * Unit coverage for {@link MessageChannelDeliver} without an installed {@link ServerConnector};
 * log lines are captured with a {@link ListAppender} attached for this test only.
 */
@ExtendWith(MicrotusExtension.class)
class MessageChannelDeliverTest {

    private final MessageChannelDeliver deliver = new MessageChannelDeliver();
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(MessageChannelDeliver.class);

    @BeforeEach
    void attachAppenderAndClearConnector() {
        TitanServerConnector.setConnector(null);
        this.appender.start();
        this.logger.addAppender(this.appender);
    }

    @AfterEach
    void detachAppenderAndUninstallConnector() {
        this.logger.detachAppender(this.appender);
        TitanServerConnector.setConnector(null);
    }

    @DisplayName("Sending to a task without a connector logs one WARN line naming the player, the task and the target")
    @Test
    void taskWithoutConnectorLogsOneWarning(Env env) {
        Player player = connect(env);
        DeliverComponent component = DeliverComponent.taskBuilder().taskName("cygnus").player(player).build();

        this.deliver.sendPlayer(player, component);

        Assertions.assertEquals(List.of(warning(player, "task", "cygnus")), messages(), "exactly one WARN line must be logged for the missed task transfer");
    }

    @DisplayName("Sending to a server without a connector logs one WARN line naming the player, the server and the target")
    @Test
    void serverWithoutConnectorLogsOneWarning(Env env) {
        Player player = connect(env);
        DeliverComponent component = DeliverComponent.serverBuilder().serverName("survival-1").player(player).build();

        this.deliver.sendPlayer(player, component);

        Assertions.assertEquals(List.of(warning(player, "server", "survival-1")), messages(), "exactly one WARN line must be logged for the missed server transfer");
    }

    @DisplayName("A missed transfer without a connector does not throw to the caller")
    @Test
    void missedTransferDoesNotThrow(Env env) {
        Player player = connect(env);
        DeliverComponent component = DeliverComponent.taskBuilder().taskName("cygnus").player(player).build();

        Assertions.assertDoesNotThrow(() -> this.deliver.sendPlayer(player, component), "a missing connector must not surface as an error to the player");
    }

    @DisplayName("Sending with an installed connector hands the task over and logs no warning")
    @Test
    void taskWithConnectorHandsOverWithoutWarning(Env env) {
        Player player = connect(env);
        RecordingConnector connector = new RecordingConnector();
        TitanServerConnector.setConnector(connector);
        DeliverComponent component = DeliverComponent.taskBuilder().taskName("cygnus").player(player).build();

        this.deliver.sendPlayer(player, component);

        Assertions.assertEquals(List.of("task " + player.getUuid() + " cygnus"), connector.calls, "the connector must receive the task transfer");
        Assertions.assertTrue(messages().isEmpty(), "a handed-over transfer must not log a warning");
    }

    @DisplayName("A null player logs nothing")
    @Test
    void nullPlayerLogsNothing(Env env) {
        Player player = connect(env);
        DeliverComponent component = DeliverComponent.taskBuilder().taskName("cygnus").player(player).build();

        this.deliver.sendPlayer(null, component);

        Assertions.assertTrue(messages().isEmpty(), "no log line may be written without a player");
    }

    @DisplayName("A null component logs nothing")
    @Test
    void nullComponentLogsNothing(Env env) {
        Player player = connect(env);

        this.deliver.sendPlayer(player, null);

        Assertions.assertTrue(messages().isEmpty(), "no log line may be written without a component");
    }

    private List<String> messages() {
        return this.appender.list.stream().map(event -> event.getLevel() + " " + event.getLoggerName() + " - " + event.getFormattedMessage()).toList();
    }

    private static Player connect(Env env) {
        Instance instance = env.createFlatInstance();
        return env.createConnection().connect(instance);
    }

    private static String warning(Player player, String kind, String target) {
        return "WARN net.onelitefeather.titan.platform.cloudnet.MessageChannelDeliver - Server connector missing: cannot send " + player.getUsername() + " (" + player.getUuid() + ") to " + kind + " " + target;
    }

    private static final class RecordingConnector implements ServerConnector {

        private final List<String> calls = new ArrayList<>();

        @Override
        public void connectToTask(UUID playerId, String taskName) {
            this.calls.add("task " + playerId + " " + taskName);
        }

        @Override
        public void connectToServer(UUID playerId, String serviceName) {
            this.calls.add("server " + playerId + " " + serviceName);
        }
    }
}
