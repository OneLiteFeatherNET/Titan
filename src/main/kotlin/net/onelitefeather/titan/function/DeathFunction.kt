package net.onelitefeather.titan.function

import com.google.inject.Inject
import com.google.inject.name.Named
import net.kyori.adventure.text.Component
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerDeathEvent

class DeathFunction: TitanFunction {

    @Named("titanNode")
    @Inject
    lateinit var eventNode: EventNode<Event>

    private fun deathListener(event: PlayerDeathEvent) {
        event.deathText = Component.empty()
        event.player.respawn()
    }

    override fun initialize() {
        eventNode.addListener(
            PlayerDeathEvent::class.java,
            this::deathListener
        )
    }

    override fun terminate() {
    }
}