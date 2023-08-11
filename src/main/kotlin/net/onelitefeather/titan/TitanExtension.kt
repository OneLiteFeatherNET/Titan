package net.onelitefeather.titan

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.InstanceContainer
import net.onelitefeather.titan.feature.ElytraFeature
import net.onelitefeather.titan.feature.SitFeature
import net.onelitefeather.titan.feature.TickelFeature
import net.onelitefeather.titan.service.elytra
import net.onelitefeather.titan.service.teleporter
import java.nio.file.Path

class TitanExtension : Extension() {

    private val lobbyWorld: InstanceContainer = MinecraftServer.getInstanceManager().createInstanceContainer()
    private val extensionEventNode: EventNode<Event>
    private val sitEventNode: EventNode<Event>
    private val elytraEventNode: EventNode<Event>
    private val tickelEventNode: EventNode<Event>
    private val worldPath = Path.of(WORLD_FOLDER_NAME)

    private val spawnLocation: Pos by lazy {
        Pos(0.5, 65.0, 0.5, -180f, 0f)
    }

    init {
        lobbyWorld.chunkLoader = AnvilLoader(worldPath)
        extensionEventNode = EventNode.all("TitanExtension")
        sitEventNode = EventNode.all("SitFeature")
        elytraEventNode = EventNode.all("ElytraRaceFeature")
        tickelEventNode = EventNode.all("TickelFeature")
    }

    override fun initialize() {

        MinecraftServer.getGlobalEventHandler().addChild(extensionEventNode)
        extensionEventNode.addListener(PlayerLoginEvent::class.java, this::playerLoginListener)
        extensionEventNode.addListener(PlayerSpawnEvent::class.java, this::playerSpawnListener)
        extensionEventNode.addListener(PlayerDeathEvent::class.java, this::deathListener)

        extensionEventNode.addListener(PickupItemEvent::class.java, this::cancelListener)
        extensionEventNode.addListener(PlayerBlockBreakEvent::class.java, this::cancelListener)
        extensionEventNode.addListener(PlayerBlockPlaceEvent::class.java, this::cancelListener)
        extensionEventNode.addListener(PlayerSwapItemEvent::class.java, this::cancelListener)
        extensionEventNode.addListener(PlayerRespawnEvent::class.java, this::respawnListener)
        MinecraftServer.getGlobalEventHandler().addChild(sitEventNode)
        MinecraftServer.getGlobalEventHandler().addChild(elytraEventNode)
        MinecraftServer.getGlobalEventHandler().addChild(tickelEventNode)
        SitFeature(0.25, sitEventNode)
        ElytraFeature(elytraEventNode)
        TickelFeature(tickelEventNode)
    }

    override fun terminate() {
    }

    private fun deathListener(event: PlayerDeathEvent) {
        event.deathText = Component.empty()
        event.player.respawn()
    }

    private fun respawnListener(event: PlayerRespawnEvent) {
        setItems(event.player)
    }

    private fun cancelListener(event: CancellableEvent) {
        event.isCancelled = true
    }

    private fun playerLoginListener(event: PlayerLoginEvent) {
        event.setSpawningInstance(lobbyWorld)
        event.player.respawnPoint = spawnLocation
    }

    private fun playerSpawnListener(event: PlayerSpawnEvent) {
        setItems(event.player)
        event.player.teleport(spawnLocation)
    }

    private fun setItems(player: Player) {
        player.inventory.clear()
        player.inventory.setItemStack(4, teleporter)
        player.inventory.chestplate = elytra
    }


}