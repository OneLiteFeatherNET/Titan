package net.onelitefeather.titan.service

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance

fun Instance.brodcastMessage(message: Component) {
    players.forEach { it.sendMessage(message) }
}

fun Collection<Player>.brodcastMessage(message: Component) {
    this.forEach { it.sendMessage(message) }
}