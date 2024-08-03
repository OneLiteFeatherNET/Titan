package net.onelitefeather.titan

import java.nio.file.Path
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerBlockBreakEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.event.player.PlayerDeathEvent
import net.minestom.server.event.player.PlayerRespawnEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.player.PlayerSwapItemEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.inventory.click.ClickType
import net.minestom.server.inventory.condition.InventoryConditionResult
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.utils.NamespaceID
import net.minestom.server.utils.chunk.ChunkUtils
import net.onelitefeather.titan.blockhandler.BannerHandler
import net.onelitefeather.titan.blockhandler.BeaconHandler
import net.onelitefeather.titan.blockhandler.CandleHandler
import net.onelitefeather.titan.blockhandler.JukeboxHandler
import net.onelitefeather.titan.blockhandler.SignHandler
import net.onelitefeather.titan.blockhandler.SkullHandler
import net.onelitefeather.titan.blockhandler.banner.BedHandler
import net.onelitefeather.titan.commands.EndCommand
import net.onelitefeather.titan.deliver.CloudNetDeliver
import net.onelitefeather.titan.deliver.Deliver
import net.onelitefeather.titan.deliver.DummyDeliver
import net.onelitefeather.titan.feature.ElytraFeature
import net.onelitefeather.titan.feature.NavigatorFeature
import net.onelitefeather.titan.feature.SitFeature
import net.onelitefeather.titan.feature.TickelFeature
import net.onelitefeather.titan.helper.Cancelable
import net.onelitefeather.titan.utils.TitanFeatures

class TitanExtension : Extension() {

    private val lobbyWorld: InstanceContainer = MinecraftServer.getInstanceManager().createInstanceContainer()
    private val extensionEventNode: EventNode<Event>
    private val sitEventNode: EventNode<Event>
    private val elytraEventNode: EventNode<Event>
    private val tickleEventNode: EventNode<Event>
    private val navigatorEventNode: EventNode<Event>
    private val worldPath: Path by lazy(TitanExtension::createWorldPath)
    private val spawnLocation: Pos by lazy(TitanExtension::createSpawn)


    init {
        MinecraftServer.getBlockManager().registerHandler(NamespaceID.from(Key.key("minecraft:bed")), ::BedHandler)
        MinecraftServer.getBlockManager().registerHandler(NamespaceID.from(Key.key("minecraft:jukebox")), ::JukeboxHandler)
        MinecraftServer.getBlockManager().registerHandler(NamespaceID.from(Key.key("minecraft:beacon")), ::BeaconHandler)
        MinecraftServer.getBlockManager().registerHandler(NamespaceID.from(Key.key("minecraft:sign")), ::SignHandler)
        MinecraftServer.getBlockManager().registerHandler(NamespaceID.from(Key.key("minecraft:banner")), ::BannerHandler)
        MinecraftServer.getBlockManager().registerHandler(NamespaceID.from(Key.key("minecraft:skull")), ::SkullHandler)
        MinecraftServer.getBlockManager().registerHandler(NamespaceID.from(Key.key("minecraft:candle")), ::CandleHandler)
        lobbyWorld.chunkLoader = AnvilLoader(worldPath)

        extensionEventNode = EventNode.all("TitanExtension")
        sitEventNode = EventNode.all("SitFeature")
        elytraEventNode = EventNode.all("ElytraRaceFeature")
        tickleEventNode = EventNode.all("TickelFeature")
        navigatorEventNode = EventNode.all("NavigatorFeature")
    }

    override fun initialize() {
        MinecraftServer.getGlobalEventHandler().addChild(extensionEventNode)
        extensionEventNode.addListener(AsyncPlayerConfigurationEvent::class.java, this::playerLoginListener)
        extensionEventNode.addListener(PlayerSpawnEvent::class.java, this::playerSpawnListener)
        extensionEventNode.addListener(PlayerDeathEvent::class.java, this::deathListener)

        extensionEventNode.addListener(PickupItemEvent::class.java, Cancelable::cancel)
        extensionEventNode.addListener(PlayerBlockBreakEvent::class.java, Cancelable::cancel)
        extensionEventNode.addListener(PlayerBlockPlaceEvent::class.java, Cancelable::cancel)
        extensionEventNode.addListener(PlayerSwapItemEvent::class.java, Cancelable::cancel)
        extensionEventNode.addListener(PlayerRespawnEvent::class.java, this::respawnListener)
        extensionEventNode.addListener(ItemDropEvent::class.java, Cancelable::cancel)
        MinecraftServer.getGlobalEventHandler().addChild(sitEventNode)
        MinecraftServer.getGlobalEventHandler().addChild(elytraEventNode)
        MinecraftServer.getGlobalEventHandler().addChild(tickleEventNode)
        MinecraftServer.getGlobalEventHandler().addChild(navigatorEventNode)
        SitFeature(0.25, sitEventNode)
        ElytraFeature(elytraEventNode)
        TickelFeature(tickleEventNode)
        NavigatorFeature(navigatorEventNode)
        MinecraftServer.getCommandManager().register(EndCommand())
        if (!ChunkUtils.isLoaded(lobbyWorld, spawnLocation)) {
            lobbyWorld.loadChunk(spawnLocation).whenComplete { _, throwable ->
                if (throwable != null) {
                    throw RuntimeException("Unable to load spawn chunk", throwable)
                }
            }
        }
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

    private fun playerLoginListener(event: AsyncPlayerConfigurationEvent) {
        event.setSpawningInstance(lobbyWorld)
        event.player.respawnPoint = spawnLocation
        event.player.inventory.addInventoryCondition(Cancelable::cancelClick)
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

    companion object {
        val elytra = ItemStack.builder(Material.ELYTRA)
            .displayName(Component.text("Elytra", NamedTextColor.DARK_PURPLE))
            .meta {
                it.unbreakable(true)
            }
            .build()

        val teleporter =
            ItemStack.builder(Material.FEATHER)
                .displayName(
                    Component.text("Navigator", NamedTextColor.AQUA)
                )
                .build()

        private fun createWorldPath(): Path {
            if (TitanFeatures.HALLOWEEN.isActive) {
                return Path.of("worlds","halloween")
            }
            if (TitanFeatures.WINTER.isActive) {
                return Path.of("worlds","winter")
            }
            return Path.of("worlds","world")
        }

        private fun createSpawn(): Pos {
            return Pos(0.5, 65.0, 0.5, -180f, 0f)
        }

        fun createDeliverAPI(): Deliver {
            try {
                Class.forName("eu.cloudnetservice.wrapper.Main")
                return CloudNetDeliver()
            } catch (e: ClassNotFoundException) {
                DummyDeliver()
            }
            return DummyDeliver()
        }

    }

}