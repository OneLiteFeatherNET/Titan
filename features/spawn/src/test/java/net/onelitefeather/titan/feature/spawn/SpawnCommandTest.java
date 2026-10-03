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
package net.onelitefeather.titan.feature.spawn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.LobbyReturnToSpawnEvent;
import net.onelitefeather.titan.core.module.SpawnReturn;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Cyano {@code Env} coverage for {@code /spawn} end to end: the command, {@link LobbySpawnReturn}
 * and {@link SpawnMessages}, with the command manager of the {@code Env}.
 */
@ExtendWith(MicrotusExtension.class)
class SpawnCommandTest {

    private static final Pos SPAWN = new Pos(5, 64, 5);
    private static final Pos ROOF = new Pos(40, 90, 40);

    /** A started spawn command and the player who stands on a roof, with chat tracking. */
    private record Scene(SpawnCommands commands, SpawnMessages messages, Player player,
                         Collector<SystemChatPacket> chat,
                         CommandManager commandManager) implements AutoCloseable {

        static Scene start(Env env, Supplier<Pos> spawn, Locale locale) {
            SpawnMessages messages = new SpawnMessages();
            messages.register();
            CommandManager commandManager = env.process().command();
            SpawnCommands commands = new SpawnCommands(commandManager, new LobbySpawnReturn(spawn::get, messages));
            commands.start();
            Instance instance = env.createFlatInstance();
            TestConnection connection = env.createConnection();
            Player player = connection.connect(instance);
            player.setLocale(locale);
            player.teleport(ROOF);
            return new Scene(commands, messages, player, connection.trackIncoming(SystemChatPacket.class), commandManager);
        }

        void run() {
            this.commandManager.execute(this.player, "spawn");
        }

        @Override
        public void close() {
            this.commands.stop();
            this.messages.close();
        }
    }

    @DisplayName("A player on a roof runs /spawn and stands at the spawn point with a confirmation")
    @Test
    void playerOnARoofIsReturnedAndTold(Env env) {
        try (Scene scene = Scene.start(env, () -> SPAWN, Locale.ENGLISH)) {
            scene.run();

            Assertions.assertEquals(SPAWN, scene.player().getPosition(), "the player must stand at the spawn point");
            Component expected = scene.messages().returned(Locale.ENGLISH);
            scene.chat().assertSingle(packet -> Assertions.assertEquals(expected, packet.message(), "the confirmation must arrive"));
        }
    }

    @DisplayName("A German client gets the confirmation in German")
    @Test
    void germanClientGetsGerman(Env env) {
        try (Scene scene = Scene.start(env, () -> SPAWN, Locale.GERMANY)) {
            scene.run();

            Component german = scene.messages().returned(Locale.GERMAN);
            Assertions.assertNotEquals(scene.messages().returned(Locale.ENGLISH), german, "the bundles must differ, otherwise this test proves nothing");
            scene.chat().assertSingle(packet -> Assertions.assertEquals(german, packet.message(), "the text must be German"));
        }
    }

    @DisplayName("A client language without a translation gets English")
    @Test
    void unknownLanguageFallsBackToEnglish(Env env) {
        try (Scene scene = Scene.start(env, () -> SPAWN, Locale.JAPAN)) {
            scene.run();

            Component english = scene.messages().returned(Locale.ENGLISH);
            scene.chat().assertSingle(packet -> Assertions.assertEquals(english, packet.message(), "the text must fall back to English"));
        }
    }

    @DisplayName("Without a spawn point the player gets the message and is not moved")
    @Test
    void noSpawnPointTellsAndDoesNotMove(Env env) {
        try (Scene scene = Scene.start(env, () -> null, Locale.ENGLISH)) {
            scene.run();

            Assertions.assertEquals(ROOF, scene.player().getPosition(), "the player must stay where they are");
            Component expected = scene.messages().noSpawn(Locale.ENGLISH);
            scene.chat().assertSingle(packet -> Assertions.assertEquals(expected, packet.message(), "the no-spawn message must arrive"));
        }
    }

    @DisplayName("A gliding player stops gliding")
    @Test
    void glidingPlayerStopsGliding(Env env) {
        try (Scene scene = Scene.start(env, () -> SPAWN, Locale.ENGLISH)) {
            scene.player().setEquipment(net.minestom.server.entity.EquipmentSlot.CHESTPLATE, ItemStack.of(Material.ELYTRA));
            scene.player().setFlyingWithElytra(true);
            Assertions.assertTrue(scene.player().isFlyingWithElytra(), "precondition: the player glides before /spawn");

            scene.run();

            Assertions.assertFalse(scene.player().isFlyingWithElytra(), "the player must not glide on from spawn");
        }
    }

    @DisplayName("The console gets the operator message; nobody is returned and no event fires")
    @Test
    void consoleIsToldAndNobodyIsReturned(Env env) {
        List<String> received = new ArrayList<>();
        ConsoleSender console = new ConsoleSender() {
            @Override
            public void sendMessage(String message) {
                received.add(message);
            }
        };
        int[] returns = {0};
        SpawnReturn fake = new SpawnReturn() {
            @Override
            public Result sendToSpawn(Player player) {
                returns[0]++;
                return Result.RETURNED;
            }

            @Override
            public void sendToSpawnAndTell(Player player) {
                returns[0]++;
            }
        };
        List<LobbyReturnToSpawnEvent> events = new ArrayList<>();
        EventNode<net.minestom.server.event.Event> node = EventNode.all("spawn-command-test");
        node.addListener(LobbyReturnToSpawnEvent.class, events::add);
        env.process().eventHandler().addChild(node);
        CommandManager commandManager = env.process().command();
        SpawnCommands commands = new SpawnCommands(commandManager, fake);
        commands.start();
        try {
            commandManager.execute(console, "spawn");

            Assertions.assertEquals(List.of(SpawnCommand.CONSOLE_MESSAGE), received, "the console must get the operator message");
            Assertions.assertEquals(0, returns[0], "SpawnReturn must never be called for the console");
            Assertions.assertTrue(events.isEmpty(), "no return event may fire for the console");
        } finally {
            commands.stop();
            env.process().eventHandler().removeChild(node);
        }
    }

    @DisplayName("After @PreDestroy /spawn is no longer registered")
    @Test
    void stopUnregistersTheCommand(Env env) {
        CommandManager commandManager = env.process().command();
        SpawnCommands commands = new SpawnCommands(commandManager, new LobbySpawnReturn(() -> SPAWN, new SpawnMessages()));
        commands.start();
        Assertions.assertTrue(commandManager.commandExists("spawn"), "spawn must be registered after start()");

        commands.stop();

        Assertions.assertFalse(commandManager.commandExists("spawn"), "spawn must be unregistered after @PreDestroy");
    }
}
