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
package net.onelitefeather.titan.app.feature.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import net.kyori.adventure.text.Component;
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
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.testing.ModuleHarness;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Env integration coverage for {@link ExampleModule} through a real {@link ModuleHarness} - the
 * top of the test pyramid described in {@code docs/lobby-modules.md}: item use dispatch, the
 * command's lifecycle, and config read from a temp {@code app.json}. The pure cooldown and
 * formatting rule already has its own coverage in {@link ExampleGreetingRuleTest}; this class only
 * checks that the module wires it to the platform correctly.
 *
 * <p>Every test uses a fixed {@link Clock} (F.I.R.S.T. - repeatable), so "now" never depends on
 * when the test happens to run.
 */
@ExtendWith(MicrotusExtension.class)
class ExampleModuleTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static ExampleModule fixedClockModule() {
        return new ExampleModule(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @DisplayName("Using the greeting token sends the configured greeting")
    @Test
    void usingTheGreetingTokenSendsTheConfiguredGreeting(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        try (ModuleHarness harness = ModuleHarness.start(env, fixedClockModule())) {
            harness.items().equip(player);
            ItemStack token = player.getInventory().getItemStack(ExampleModule.GREETING_TOKEN_SLOT);
            Assertions.assertEquals(Material.FEATHER, token.material(), "equip() must place the greeting token on its configured hotbar slot");
            Assertions.assertEquals("titan:example", token.getTag(ItemRegistry.IDENTITY_TAG), "the handed-out stack must carry the registry's identity tag so its use reaches this module");

            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, token, 0L));

            messages.assertSingle(message -> Assertions.assertEquals(ExampleGreetingRule.greeting(ExampleConfig.DEFAULTS.greeting(), player.getUsername()), message.message()));
        }
    }

    @DisplayName("Using the greeting token again within the cooldown sends the on-cooldown message instead")
    @Test
    void usingTheGreetingTokenAgainWithinTheCooldownSendsTheOnCooldownMessage(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        try (ModuleHarness harness = ModuleHarness.start(env, fixedClockModule())) {
            harness.items().equip(player);
            ItemStack token = player.getInventory().getItemStack(ExampleModule.GREETING_TOKEN_SLOT);
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, token, 0L));
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, token, 1L));

            // Collector#collect() consumes the tracker (see Cyano's IncomingCollector), so both
            // uses must happen before the one and only collect() call below - not one collect()
            // per use.
            List<SystemChatPacket> collected = messages.collect();
            Assertions.assertEquals(2, collected.size(), "the second use must still send a message, just not a fresh greeting");
            Assertions.assertEquals(ExampleGreetingRule.greeting(ExampleConfig.DEFAULTS.greeting(), player.getUsername()), collected.get(0).message());
            Assertions.assertEquals(ExampleItems.ON_COOLDOWN, collected.get(1).message(), "a second use within the cooldown must not send a fresh greeting");
        }
    }

    @DisplayName("Using a look-alike token that was never registered does not dispatch to this module")
    @Test
    void usingAnUnregisteredLookAlikeTokenDoesNotDispatch(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        try (ModuleHarness harness = ModuleHarness.start(env, fixedClockModule())) {
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, ItemStack.of(Material.FEATHER), 0L));

            messages.assertEmpty();
        }
    }

    @DisplayName("Running the command greets the sender")
    @Test
    void runningTheCommandGreetsTheSender(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        try (ModuleHarness harness = ModuleHarness.start(env, fixedClockModule())) {
            env.process().command().execute(player, ExampleModule.COMMAND_NAME);

            messages.assertSingle(message -> Assertions.assertEquals(ExampleGreetingRule.greeting(ExampleConfig.DEFAULTS.greeting(), player.getUsername()), message.message()));
        }
    }

    @DisplayName("The command is registered while the module is enabled, and gone once the harness closes")
    @Test
    void commandIsRegisteredWhileEnabledAndGoneAfterClose(Env env) {
        ModuleHarness harness = ModuleHarness.start(env, fixedClockModule());
        try {
            Assertions.assertTrue(env.process().command().commandExists(ExampleModule.COMMAND_NAME), "the command must exist while the module is enabled");
        } finally {
            harness.close();
        }

        Assertions.assertFalse(env.process().command().commandExists(ExampleModule.COMMAND_NAME), "the command must be gone once the module is disabled");
    }

    @DisplayName("Disconnecting clears a player's cooldown, so a rejoining player is greeted immediately")
    @Test
    void disconnectingClearsAPlayersCooldown(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        try (ModuleHarness harness = ModuleHarness.start(env, fixedClockModule())) {
            harness.items().equip(player);
            ItemStack token = player.getInventory().getItemStack(ExampleModule.GREETING_TOKEN_SLOT);
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, token, 0L));
            env.process().eventHandler().call(new PlayerDisconnectEvent(player));
            env.process().eventHandler().call(new PlayerUseItemEvent(player, PlayerHand.MAIN, token, 1L));

            // Both uses must happen before this one collect() call - see the comment in
            // usingTheGreetingTokenAgainWithinTheCooldownSendsTheOnCooldownMessage().
            List<SystemChatPacket> collected = messages.collect();
            Assertions.assertEquals(2, collected.size());
            Assertions.assertEquals(ExampleGreetingRule.greeting(ExampleConfig.DEFAULTS.greeting(), player.getUsername()), collected.get(1).message(), "a disconnected player's cooldown must be forgotten, not carried over");
        }
    }

    @DisplayName("The module reads its own section from a temp app.json")
    @Test
    void readsItsOwnSectionFromATempAppJson(Env env, @TempDir Path dir) throws IOException {
        Path file = dir.resolve("app.json");
        Files.writeString(file, "{\"configVersion\":2,\"example\":{\"greeting\":\"Hi %s, enjoy the lobby!\",\"cooldownMillis\":0}}");
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        try (ModuleHarness harness = ModuleHarness.start(env, file, fixedClockModule())) {
            env.process().command().execute(player, ExampleModule.COMMAND_NAME);

            messages.assertSingle(message -> Assertions.assertEquals(Component.text("Hi " + player.getUsername() + ", enjoy the lobby!"), message.message(), "the module must read the configured greeting from its own section, not the default"));
        }
    }
}
