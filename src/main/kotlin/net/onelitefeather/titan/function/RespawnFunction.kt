package net.onelitefeather.titan.function

import com.google.inject.Inject
import com.google.inject.name.Named
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerRespawnEvent

class RespawnFunction: TitanFunction {

    @Inject
    lateinit var navigationFunction: NavigationFunction

    @Named("titanNode")
    @Inject
    lateinit var eventNode: EventNode<Event>

    private fun respawnListener(event: PlayerRespawnEvent) {
        this.navigationFunction.setItems(event.player)
    }

    override fun initialize() {
        eventNode.addListener(
            PlayerRespawnEvent::class.java,
            this::respawnListener
        )
    }

    override fun terminate() {
    }
}