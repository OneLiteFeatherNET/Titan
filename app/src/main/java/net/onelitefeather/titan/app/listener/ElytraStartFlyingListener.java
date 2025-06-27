package net.onelitefeather.titan.app.listener;

import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.onelitefeather.titan.common.utils.Items;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class ElytraStartFlyingListener implements Consumer<PlayerStartFlyingWithElytraEvent> {

    @Override
    public void accept(@NotNull PlayerStartFlyingWithElytraEvent event) {
        Player player = event.getPlayer();
        player.getInventory().setItemStack(EquipmentSlot.OFF_HAND.armorSlot(), Items.PLAYER_FIREWORK, true);
    }
}
