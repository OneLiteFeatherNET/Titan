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
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSettingsChangeEvent;
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

/**
 * The quick action of each player: the dance list in his language with his dances, sent on the
 * first spawn and again when his language changes.
 */
@ExtendWith(MicrotusExtension.class)
class QuickActionTagsIntegrationTest {

    private static final String QUICK_ACTIONS = "minecraft:quick_actions";
    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");

    /** The registry id of the dance list {@code key}, as the client sees it. */
    static int registeredId(Key key) {
        return MinecraftServer.getDialogRegistry().getId(MinecraftServer.getDialogRegistry().getKey(key));
    }

    /** The ids in the quick-action tag of {@code packet}; fails if the packet has no such tag. */
    static int[] quickActionEntries(TagsPacket packet) {
        return packet.registries().getFirst().tags().stream().filter(tag -> tag.identifier().equals(QUICK_ACTIONS)).findFirst().map(TagsPacket.Tag::entries).orElseThrow(() -> new AssertionError("the packet is missing " + QUICK_ACTIONS));
    }

    @DisplayName("A first spawn sends the English list to an English player")
    @Test
    void firstSpawnSendsTheEnglishList(Env env) {
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
                Assertions.assertArrayEquals(new int[]{registeredId(DanceList.key(Locale.ENGLISH, EnumSet.noneOf(Dance.class)))}, quickActionEntries(sent.getFirst()), "an English player without spin gets titan:dances/en");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A German player gets the German list")
    @Test
    void germanPlayerGetsTheGermanList(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        player.setLocale(Locale.GERMANY);
        Collector<TagsPacket> tags = connection.trackIncoming(TagsPacket.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            try {
                env.process().eventHandler().call(new PlayerSpawnEvent(player, instance, true));

                Assertions.assertArrayEquals(new int[]{registeredId(DanceList.key(Locale.GERMAN, EnumSet.noneOf(Dance.class)))}, quickActionEntries(tags.collect().getFirst()), "a German player without spin gets titan:dances/de");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A player with the spin permission gets the list that offers spin")
    @Test
    void spinPermissionSelectsTheSpinList(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<TagsPacket> tags = connection.trackIncoming(TagsPacket.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService().grant("titan.dance.spin"), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            try {
                env.process().eventHandler().call(new PlayerSpawnEvent(player, instance, true));

                Assertions.assertArrayEquals(new int[]{registeredId(DanceList.key(Locale.ENGLISH, EnumSet.of(Dance.SPIN)))}, quickActionEntries(tags.collect().getFirst()), "an English player with spin gets titan:dances/en_spin");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A change of language sends the quick action for the new language again")
    @Test
    void languageChangeResendsTheQuickAction(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<TagsPacket> tags = connection.trackIncoming(TagsPacket.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            try {
                env.process().eventHandler().call(new PlayerSpawnEvent(player, instance, true));
                player.setLocale(Locale.GERMANY);
                env.process().eventHandler().call(new PlayerSettingsChangeEvent(player));

                List<TagsPacket> sent = tags.collect();
                Assertions.assertEquals(2, sent.size(), "the settings change must send the quick action again");
                Assertions.assertArrayEquals(new int[]{registeredId(DanceList.key(Locale.GERMAN, EnumSet.noneOf(Dance.class)))}, quickActionEntries(sent.getLast()), "after the change the German list is the quick action");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A spawn that is not the first does not send the quick action again")
    @Test
    void respawnDoesNotSendTheQuickActionAgain(Env env) {
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
}
