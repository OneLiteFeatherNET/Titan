package net.onelitefeather.titan.service

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

val firework =
    ItemStack.builder(Material.FIREWORK_ROCKET)
        .displayName(
            Component.text("Firework Rocket")
        )
        .build()

val elytra =
    ItemStack.builder(Material.ELYTRA)
        .displayName(
            Component.text("Elytra", NamedTextColor.DARK_PURPLE)
        )
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


