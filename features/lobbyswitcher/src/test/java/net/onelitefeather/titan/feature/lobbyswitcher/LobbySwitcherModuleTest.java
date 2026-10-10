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
package net.onelitefeather.titan.feature.lobbyswitcher;

import java.util.Locale;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Shutting the module down, and the texts a player is sent in their own language. */
@ExtendWith(MicrotusExtension.class)
class LobbySwitcherModuleTest {

    private static String tellFull(Env env, Locale locale) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Instance instance = env.createFlatInstance();
            TestConnection connection = env.createConnection();
            Player player = connection.connect(instance);
            player.setLocale(locale);
            Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);

            fixture.module().tell(player, SwitcherClickDecision.FULL, "Lobby-3");

            return PlainTextComponentSerializer.plainText().serialize(chat.collect().getFirst().message());
        }
    }

    @DisplayName("A player with de_DE is told the lobby is full in German")
    @Test
    void germanPlayerGetsGerman(Env env) {
        String text = tellFull(env, Locale.of("de", "DE"));

        Assertions.assertTrue(text.contains("voll"), "expected German, was: " + text);
    }

    @DisplayName("A player with ja_JP is told the lobby is full in English")
    @Test
    void japanesePlayerGetsEnglish(Env env) {
        String text = tellFull(env, Locale.of("ja", "JP"));

        Assertions.assertTrue(text.contains("full"), "expected the English fallback, was: " + text);
    }

    @DisplayName("After stopping, using the clock opens nothing")
    @Test
    void stoppedModuleOpensNothing(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = fixture.join(env.createFlatInstance());
            fixture.module().stop();

            fixture.use(player);
            env.tick();

            Assertions.assertNull(player.getOpenInventory(), "a stopped module must not open the inventory");
            Assertions.assertEquals(0, fixture.counts().reads(), "a stopped module must not read");
        }
    }

    @DisplayName("Stopping the inventory closes it for a viewer who has it open")
    @Test
    void stoppingTheInventoryClosesItForViewers(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            Player player = fixture.join(env.createFlatInstance());
            fixture.use(player);
            env.tick();
            Assertions.assertNotNull(player.getOpenInventory(), "precondition: the inventory is open");

            fixture.inventory().stop();

            Assertions.assertNull(player.getOpenInventory(), "unregistering must close the inventory for its viewers");
        }
    }

    @DisplayName("The module's event node is gone after stopping")
    @Test
    void stopRemovesTheEventNode(Env env) {
        try (SwitcherFixture fixture = SwitcherFixture.active(env)) {
            long before = fixture.titanNode().getChildren().stream().filter(child -> child.getName().equals("titan/lobbyswitcher")).count();
            fixture.module().stop();
            long after = fixture.titanNode().getChildren().stream().filter(child -> child.getName().equals("titan/lobbyswitcher")).count();

            Assertions.assertEquals(1, before, "precondition: the node is attached");
            Assertions.assertEquals(0, after, "stop() must detach the node");
        }
    }
}
