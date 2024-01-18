package net.onelitefeather.titan.deliver

import eu.cloudnetservice.driver.inject.InjectionLayer
import eu.cloudnetservice.driver.registry.ServiceRegistry
import eu.cloudnetservice.modules.bridge.player.PlayerManager
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType
import net.minestom.server.entity.Player

class CloudNetDeliver : Deliver {

    private val playerManager: PlayerManager

    init {
        val serviceRegistry = InjectionLayer.ext().instance(ServiceRegistry::class.java)
        playerManager = serviceRegistry.firstProvider(PlayerManager::class.java)
    }

    override fun sendPlayer(player: Player, group: String) {
        playerManager.playerExecutor(player.uuid).connectToTask(group, ServerSelectorType.RANDOM)
    }
}