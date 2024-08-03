package net.onelitefeather.titan.function

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

class ItemFunction : AbstractModule() {
    private val playerElytra = ItemStack.builder(Material.ELYTRA)
        .displayName(Component.text("Elytra", NamedTextColor.DARK_PURPLE))
        .meta {
            it.unbreakable(true)
        }
        .build()

    private val playerTeleporter =
        ItemStack.builder(Material.FEATHER)
            .displayName(
                MiniMessage.miniMessage().deserialize("<!i><aqua>Navigator")
            )
            .build()

    private val playerFirework =
        ItemStack.builder(Material.FIREWORK_ROCKET)
            .displayName(
                Component.text("Firework Rocket")
            )
            .build()

    private val navigatorBlankItemStack =
        ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE)
            .displayName(Component.empty()).build()

    private val navigatorElytraItemStack =
        ItemStack.builder(Material.ELYTRA).displayName(
            MiniMessage.miniMessage().deserialize(
                "<!i><gradient:#fcba03:#03fc8c>ElytraRace</gradient>"
            )
        ).build()

    private val navigatorSlenderItemStack =
        ItemStack.builder(Material.ENDERMAN_SPAWN_EGG).displayName(
            MiniMessage.miniMessage().deserialize(
                "<!i><gradient:#616161:#e80000c>Slender</gradient>"
            )
        ).build()

    private val navigatorSurvivalItemStack =
        ItemStack.builder(Material.GRASS_BLOCK).displayName(
            MiniMessage.miniMessage().deserialize(
                "<!i><green>Survival"
            )
        ).build()

    private val navigatorCreativeItemStack =
        ItemStack.builder(Material.WOODEN_AXE).displayName(
            MiniMessage.miniMessage().deserialize(
                "<!i><rainbow>Creative</rainbow>"
            )
        ).build()

    override fun configure() {
        bind(ItemStack::class.java).annotatedWith(Names.named("playerElytra"))
            .toInstance(playerElytra)
        bind(ItemStack::class.java).annotatedWith(Names.named("playerTeleporter"))
            .toInstance(playerTeleporter)
        bind(ItemStack::class.java).annotatedWith(Names.named("playerFirework"))
            .toInstance(playerFirework)
        bind(ItemStack::class.java).annotatedWith(Names.named("navigatorBlankItemStack"))
            .toInstance(navigatorBlankItemStack)
        bind(ItemStack::class.java).annotatedWith(Names.named("navigatorElytraItemStack"))
            .toInstance(navigatorElytraItemStack)
        bind(ItemStack::class.java).annotatedWith(Names.named("navigatorSurvivalItemStack"))
            .toInstance(navigatorSurvivalItemStack)
        bind(ItemStack::class.java).annotatedWith(Names.named("navigatorCreativeItemStack"))
            .toInstance(navigatorCreativeItemStack)
        bind(ItemStack::class.java).annotatedWith(Names.named("navigatorSlenderItemStack"))
            .toInstance(navigatorSlenderItemStack)
    }


}