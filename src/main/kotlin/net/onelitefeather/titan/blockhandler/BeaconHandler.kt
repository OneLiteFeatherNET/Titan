package net.onelitefeather.titan.blockhandler

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import net.minestom.server.utils.NamespaceID

class BeaconHandler : BlockHandler {
    override fun getNamespaceId(): NamespaceID = NamespaceID.from(Key.key("minecraft:beacon"))

    private val beaconTags = listOf(
        Tag.String("CustomName"),
        Tag.String("Lock"),
        Tag.Integer("Levels"),
        Tag.Integer("Primary"),
        Tag.Integer("Secondary"))

    override fun getBlockEntityTags(): MutableCollection<Tag<*>> {
        return beaconTags.toMutableList()
    }
}