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
package net.onelitefeather.titan.feature.respawn;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerRespawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * Env integration coverage for {@link RespawnModule}, built directly with a
 * {@link Mockito#mock(Class) mocked} {@link LobbyItems}: a death produces no message and a respawn
 * by the next tick that hands the loadout back via {@code equip}, and {@link RespawnModule#stop()}
 * leaves the player untouched by further events.
 *
 * <p>{@link LobbyItems} is mocked rather than its real implementation ({@code HotbarLobbyItems}):
 * that implementation lives in {@code features/hotbar}, a sibling column this column must not
 * depend on, even in tests. What actually placing an item in a player's inventory looks like is
 * {@code HotbarLobbyItems}'s own concern and its own test's job.
 */
@ExtendWith(MicrotusExtension.class)
class RespawnModuleTest {

    private static RespawnModule startedModule(TestTitanNode titan, LobbyItems lobbyItems) {
        RespawnModule module = new RespawnModule(titan.node(), lobbyItems, Telemetry.noop());
        module.start();
        return module;
    }

    @DisplayName("A player's death produces no death message")
    @Test
    void deathProducesNoMessage(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        LobbyItems lobbyItems = Mockito.mock(LobbyItems.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            RespawnModule module = startedModule(titan, lobbyItems);
            try {
                Collector<PlayerDeathEvent> collector = env.trackEvent(PlayerDeathEvent.class, EventFilter.PLAYER, player);

                player.kill();

                collector.assertSingle();
                PlayerDeathEvent first = collector.collect().getFirst();
                Assertions.assertEquals(Component.empty(), first.getDeathText());
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A real death triggers a respawn - with the platform's loadout handed back - by the next tick")
    @Test
    void deathTriggersARespawnWithLoadoutByTheNextTick(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        LobbyItems lobbyItems = Mockito.mock(LobbyItems.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            RespawnModule module = startedModule(titan, lobbyItems);
            try {
                Collector<PlayerDeathEvent> deathCollector = env.trackEvent(PlayerDeathEvent.class, EventFilter.PLAYER, player);
                Collector<PlayerRespawnEvent> respawnCollector = env.trackEvent(PlayerRespawnEvent.class, EventFilter.PLAYER, player);

                // kill() dispatches PlayerDeathEvent before isDead() flips true, and respawn() is
                // a no-op while isDead() is false - a listener calling it directly would no-op.
                player.kill();
                Assertions.assertTrue(player.isDead(), "the respawn must not happen synchronously inside the death event - only once a later tick runs");

                // collect() (used below) unmaps its listener as a side effect, so it must run
                // after this tick, which produces the event under test.
                env.tick();

                deathCollector.assertSingle();
                Assertions.assertEquals(Component.empty(), deathCollector.collect().getFirst().getDeathText(), "the death text must be blanked");
                respawnCollector.assertSingle();
                Assertions.assertFalse(player.isDead(), "the player must be alive again after the next tick, without waiting for a respawn screen");
                Mockito.verify(lobbyItems).equip(player);
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A respawn hands the player exactly the platform's loadout")
    @Test
    void respawnEquipsThePlatformLoadout(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        LobbyItems lobbyItems = Mockito.mock(LobbyItems.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            RespawnModule module = startedModule(titan, lobbyItems);
            try {
                env.process().eventHandler().call(new PlayerRespawnEvent(player));

                Mockito.verify(lobbyItems).equip(player);
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Once stopped, neither death nor respawn is handled any more")
    @Test
    void moduleStopsReactingAfterStop(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        LobbyItems lobbyItems = Mockito.mock(LobbyItems.class);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            RespawnModule module = startedModule(titan, lobbyItems);
            module.stop();

            Collector<PlayerDeathEvent> deathCollector = env.trackEvent(PlayerDeathEvent.class, EventFilter.PLAYER, player);
            Collector<PlayerRespawnEvent> respawnCollector = env.trackEvent(PlayerRespawnEvent.class, EventFilter.PLAYER, player);
            player.kill();
            deathCollector.assertSingle();
            PlayerDeathEvent first = deathCollector.collect().getFirst();
            Assertions.assertNotEquals(Component.empty(), first.getDeathText(), "the death text must be untouched once the module is stopped");
            respawnCollector.assertEmpty();
            Assertions.assertTrue(player.isDead(), "the player must stay dead once the module is stopped");

            env.process().eventHandler().call(new PlayerRespawnEvent(player));
            Mockito.verifyNoInteractions(lobbyItems);
        }
    }
}
