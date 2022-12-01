package net.onelitefeather.titan.service

import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.SetCooldownPacket

class Cooldown {

    private val cooldowns = mutableMapOf<Player, Long>()

    fun setCooldown(player: Player, item: Int, cooldown: Int, ticks: Int) {
        cooldowns[player] = (System.currentTimeMillis() + cooldown)
        val packet = SetCooldownPacket(item, ticks)
        player.playerConnection.sendPacket(packet)
    }

    fun remove(player: Player) {
        cooldowns.remove(player)
    }

    fun hasCooldown(player: Player): Boolean {
        val time = cooldowns[player] ?: return false
        if (System.currentTimeMillis() >= time) {
            remove(player)
            return false
        }
        return true
    }


    private fun getCooldownPacket(itemId: Int, cooldown: Int): SetCooldownPacket {
        return SetCooldownPacket(itemId, cooldown)
    }

}