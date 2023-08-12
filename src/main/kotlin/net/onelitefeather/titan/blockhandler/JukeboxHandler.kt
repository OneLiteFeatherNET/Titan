package net.onelitefeather.titan.blockhandler

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import net.minestom.server.utils.NamespaceID

class JukeboxHandler : BlockHandler {
    override fun getNamespaceId(): NamespaceID = NamespaceID.from(Key.key("minecraft:jukebox"))

    private val jukeTags = listOf(
        Tag.Boolean("IsPlaying"),
        Tag.ItemStack("RecordItem"),
        Tag.Long("RecordStartTick"),
        Tag.Long("TickCount"))

    override fun getBlockEntityTags(): MutableCollection<Tag<*>> {
        return jukeTags.toMutableList()
    }
}