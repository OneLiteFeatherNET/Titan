package net.onelitefeather.titan.function;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;

public final class ElytraFunction {

    private static final int SLOT = 45;

    public ElytraFunction(EventNode<Event> node) {
        node.addListener(PlayerStartFlyingWithElytraEvent.class, this::onPlayerStartFlyingWithElytra);
        node.addListener(PlayerStartFlyingWithElytraEvent.class, this::onPlayerStopFlyingWithElytra);
        node.addListener(PlayerUseItemEvent.class, this::onFireworkUse);
    }

    private void onPlayerStartFlyingWithElytra(PlayerStartFlyingWithElytraEvent event) {
        event.getPlayer().getInventory().setItemStack(SLOT, ItemModule.PLAYER_FIREWORK);
    }

    private void onPlayerStopFlyingWithElytra(PlayerStartFlyingWithElytraEvent event) {
        event.getPlayer().getInventory().setItemStack(SLOT, ItemStack.AIR);
    }

    private void onFireworkUse(PlayerUseItemEvent event) {
        var itemStack = event.getItemStack();
        if (itemStack.isSimilar(ItemModule.PLAYER_FIREWORK) && event.getPlayer().isFlyingWithElytra()) {
            event.getPlayer().setVelocity(event.getPlayer().getVelocity().normalize().add(event.getPlayer().getPosition().direction().mul(35.0)));
        }
    }

    public static ElytraFunction instance(EventNode<Event> node) {
        return new ElytraFunction(node);
    }
}
