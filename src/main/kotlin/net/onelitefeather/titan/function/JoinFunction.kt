package net.onelitefeather.titan.function

import com.google.inject.Inject
import com.google.inject.name.Named
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.network.packet.server.CachedPacket
import net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket
import net.onelitefeather.titan.helper.Cancelable

class JoinFunction : TitanFunction {

    @Named("spawnPos")
    @Inject
    lateinit var spawnPos: Pos

    @Inject
    lateinit var navigationFunction: NavigationFunction

    @Named("titanNode")
    @Inject
    lateinit var eventNode: EventNode<Event>

    @Inject
    lateinit var worldFunction: WorldFunction

    private val simulationDistance = CachedPacket(UpdateSimulationDistancePacket(2))

    private fun playerLoginListener(event: AsyncPlayerConfigurationEvent) {
        event.setSpawningInstance(worldFunction.provideLobbyWorld())
        event.player.respawnPoint = spawnPos
        event.player.inventory.addInventoryCondition(Cancelable::cancelClick)
    }

    private fun playerSpawnListener(event: PlayerSpawnEvent) {
        navigationFunction.setItems(event.player)
        event.player.teleport(spawnPos)
        event.player.playerConnection.sendPacket(simulationDistance)
    }

    override fun initialize() {
        eventNode.addListener(
            AsyncPlayerConfigurationEvent::class.java,
            this::playerLoginListener
        )
        eventNode.addListener(
            PlayerSpawnEvent::class.java,
            this::playerSpawnListener
        )
    }

    override fun terminate() {
    }

}