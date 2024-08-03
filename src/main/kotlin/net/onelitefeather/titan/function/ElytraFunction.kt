package net.onelitefeather.titan.function

import com.google.inject.Inject
import com.google.inject.name.Named
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.ItemStack

class ElytraFunction : TitanFunction {
    @Named("titanNode")
    @Inject
    lateinit var eventNode: EventNode<Event>

    @Named("playerFirework")
    @Inject
    lateinit var playerFirework: ItemStack

    private fun playerStartFlyingWithElytraEvent(event: PlayerStartFlyingWithElytraEvent) {
        event.player.inventory.setItemStack(45, playerFirework)
    }

    private fun playerStopFlyingWithElytraEvent(event: PlayerStopFlyingWithElytraEvent) {
        event.player.inventory.setItemStack(45, ItemStack.AIR)
    }

    private fun playerUseItemEvent(event: PlayerUseItemEvent) {
        val item = event.itemStack
        if (item == playerFirework && event.player.isFlyingWithElytra) {
            event.player.velocity = event.player.velocity.normalize().add(event.player.position.direction().mul(35.0))
        }
    }

    override fun initialize() {
        eventNode.addListener(PlayerStartFlyingWithElytraEvent::class.java, this::playerStartFlyingWithElytraEvent)
        eventNode.addListener(PlayerStopFlyingWithElytraEvent::class.java, this::playerStopFlyingWithElytraEvent)
        eventNode.addListener(PlayerUseItemEvent::class.java, this::playerUseItemEvent)
    }

    override fun terminate() {
    }

}