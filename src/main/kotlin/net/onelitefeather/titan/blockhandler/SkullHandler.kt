package net.onelitefeather.titan.blockhandler

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import net.minestom.server.utils.NamespaceID

class SkullHandler : BlockHandler {
    override fun getNamespaceId(): NamespaceID  = NamespaceID.from(Key.key("minecraft:skull"))
    private val skullTags = listOf(
        Tag.String("ExtraType"),
        Tag.NBT("SkullOwner"))

    override fun getBlockEntityTags(): MutableCollection<Tag<*>> {
        return skullTags.toMutableList()
    }
}