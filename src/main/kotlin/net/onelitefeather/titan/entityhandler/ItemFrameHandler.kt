package net.onelitefeather.titan.entityhandler

import net.kyori.adventure.util.Codec
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.entity.metadata.other.ItemFrameMeta
import net.minestom.server.item.ItemStack
import net.minestom.server.utils.NamespaceID
import net.onelitefeather.titan.entity.EntityDecodeException
import net.onelitefeather.titan.entity.EntityEncodeException
import net.onelitefeather.titan.entity.EntityHandler
import net.onelitefeather.titan.helper.EntityLoadingHelper
import org.jglrxavpok.hephaistos.nbt.NBTCompound
import java.util.*

class ItemFrameHandler :
    EntityHandler<Entity, EntityDecodeException, EntityEncodeException> {
    override fun getNamespaceId(): NamespaceID {
        return NamespaceID.from("item_frame")
    }

    override fun entityCodec(): Codec<Entity, NBTCompound, EntityDecodeException, EntityEncodeException> {
        return Codec.codec(DecodeItemFrameEntity(), EncodeItemFrameEntity())
    }

    override fun posCodec(): Codec<Point, NBTCompound, EntityDecodeException, EntityEncodeException> {
        return Codec.codec(DecodeItemFramePos(), EncodeItemFramePos())
    }

    class DecodeItemFrameEntity :
        Codec.Decoder<Entity, NBTCompound, EntityDecodeException> {
        override fun decode(encoded: NBTCompound): Entity {
            val uuid = encoded.getIntArray("UUID")
            val itemFrame = LivingEntity(
                EntityType.ITEM_FRAME,
                UUID(uuid!![0].toLong(), uuid[1].toLong())
            )
            EntityLoadingHelper.genericNbtCompound(itemFrame, encoded)
            EntityLoadingHelper.genericMobNbtCompound(itemFrame, encoded)
            itemFrame.editEntityMeta(ItemFrameMeta::class.java) { meta ->
                meta.isInvisible = encoded.getBoolean("Invisible") ?: false
                encoded.getCompound("Item")?.let {
                    meta.item = ItemStack.fromItemNBT(it)
                }

            }
            return itemFrame
        }
    }



    class EncodeItemFrameEntity :
        Codec.Encoder<Entity, NBTCompound, EntityEncodeException> {
        override fun encode(decoded: Entity): NBTCompound {
            return NBTCompound()
        }
    }

    class DecodeItemFramePos :
        Codec.Decoder<Point, NBTCompound, EntityDecodeException> {
        override fun decode(encoded: NBTCompound): Point {
            return EntityLoadingHelper.nbtToPos(encoded)
        }
    }

    class EncodeItemFramePos :
        Codec.Encoder<Point, NBTCompound, EntityEncodeException> {
        override fun encode(decoded: Point): NBTCompound {
            return NBTCompound()
        }
    }
}