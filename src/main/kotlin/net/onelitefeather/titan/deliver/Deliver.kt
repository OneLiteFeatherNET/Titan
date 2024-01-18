package net.onelitefeather.titan.deliver

import net.minestom.server.entity.Player

interface Deliver {

    fun sendPlayer(player: Player, group: String)

}