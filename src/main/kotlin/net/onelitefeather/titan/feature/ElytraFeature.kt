package net.onelitefeather.titan.feature

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.ItemStack
import net.onelitefeather.titan.service.firework

class ElytraFeature(eventNode: EventNode<Event>) {

    init {
        eventNode.addListener(PlayerStartFlyingWithElytraEvent::class.java, this::playerStartFlyingWithElytraEvent)
        eventNode.addListener(PlayerStopFlyingWithElytraEvent::class.java, this::playerStopFlyingWithElytraEvent)
        eventNode.addListener(PlayerUseItemEvent::class.java, this::playerUseItemEvent)
    }

    private fun playerStartFlyingWithElytraEvent(event: PlayerStartFlyingWithElytraEvent) {
        event.player.inventory.setItemStack(45, firework)
    }

    private fun playerStopFlyingWithElytraEvent(event: PlayerStopFlyingWithElytraEvent) {
        event.player.inventory.setItemStack(45, ItemStack.AIR)
    }

    private fun playerUseItemEvent(event: PlayerUseItemEvent) {
        val item = event.itemStack
        if (item == firework && event.player.isFlyingWithElytra) {
            event.player.velocity = event.player.velocity.normalize().add(event.player.position.direction().mul(35.0))

            /*val spawnBlock = event.entity.position
            val instance = event.player.instance ?: return
            val customFirework = CustomFirework(event.player, EntityType.FIREWORK_ROCKET)
            customFirework.setInstance(instance, spawnBlock)*/
        }
    }


}