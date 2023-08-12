package net.onelitefeather.titan.blockhandler

import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import net.minestom.server.tag.TagReadable
import net.minestom.server.tag.TagSerializer
import net.minestom.server.tag.TagWritable
import net.minestom.server.utils.NamespaceID
import net.onelitefeather.titan.blockhandler.banner.BannerPattern
import org.jetbrains.annotations.NotNull


class BannerHandler : BlockHandler {
    override fun getNamespaceId(): NamespaceID = NamespaceID.from("minecraft:banner")

    private val patternTag: Tag<MutableList<BannerPattern>> = Tag.Structure("Patterns",

        object : TagSerializer<BannerPattern> {
            override fun read(@NotNull reader: TagReadable): BannerPattern {
                val color: Int = reader.getTag(Tag.Integer("Color"))
                val pattern: String = reader.getTag(Tag.String("Pattern"))
                return BannerPattern(color, pattern)
            }

            override fun write(@NotNull writer: TagWritable, @NotNull value: BannerPattern) {
                writer.setTag(Tag.Integer("Color"), value.color)
                writer.setTag(Tag.String("Pattern"), value.pattern)
            }
        }).list().defaultValue(listOf())

    private val tagList = listOf(
        Tag.String("CustomName"),
        patternTag
    )

    override fun getBlockEntityTags(): MutableCollection<Tag<*>> {
        return tagList.toMutableList()
    }
}