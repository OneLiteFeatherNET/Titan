package net.onelitefeather.titan.function;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket;
import net.onelitefeather.titan.event.EntityDismountEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SitFunction {

    private static final double SIT_OFFSET = 0.25;

    private final Map<UUID, Pos> playerExitLocations = new HashMap<>();
    private final Map<UUID, Entity> playerSitsMap = new HashMap<>();

    private SitFunction(EventNode<Event> eventNode) {
        eventNode.addListener(PlayerBlockInteractEvent.class, this::onInteract);
        eventNode.addListener(PlayerPacketEvent.class, this::onPacket);
        eventNode.addListener(EntityDismountEvent.class, this::onDismount);
        eventNode.addListener(PlayerDisconnectEvent.class, this::onDisconnect);

    }

    private void onPacket(PlayerPacketEvent event) {
        if (event.getPacket() instanceof ClientSteerVehiclePacket packet) {
            var ridingEntity = event.getPlayer().getVehicle();
            if (packet.flags() == 2 && ridingEntity != null) {
                var entityDismountEvent = new EntityDismountEvent(ridingEntity, event.getPlayer());
                EventDispatcher.call(entityDismountEvent);
            }
         }
    }

    private void onDisconnect(PlayerDisconnectEvent event) {
        removePlayer(event.getPlayer());
    }

    private void onDismount(EntityDismountEvent event) {
        if (event.rider() instanceof Player player && isSitting(player)) {
            removePlayer(player);
        }
    }

    private void onInteract(PlayerBlockInteractEvent event) {
        if (event.getBlock().namespace() == Block.SPRUCE_STAIRS.namespace()) {
            sitPlayer(event.getPlayer(), event.getBlockPosition());
        }
    }

    private void sitPlayer(Player player, Point sitLocation) {
        var instance = player.getInstance();
        if (instance == null) return;
        var playerLocation = player.getPosition();
        var arrow = new Entity(EntityType.ARROW);
        arrow.setInstance(instance, sitLocation.add(0.5, SIT_OFFSET, 0.5));
        arrow.setInvisible(true);
        arrow.setSilent(true);

        playerExitLocations.put(player.getUuid(), playerLocation);
        arrow.addPassenger(player);

        playerSitsMap.remove(player.getUuid());
        playerSitsMap.put(player.getUuid(), arrow);
    }

    private void removePlayer(Player player) {
        var arrow = playerSitsMap.getOrDefault(player.getUuid(), null);
        if (arrow != null) {
            arrow.removePassenger(player);
            arrow.remove();

            var exitLocation = playerExitLocations.remove(player.getUuid());
            if (exitLocation != null) {
                player.teleport(exitLocation);
            }
        }
    }

    private boolean isSitting(Player player) {
        return playerSitsMap.containsKey(player.getUuid());
    }

    public static SitFunction instance(EventNode<Event> eventNode) {
        return new SitFunction(eventNode);
    }

}
