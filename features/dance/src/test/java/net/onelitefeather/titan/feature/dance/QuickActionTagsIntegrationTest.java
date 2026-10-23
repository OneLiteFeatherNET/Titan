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
import java.util.List;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.common.TagsPacket;
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

/** The quick-action tag reaches a player on the first spawn only, through the dance module. */
@ExtendWith(MicrotusExtension.class)
class QuickActionTagsIntegrationTest {

    private static final String QUICK_ACTIONS = "minecraft:quick_actions";
    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");

    @DisplayName("A first spawn sends the player the quick-action tag holding the dance dialog")
    @Test
    void firstSpawnSendsTheQuickActionTag(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<TagsPacket> tags = connection.trackIncoming(TagsPacket.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            try {
                env.process().eventHandler().call(new PlayerSpawnEvent(player, instance, true));

                List<TagsPacket> sent = tags.collect();
                Assertions.assertEquals(1, sent.size(), "a first spawn must send the quick-action tag once");
                int dialogId = MinecraftServer.getDialogRegistry().getId(MinecraftServer.getDialogRegistry().getKey(DanceKeys.DIALOG));
                Assertions.assertArrayEquals(new int[]{dialogId}, entries(sent.getFirst()), "the quick-action tag must hold the id of titan:dances");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A spawn that is not the first does not send the quick-action tag again")
    @Test
    void respawnDoesNotSendTheQuickActionTagAgain(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<TagsPacket> tags = connection.trackIncoming(TagsPacket.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            try {
                env.process().eventHandler().call(new PlayerSpawnEvent(player, instance, true));
                env.process().eventHandler().call(new PlayerSpawnEvent(player, instance, false));

                Assertions.assertEquals(1, tags.collect().size(), "only the first spawn may send the quick-action tag");
            } finally {
                module.stop();
            }
        }
    }

    private static int[] entries(TagsPacket packet) {
        return packet.registries().getFirst().tags().stream().filter(tag -> tag.identifier().equals(QUICK_ACTIONS)).findFirst().map(TagsPacket.Tag::entries).orElseThrow(() -> new AssertionError("the packet is missing " + QUICK_ACTIONS));
    }
}
