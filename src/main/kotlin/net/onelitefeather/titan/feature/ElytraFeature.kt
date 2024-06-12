package net.onelitefeather.titan.feature

import net.kyori.adventure.text.Component
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

class ElytraFeature(eventNode: EventNode<Event>) {

    private val firework =
        ItemStack.builder(Material.FIREWORK_ROCKET)
            .displayName(
                Component.text("Firework Rocket")
            )
            .build()

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
        }
    }


}