package net.onelitefeather.titan.entityhandler

import net.kyori.adventure.util.Codec
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.metadata.water.fish.TropicalFishMeta
import net.minestom.server.utils.NamespaceID
import net.onelitefeather.titan.entity.EntityDecodeException
import net.onelitefeather.titan.entity.EntityEncodeException
import net.onelitefeather.titan.entity.EntityHandler
import net.onelitefeather.titan.helper.EntityLoadingHelper
import org.jglrxavpok.hephaistos.nbt.NBTCompound
import java.util.*

class TropicalFishHandler :
    EntityHandler<Entity, EntityDecodeException, EntityEncodeException> {
    override fun getNamespaceId(): NamespaceID {
        return NamespaceID.from("tropical_fish")
    }

    override fun entityCodec(): Codec<Entity, NBTCompound, EntityDecodeException, EntityEncodeException> {
        return Codec.codec(DecodeTropicalFishEntity(), EncodeTropicalFishEntity())
    }

    override fun posCodec(): Codec<Point, NBTCompound, EntityDecodeException, EntityEncodeException> {
        return Codec.codec(DecodeTropicalFishPos(), EncodeTropicalFishPos())
    }

    class DecodeTropicalFishEntity :
        Codec.Decoder<Entity, NBTCompound, EntityDecodeException> {
        override fun decode(encoded: NBTCompound): Entity {
            val uuid = encoded.getIntArray("UUID")
            val itemFrame = LivingEntity(
                EntityType.TROPICAL_FISH,
                UUID(uuid!![0].toLong(), uuid[1].toLong())
            )
            EntityLoadingHelper.genericNbtCompound(itemFrame, encoded)
            EntityLoadingHelper.genericMobNbtCompound(itemFrame, encoded)
            itemFrame.editEntityMeta(TropicalFishMeta::class.java) { meta ->
                meta.isInvisible = encoded.getBoolean("Invisible") ?: false
            }
            return itemFrame
        }
    }



    class EncodeTropicalFishEntity :
        Codec.Encoder<Entity, NBTCompound, EntityEncodeException> {
        override fun encode(decoded: Entity): NBTCompound {
            return NBTCompound()
        }
    }

    class DecodeTropicalFishPos :
        Codec.Decoder<Point, NBTCompound, EntityDecodeException> {
        override fun decode(encoded: NBTCompound): Point {
            return EntityLoadingHelper.nbtToPos(encoded)
        }
    }

    class EncodeTropicalFishPos :
        Codec.Encoder<Point, NBTCompound, EntityEncodeException> {
        override fun encode(decoded: Point): NBTCompound {
            return NBTCompound()
        }
    }
}