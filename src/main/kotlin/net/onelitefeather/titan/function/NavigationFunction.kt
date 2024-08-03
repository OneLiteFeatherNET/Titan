package net.onelitefeather.titan.function

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.google.inject.Inject
import com.google.inject.name.Named
import de.icevizion.aves.inventory.InventoryLayout
import de.icevizion.aves.inventory.PersonalInventoryBuilder
import de.icevizion.aves.inventory.util.LayoutCalculator
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.click.ClickType
import net.minestom.server.inventory.condition.InventoryConditionResult
import net.minestom.server.item.ItemStack
import net.onelitefeather.titan.deliver.Deliver
import net.onelitefeather.titan.utils.TitanFeatures
import org.togglz.core.user.thread.ThreadLocalUserProvider
import java.time.Duration
import java.util.*


class NavigationFunction: TitanFunction {

    private val inventoryName = "<yellow>Navigator"

    @Named("navigatorBlankItemStack")
    @Inject
    lateinit var navigatorBlankItemStack: ItemStack

    @Named("navigatorElytraItemStack")
    @Inject
    lateinit var navigatorElytraItemStack: ItemStack

    @Named("navigatorSlenderItemStack")
    @Inject
    lateinit var navigatorSlenderItemStack: ItemStack

    @Named("navigatorSurvivalItemStack")
    @Inject
    lateinit var navigatorSurvivalItemStack: ItemStack

    @Named("navigatorCreativeItemStack")
    @Inject
    lateinit var navigatorCreativeItemStack: ItemStack

    @Named("playerTeleporter")
    @Inject
    lateinit var teleporter: ItemStack

    @Named("playerElytra")
    @Inject
    lateinit var  elytra: ItemStack

    @Inject
    lateinit var deliver: Deliver

    @Named("titanNode")
    @Inject
    lateinit var eventNode: EventNode<Event>

    private var inventoryBuilderLoadingCache: LoadingCache<UUID, PersonalInventoryBuilder> = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .refreshAfterWrite(Duration.ofMinutes(1))
        .build { key: UUID -> createPersonalInventoryBuilder(MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(key)) }

    private fun createPersonalInventoryBuilder(player: Player?): PersonalInventoryBuilder? {
        player ?: return null
        val inventoryBuilder = PersonalInventoryBuilder(MiniMessage.miniMessage().deserialize(inventoryName), InventoryType.CHEST_1_ROW, player)
        inventoryBuilder.setLayout(InventoryLayout.fromType(InventoryType.CHEST_1_ROW))
        inventoryBuilder.setDataLayoutFunction {
            val layout = it ?: InventoryLayout.fromType(InventoryType.CHEST_1_ROW)
            layout.setNonClickItems(LayoutCalculator.fillRow(InventoryType.CHEST_1_ROW), navigatorBlankItemStack)
            layout.setItem(0, navigatorElytraItemStack, this::clickElytra)
            ThreadLocalUserProvider.bind(player.toFeatureUser())
            if (TitanFeatures.SLENDER.isActive) {
                layout.setItem(3, navigatorSlenderItemStack, this::clickSlender)
            }
            layout.setItem(4, navigatorSurvivalItemStack, this::clickSurvival)
            if (TitanFeatures.MANIS.isActive) {
                layout.setItem(5, navigatorSlenderItemStack, this::clickSurvival)
            }
            layout.setItem(8, navigatorCreativeItemStack, this::clickCreative)
            ThreadLocalUserProvider.release()
            layout
        }
        inventoryBuilder.register()
        return inventoryBuilder
    }

    fun setItems(player: Player) {
        player.inventory.clear()
        player.inventory.setItemStack(4, teleporter)
        player.inventory.chestplate = elytra
    }

    private fun clickElytra(player: Player, slot: Int, type: ClickType, conditionResult: InventoryConditionResult) {
        conditionResult.isCancel = true
        deliver.sendPlayer(player, "ElytraRace")
    }

    private fun clickSurvival(player: Player, slot: Int, type: ClickType, conditionResult: InventoryConditionResult) {
        conditionResult.isCancel = true
        deliver.sendPlayer(player, "120Survival")
    }

    private fun clickSlender(player: Player, slot: Int, type: ClickType, conditionResult: InventoryConditionResult) {
        conditionResult.isCancel = true
        deliver.sendPlayer(player, "Slender")
    }

    private fun clickCreative(player: Player, slot: Int, type: ClickType, conditionResult: InventoryConditionResult) {
        conditionResult.isCancel = true
        deliver.sendPlayer(player, "MemberBuild")
    }

    private fun playerUseItemEvent(event: PlayerUseItemEvent) {
        val item = event.itemStack
        if (item == teleporter) {
            val personalInventoryBuilder =
                inventoryBuilderLoadingCache[event.player.uuid]
            personalInventoryBuilder.invalidateDataLayout()
            personalInventoryBuilder.open()
        }
    }

    override fun initialize() {
        eventNode.addListener(
            PlayerUseItemEvent::class.java,
            this::playerUseItemEvent
        )
    }

    override fun terminate() {
    }


}