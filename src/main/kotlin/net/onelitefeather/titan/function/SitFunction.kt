package net.onelitefeather.titan.function

import com.google.inject.Inject
import com.google.inject.name.Named
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.EventListener
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerPacketEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.client.play.ClientSteerVehiclePacket
import net.onelitefeather.titan.event.EntityDismountEvent
import java.util.*

class SitFunction : TitanFunction {

    private val offsetY: Double = 0.25

    @Named("titanNode")
    @Inject
    lateinit var eventNode: EventNode<Event>

    private var playerSitsMap: MutableMap<UUID, Entity> = mutableMapOf()
    private var playerExitLocations: MutableMap<UUID, Pos> = mutableMapOf()

    private fun onDisconnect(playerDisconnectEvent: PlayerDisconnectEvent) {
        removePlayer(playerDisconnectEvent.player, true)
    }

    private fun onDismount(entityDismountEvent: EntityDismountEvent) {
        if (entityDismountEvent.rider is Player && isSitting(entityDismountEvent.rider)) {
            removePlayer(entityDismountEvent.rider, true)
        }
    }

    private fun onPacket(event: PlayerPacketEvent) {
        val packet = event.packet
        if (packet is ClientSteerVehiclePacket) {
            val ridingEntity = event.player.vehicle
            if (packet.flags == 2.toByte() && ridingEntity != null) {
                val entityDismount =
                    EntityDismountEvent(ridingEntity, event.player)
                EventDispatcher.call(entityDismount)
            }
        }
    }

    private fun onInteract(playerBlockInteractEvent: PlayerBlockInteractEvent) {
        if (playerBlockInteractEvent.block.namespace() == Block.SPRUCE_STAIRS.namespace()) {
            sitPlayer(
                playerBlockInteractEvent.player,
                playerBlockInteractEvent.blockPosition
            )
        }
    }

    private fun sitPlayer(player: Player, sitLocation: Point) {
        val instance = player.instance ?: return
        val playerLocation = player.position
        val arrow = Entity(EntityType.ARROW)
        arrow.setInstance(instance, sitLocation.add(0.5, offsetY, 0.5))
        arrow.isSilent = true
        arrow.isInvisible = true

        playerExitLocations[player.uuid] = playerLocation
        arrow.addPassenger(player)

        playerSitsMap[player.uuid]?.remove()
        playerSitsMap[player.uuid] = arrow
    }

    private fun isSitting(player: Player): Boolean {
        return playerSitsMap.containsKey(player.uuid)
    }

    private fun removePlayer(player: Player, remove: Boolean) {
        val arrow = playerSitsMap.remove(player.uuid)

        if (arrow != null) {
            arrow.removePassenger(player)
            if (remove) arrow.remove()

            val exitLocation = playerExitLocations.remove(player.uuid)
            if (exitLocation != null) {
                player.teleport(exitLocation)
            }
        }
    }

    override fun initialize() {
        eventNode.addListener(
            EventListener.of(
                PlayerBlockInteractEvent::class.java,
                this::onInteract
            )
        )
        eventNode.addListener(
            EventListener.of(
                PlayerPacketEvent::class.java,
                this::onPacket
            )
        )
        eventNode.addListener(
            EventListener.of(
                EntityDismountEvent::class.java,
                this::onDismount
            )
        )
        eventNode.addListener(
            EventListener.of(
                PlayerDisconnectEvent::class.java,
                this::onDisconnect
            )
        )
    }

    override fun terminate() {
    }
}