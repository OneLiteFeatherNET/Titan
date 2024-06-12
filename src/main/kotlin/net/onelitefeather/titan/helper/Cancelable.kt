package net.onelitefeather.titan.helper

import net.minestom.server.entity.Player
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.inventory.click.ClickType
import net.minestom.server.inventory.condition.InventoryConditionResult

class Cancelable {
    companion object {
        fun cancel(event: CancellableEvent) {
            event.isCancelled = true
        }

        fun cancelClick(
            player: Player,
            i: Int,
            clickType: ClickType,
            inventoryConditionResult: InventoryConditionResult
        ) {
            inventoryConditionResult.isCancel = true
        }
    }
}