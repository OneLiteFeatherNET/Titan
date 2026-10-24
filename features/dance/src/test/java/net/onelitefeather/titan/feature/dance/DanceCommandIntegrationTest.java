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
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import net.minestom.server.command.CommandManager;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;
import net.minestom.server.network.packet.server.common.TagsPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.AdjustableClock;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** {@code /dance} opens the same dance list as the quick action, and is the only dance command. */
@ExtendWith(MicrotusExtension.class)
class DanceCommandIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");

    @DisplayName("/dance opens the dance list for the player who typed it")
    @Test
    void danceOpensTheList(Env env) {
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

                Assertions.assertEquals(1, dialogs.collect().size(), "/dance must show the dance list once");
            } finally {
                registration.stop();
                module.stop();
            }
        }
    }

    @DisplayName("/dance resends the quick action for the player's language and permissions")
    @Test
    void danceResendsTheQuickAction(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        player.setLocale(Locale.GERMANY);
        Collector<TagsPacket> tags = connection.trackIncoming(TagsPacket.class);

        CommandManager commands = env.process().command();
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService().allowEveryone("titan.dance.spin"), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            DanceCommands registration = new DanceCommands(commands, module);
            registration.start();
            try {
                commands.execute(player, DanceCommand.NAME);

                // With the spin permission the German list offers every dance.
                int expected = QuickActionTagsIntegrationTest.registeredId(DanceList.key(Locale.GERMAN, EnumSet.allOf(Dance.class)));
                List<TagsPacket> sent = tags.collect();
                Assertions.assertEquals(1, sent.size(), "/dance must send the player's quick action once");
                Assertions.assertArrayEquals(new int[]{expected}, QuickActionTagsIntegrationTest.quickActionEntries(sent.getFirst()), "the quick action must be the German list with spin");
            } finally {
                registration.stop();
                module.stop();
            }
        }
    }
}
