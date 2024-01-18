package net.onelitefeather.titan.feature

import de.icevizion.aves.inventory.GlobalInventoryBuilder
import de.icevizion.aves.inventory.InventoryLayout
import de.icevizion.aves.inventory.util.LayoutCalculator
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.click.ClickType
import net.minestom.server.inventory.condition.InventoryConditionResult
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.onelitefeather.titan.*

class NavigatorFeature(
    private val titanExtension: TitanExtension, eventNode: EventNode<Event>
) {

    val SLENDER_NAME = "<!i><gradient:#616161:#e80000c>Slender</gradient>"
    val INVENTORY_NAME = "<yellow>Navigator"
    val ELYTRA_NAME = "<!i><gradient:#fcba03:#03fc8c>ElytraRace</gradient>"
    val SURVIVAL_NAME = "<!i><green>Survival"
    val CREATIVE_NAME = "<!i><rainbow>Creative</rainbow>"


    private val globalInventory by lazy {
        GlobalInventoryBuilder(MiniMessage.miniMessage().deserialize(INVENTORY_NAME), InventoryType.CHEST_1_ROW)
    }

    private val globalLayout by lazy {
        InventoryLayout(InventoryType.CHEST_1_ROW)
    }
    private val blankItemStack by lazy {
        ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).displayName(Component.empty()).build()
    }

    private val elytraItemStack by lazy {
        ItemStack.builder(Material.ELYTRA).displayName(MiniMessage.miniMessage().deserialize(ELYTRA_NAME)).build()
    }

    private val slenderItemStack by lazy {
        ItemStack.builder(Material.ENDERMAN_SPAWN_EGG).displayName(MiniMessage.miniMessage().deserialize(SLENDER_NAME)).build()
    }

    private val survivalItemStack by lazy {
        ItemStack.builder(Material.GRASS_BLOCK).displayName(MiniMessage.miniMessage().deserialize(SURVIVAL_NAME)).build()
    }

    private val creativeItemStack by lazy {
        ItemStack.builder(Material.WOODEN_AXE).displayName(MiniMessage.miniMessage().deserialize(CREATIVE_NAME)).build()
    }



    init {
        globalLayout.setNonClickItems(LayoutCalculator.fillRow(InventoryType.CHEST_1_ROW), blankItemStack)
        globalLayout.setItem(0, elytraItemStack, this::clickElytra)
        globalLayout.setItem(3, survivalItemStack, this::clickSurvival)
        globalLayout.setItem(5, slenderItemStack, this::clickSlender)
        globalLayout.setItem(8, creativeItemStack, this::clickCreative)
        globalInventory.setLayout(globalLayout)
        globalInventory.register()
        eventNode.addListener(PlayerUseItemEvent::class.java, this::playerUseItemEvent)
    }

    private fun clickElytra(player: Player, type: ClickType, slot: Int, conditionResult: InventoryConditionResult) {
        conditionResult.isCancel = true
        titanExtension.deliver.sendPlayer(player, "ElytraRace")
    }

    private fun clickSurvival(player: Player, type: ClickType, slot: Int, conditionResult: InventoryConditionResult) {
        conditionResult.isCancel = true
        titanExtension.deliver.sendPlayer(player, "120Survival")
    }

    private fun clickSlender(player: Player, type: ClickType, slot: Int, conditionResult: InventoryConditionResult) {
        conditionResult.isCancel = true
        titanExtension.deliver.sendPlayer(player, "Slender")
    }

    private fun clickCreative(player: Player, type: ClickType, slot: Int, conditionResult: InventoryConditionResult) {
        conditionResult.isCancel = true
        titanExtension.deliver.sendPlayer(player, "MemberBuild")
    }

    private fun playerUseItemEvent(event: PlayerUseItemEvent) {
        val item = event.itemStack
        if (item == titanExtension.teleporter) {
            event.player.openInventory(this.globalInventory.inventory)
        }
    }

}