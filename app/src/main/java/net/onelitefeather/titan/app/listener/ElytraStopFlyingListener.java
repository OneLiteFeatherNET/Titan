package net.onelitefeather.titan.app.listener;

import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class ElytraStopFlyingListener implements Consumer<PlayerStopFlyingWithElytraEvent> {

    @Override
    public void accept(@NotNull PlayerStopFlyingWithElytraEvent event) {
        Player player = event.getPlayer();
        player.getInventory().setItemStack(EquipmentSlot.OFF_HAND.armorSlot(), ItemStack.AIR, true);
    }
}
