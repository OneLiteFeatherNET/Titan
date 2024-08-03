package net.onelitefeather.titan.function

import com.google.inject.Inject
import com.google.inject.name.Named
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.event.player.PlayerBlockBreakEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.event.player.PlayerSwapItemEvent
import net.onelitefeather.titan.helper.Cancelable

class PreventFunction : TitanFunction {

    @Named("titanNode")
    @Inject
    lateinit var eventNode: EventNode<Event>

    override fun initialize() {
        eventNode.addListener(PickupItemEvent::class.java, Cancelable::cancel)
        eventNode.addListener(
            PlayerBlockBreakEvent::class.java, Cancelable::cancel
        )
        eventNode.addListener(
            PlayerBlockPlaceEvent::class.java, Cancelable::cancel
        )
        eventNode.addListener(
            PlayerSwapItemEvent::class.java, Cancelable::cancel
        )
        eventNode.addListener(ItemDropEvent::class.java, Cancelable::cancel)
    }

    override fun terminate() {
    }
}