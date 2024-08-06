package net.onelitefeather.titan.entityhandler

import net.kyori.adventure.util.Codec
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.utils.NamespaceID
import net.onelitefeather.titan.entity.EntityDecodeException
import net.onelitefeather.titan.entity.EntityEncodeException
import net.onelitefeather.titan.entity.EntityHandler
import net.onelitefeather.titan.entity.ReusableLivingEntity
import net.onelitefeather.titan.helper.EntityLoadingHelper
import org.jglrxavpok.hephaistos.nbt.NBTCompound
import java.util.*

class ArmorStandHandler :
    EntityHandler<Entity, EntityDecodeException, EntityEncodeException> {
    override fun getNamespaceId(): NamespaceID {
        return NamespaceID.from("armor_stand")
    }

    override fun entityCodec(): Codec<Entity, NBTCompound, EntityDecodeException, EntityEncodeException> {
        return Codec.codec(DecodeArmorStandEntity(), EncodeArmorStandEntity())
    }

    override fun posCodec(): Codec<Point, NBTCompound, EntityDecodeException, EntityEncodeException> {
        return Codec.codec(DecodeArmorStandPos(), EncodeArmorStandPos())
    }

    class DecodeArmorStandEntity :
        Codec.Decoder<Entity, NBTCompound, EntityDecodeException> {
        override fun decode(encoded: NBTCompound): Entity {
            val uuid = encoded.getIntArray("UUID")
            val armorStand =
                ReusableLivingEntity(
                    EntityType.ARMOR_STAND,
                    UUID(uuid!![0].toLong(), uuid[1].toLong())
                )
            EntityLoadingHelper.genericNbtCompound(armorStand, encoded)
            EntityLoadingHelper.genericMobNbtCompound(armorStand, encoded)
            armorStand.editEntityMeta(ArmorStandMeta::class.java) {
                it.isSmall = encoded.getBoolean("Small") ?: false
                it.isHasNoBasePlate = encoded.getBoolean("NoBasePlate") ?: false
                it.isMarker = encoded.getBoolean("Marker") ?: true
                it.isInvisible = encoded.getBoolean("Invisible") ?: false
                it.isHasArms = encoded.getBoolean("ShowArms") ?: false
                val poseNBT = encoded.getCompound("Pose") ?: return@editEntityMeta
                it.headRotation = EntityLoadingHelper.getVecByFloatArray(poseNBT, "Head")
                it.bodyRotation = EntityLoadingHelper.getVecByFloatArray(poseNBT, "Body")
                it.leftArmRotation = EntityLoadingHelper.getVecByFloatArray(poseNBT, "LeftArm")
                it.leftLegRotation = EntityLoadingHelper.getVecByFloatArray(poseNBT, "LeftLeg")
                it.rightArmRotation = EntityLoadingHelper.getVecByFloatArray(poseNBT, "RightArm")
                it.rightLegRotation = EntityLoadingHelper.getVecByFloatArray(poseNBT, "RightLeg")
            }
            return armorStand
        }
    }



    class EncodeArmorStandEntity :
        Codec.Encoder<Entity, NBTCompound, EntityEncodeException> {
        override fun encode(decoded: Entity): NBTCompound {
            return NBTCompound()
        }
    }

    class DecodeArmorStandPos :
        Codec.Decoder<Point, NBTCompound, EntityDecodeException> {
        override fun decode(encoded: NBTCompound): Point {
            return EntityLoadingHelper.nbtToPos(encoded)
        }
    }

    class EncodeArmorStandPos :
        Codec.Encoder<Point, NBTCompound, EntityEncodeException> {
        override fun encode(decoded: Point): NBTCompound {
            return NBTCompound()
        }
    }
}