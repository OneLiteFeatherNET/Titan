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
package net.onelitefeather.titan.apps.cloudnet.feature.example;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.feature.hotbar.HotbarLobbyItems;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code Env} integration coverage for {@link ExampleModule}: item use dispatch through
 * {@link HotbarLobbyItems} and the disconnect cleanup listener.
 *
 * <p>Every test uses a fixed {@link Clock} (F.I.R.S.T. - repeatable), so "now" never depends on
 * when the test runs.
 */
@ExtendWith(MicrotusExtension.class)
class ExampleModuleTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static ExampleModule fixedClockModule(TestTitanNode titan) {
        return new ExampleModule(titan.node(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static HotbarLobbyItems itemsFor(TestTitanNode titan, ExampleModule module) {
        LobbyItem token = new ExampleGreetingItems().greetingToken(module);
        return new HotbarLobbyItems(List.of(token), titan.node(), Telemetry.noop());
    }

    @DisplayName("Using the greeting token sends the configured greeting")
    @Test
    void usingTheGreetingTokenSendsTheConfiguredGreeting(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ExampleModule module = fixedClockModule(titan);
            module.start();
            HotbarLobbyItems lobbyItems = itemsFor(titan, module);
            try {
                lobbyItems.equip(player);
                ItemStack token = player.getInventory().getItemStack(ItemSlot.MAX_HOTBAR_SLOT);
                Assertions.assertEquals(Material.FEATHER, token.material(), "equip() must place the greeting token on its configured hotbar slot");
                Assertions.assertEquals("titan:example", token.getTag(HotbarLobbyItems.IDENTITY_TAG), "the handed-out stack must carry HotbarLobbyItems' identity tag so its use reaches this feature");

                env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, token, 0L));

                messages.assertSingle(message -> Assertions.assertEquals(ExampleGreetingRule.greeting(ExampleModule.DEFAULT_GREETING, player.getUsername()), message.message()));
            } finally {
                module.stop();
                lobbyItems.stop();
            }
        }
    }

    @DisplayName("Using the greeting token again within the cooldown sends the on-cooldown message instead")
    @Test
    void usingTheGreetingTokenAgainWithinTheCooldownSendsTheOnCooldownMessage(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ExampleModule module = fixedClockModule(titan);
            module.start();
            HotbarLobbyItems lobbyItems = itemsFor(titan, module);
            try {
                lobbyItems.equip(player);
                ItemStack token = player.getInventory().getItemStack(ItemSlot.MAX_HOTBAR_SLOT);
                env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, token, 0L));
                env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, token, 1L));

                // collect() consumes the tracker, so both uses must happen before this one call,
                // not one collect() per use.
                List<SystemChatPacket> collected = messages.collect();
                Assertions.assertEquals(2, collected.size(), "the second use must still send a message, just not a fresh greeting");
                Assertions.assertEquals(ExampleGreetingRule.greeting(ExampleModule.DEFAULT_GREETING, player.getUsername()), collected.get(0).message());
                Assertions.assertEquals(ExampleItems.ON_COOLDOWN, collected.get(1).message(), "a second use within the cooldown must not send a fresh greeting");
            } finally {
                module.stop();
                lobbyItems.stop();
            }
        }
    }

    @DisplayName("Using a look-alike token that was never registered does not dispatch to this feature")
    @Test
    void usingAnUnregisteredLookAlikeTokenDoesNotDispatch(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ExampleModule module = fixedClockModule(titan);
            module.start();
            HotbarLobbyItems lobbyItems = itemsFor(titan, module);
            try {
                env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, ItemStack.of(Material.FEATHER), 0L));

                messages.assertEmpty();
            } finally {
                module.stop();
                lobbyItems.stop();
            }
        }
    }

    @DisplayName("Disconnecting clears a player's cooldown, so a rejoining player is greeted immediately")
    @Test
    void disconnectingClearsAPlayersCooldown(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ExampleModule module = fixedClockModule(titan);
            module.start();
            HotbarLobbyItems lobbyItems = itemsFor(titan, module);
            try {
                lobbyItems.equip(player);
                ItemStack token = player.getInventory().getItemStack(ItemSlot.MAX_HOTBAR_SLOT);
                env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, token, 0L));
                env.process().eventHandler().call(new PlayerDisconnectEvent(player));
                env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, token, 1L));

                // Both uses must happen before this one collect() call - see the comment in
                // usingTheGreetingTokenAgainWithinTheCooldownSendsTheOnCooldownMessage().
                List<SystemChatPacket> collected = messages.collect();
                Assertions.assertEquals(2, collected.size());
                Assertions.assertEquals(ExampleGreetingRule.greeting(ExampleModule.DEFAULT_GREETING, player.getUsername()), collected.get(1).message(), "a disconnected player's cooldown must be forgotten, not carried over");
            } finally {
                module.stop();
                lobbyItems.stop();
            }
        }
    }

    @DisplayName("Once stopped, disconnecting no longer clears a player's cooldown")
    @Test
    void stopLeavesNoListenerBehind(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ExampleModule module = fixedClockModule(titan);
            module.start();
            HotbarLobbyItems lobbyItems = itemsFor(titan, module);
            try {
                lobbyItems.equip(player);
                ItemStack token = player.getInventory().getItemStack(ItemSlot.MAX_HOTBAR_SLOT);
                env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, token, 0L));
                module.stop();

                // stop() detaches this feature's own event node, so the disconnect below has no
                // listener left to clear the cooldown.
                env.process().eventHandler().call(new PlayerDisconnectEvent(player));
                env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, token, 1L));

                List<SystemChatPacket> collected = messages.collect();
                Assertions.assertEquals(2, collected.size());
                Assertions.assertEquals(ExampleItems.ON_COOLDOWN, collected.get(1).message(), "a stopped feature must no longer clear a player's cooldown on disconnect");
            } finally {
                lobbyItems.stop();
            }
        }
    }
}
