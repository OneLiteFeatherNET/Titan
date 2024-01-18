package net.onelitefeather.titan.deliver

import net.minestom.server.entity.Player

class DummyDeliver : Deliver {
    override fun sendPlayer(player: Player, group: String) {
        player.sendMessage(group)
    }
}