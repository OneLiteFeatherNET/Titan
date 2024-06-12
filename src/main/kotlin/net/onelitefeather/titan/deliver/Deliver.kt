package net.onelitefeather.titan.deliver

import net.minestom.server.entity.Player

fun interface Deliver {
    fun sendPlayer(player: Player, group: String)
}