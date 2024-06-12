package net.onelitefeather.titan.blockhandler

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import net.minestom.server.tag.TagReadable
import net.minestom.server.tag.TagSerializer
import net.minestom.server.tag.TagWritable
import net.minestom.server.utils.NamespaceID

class SignHandler : BlockHandler {
    override fun getNamespaceId(): NamespaceID  = NamespaceID.from(Key.key("minecraft:sign"))

    private val signTags = listOf(
        Tag.Byte("is_waxed"),
        Tag.Structure("front_text", SignSerializer()),
        Tag.Structure("back_text", SignSerializer())
        )

    override fun getBlockEntityTags(): MutableCollection<Tag<*>> {
        return signTags.toMutableList()
    }

    class SignSerializer : TagSerializer<SignSide> {
        override fun read(reader: TagReadable): SignSide {
            val hasGlowing = reader.getTag(Tag.Boolean("has_glowing_text"))
            val color = reader.getTag(Tag.String("color"))
            val messages = reader.getTag(Tag.String("messages").list())
            return SignSide(hasGlowing, color, messages)
        }

        override fun write(writer: TagWritable, value: SignSide) {
            writer.setTag(Tag.Boolean("has_glowing_text"), value.glowing)
            writer.setTag(Tag.String("color"), value.color)
            writer.setTag(Tag.String("messages").list(), value.message)
        }

    }

    data class SignSide(
        val glowing: Boolean,
        val color: String,
        val message: List<String>,
    )
}