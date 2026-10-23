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
package net.onelitefeather.titan.feature.dance;

import java.time.Instant;
import java.time.ZoneOffset;
import net.minestom.server.command.CommandManager;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** {@code /tanz} and its alias open the same per-player choice as the quick action. */
@ExtendWith(MicrotusExtension.class)
class DanceCommandIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");

    @DisplayName("/tanz opens the dance choice for the player who typed it")
    @Test
    void tanzOpensTheChoice(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<ShowDialogPacket> dialogs = connection.trackIncoming(ShowDialogPacket.class);

        CommandManager commands = env.process().command();
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            DanceCommands registration = new DanceCommands(commands, module);
            registration.start();
            try {
                commands.execute(player, DanceCommand.NAME);

                Assertions.assertEquals(1, dialogs.collect().size(), "/tanz must show the dance choice once");
            } finally {
                registration.stop();
                module.stop();
            }
        }
    }

    @DisplayName("The alias /dance opens the same choice")
    @Test
    void danceAliasOpensTheChoice(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<ShowDialogPacket> dialogs = connection.trackIncoming(ShowDialogPacket.class);

        CommandManager commands = env.process().command();
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            DanceCommands registration = new DanceCommands(commands, module);
            registration.start();
            try {
                commands.execute(player, DanceCommand.ALIAS);

                Assertions.assertEquals(1, dialogs.collect().size(), "/dance must show the dance choice once");
            } finally {
                registration.stop();
                module.stop();
            }
        }
    }

    @DisplayName("The quick action's button opens the choice for the clicking player")
    @Test
    void quickActionOpensTheChoice(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<ShowDialogPacket> dialogs = connection.trackIncoming(ShowDialogPacket.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.openClick(player));

                Assertions.assertEquals(1, dialogs.collect().size(), "the quick action's button must show the choice to the clicking player");
            } finally {
                module.stop();
            }
        }
    }
}
