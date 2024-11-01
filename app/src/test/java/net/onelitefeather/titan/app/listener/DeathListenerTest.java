package net.onelitefeather.titan.app.listener;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MicrotusExtension.class)
class DeathListenerTest {

    @DisplayName("Test if the DeathListener sets the death text to empty")
    @Test
    void testDeathListenerForEmptyDeathText(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        MinecraftServer.getGlobalEventHandler().addListener(PlayerDeathEvent.class, new DeathListener());
        Collector<PlayerDeathEvent> playerDeathEventCollector = env.trackEvent(PlayerDeathEvent.class, EventFilter.PLAYER, player);
        player.kill();
        PlayerDeathEvent first = playerDeathEventCollector.collect().getFirst();

        playerDeathEventCollector.assertSingle();
        Assertions.assertEquals(Component.empty(), first.getDeathText());
    }

    @DisplayName("Test if the DeathListener call respawn on the player")
    @Test
    void testDeathListenerForRespawnCall(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = spy(env.createPlayer(flatInstance));
        MinecraftServer.getGlobalEventHandler().addListener(PlayerDeathEvent.class, new DeathListener());
        Collector<PlayerDeathEvent> playerDeathEventCollector = env.trackEvent(PlayerDeathEvent.class, EventFilter.PLAYER, player);
        player.kill();

        playerDeathEventCollector.assertSingle();
        verify(player).respawn();
    }

}
