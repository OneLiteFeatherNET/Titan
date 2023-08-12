package net.onelitefeather.titan.blockhandler

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import net.minestom.server.utils.NamespaceID

class CandleHandler : BlockHandler {
    override fun getNamespaceId(): NamespaceID  = NamespaceID.from(Key.key("minecraft:candle"))
    private val signTags = listOf(
        Tag.Integer("candles").defaultValue(1),
        Tag.Boolean("lit").defaultValue(false),
        Tag.Boolean("waterlogged").defaultValue(false))

    override fun getBlockEntityTags(): MutableCollection<Tag<*>> {
        return signTags.toMutableList()
    }
}