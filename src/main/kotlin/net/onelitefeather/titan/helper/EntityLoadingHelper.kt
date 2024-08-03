package net.onelitefeather.titan.helper

import net.kyori.adventure.text.serializer.json.JSONComponentSerializer
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.LivingEntity
import net.minestom.server.item.ItemStack
import net.onelitefeather.titan.entity.InstanceEntityManager
import org.jglrxavpok.hephaistos.nbt.*
import java.util.*

object EntityLoadingHelper {
    fun genericMobNbtCompound(entity: LivingEntity, nbtCompound: NBTCompound) {
        nbtCompound.getList<NBTCompound>("ArmorItems")?.let { nbtCompounds ->
            val boots = nbtCompounds[0]
            val leggings = nbtCompounds[1]
            val chestplate = nbtCompounds[2]
            val helmet = nbtCompounds[3]
            if (!boots.isEmpty()) entity.boots = ItemStack.fromItemNBT(boots)
            if (!leggings.isEmpty()) entity.leggings = ItemStack.fromItemNBT(leggings)
            if (!chestplate.isEmpty()) entity.chestplate = ItemStack.fromItemNBT(chestplate)
            if (!helmet.isEmpty()) entity.helmet = ItemStack.fromItemNBT(helmet)
        }

        nbtCompound.getList<NBTCompound>("HandItems")?.let { nbtCompounds ->
            val mainHand = nbtCompounds[0]
            val offHand = nbtCompounds[1]
            if (!mainHand.isEmpty()) entity.itemInMainHand = ItemStack.fromItemNBT(mainHand)
            if (!offHand.isEmpty()) entity.itemInOffHand = ItemStack.fromItemNBT(offHand)
        }
    }

    fun getVecByFloatArray(compound: NBTCompound, key: String): Vec {
        val headNBT = compound.getList<NBTFloat>(key)
        val x = headNBT?.get(0)?.value ?: .0f
        val y = headNBT?.get(1)?.value ?: .0f
        val z = headNBT?.get(2)?.value ?: .0f
        return Vec(x.toDouble(), y.toDouble(), z.toDouble())
    }

    fun genericNbtCompound(entity: Entity, nbtCompound: NBTCompound) {
        entity.isSilent = nbtCompound.getBoolean("Silent") ?: false
        entity.setNoGravity(nbtCompound.getBoolean("NoGravity") ?: false)
        entity.isOnFire = nbtCompound.getBoolean("HasVisualFire") ?: false
        entity.isGlowing = nbtCompound.getBoolean("Glowing") ?: false
        entity.isCustomNameVisible = nbtCompound.getBoolean("CustomNameVisible") ?: false
        nbtCompound.getString("CustomName")?.let {
            entity.customName = JSONComponentSerializer.json().deserialize(it)
        }
        val passengers = nbtCompound.getList("Passengers") ?: NBTList(NBTType.TAG_Compound)
        passengers.forEach {
            val entityId =
                Objects.requireNonNull<String>(it.getString("id"))
            val entityHandler = InstanceEntityManager.getInstance()
                .getHandlerOrDummy(entityId)
            entity.addPassenger(entityHandler.entityCodec().decode(it))
        }


    }

    fun nbtToPos(compound: NBTCompound): Point {
        val posArray = compound.getList<NBTDouble>("Pos") ?: return Pos.ZERO
        val x = posArray[0].value
        val y = posArray[1].value
        val z = posArray[2].value
        val rotationArray = compound.getList<NBTFloat>("Rotation") ?: return Pos.ZERO
        val yaw = rotationArray[0].value
        val pitch = rotationArray[1].value
        return Pos(x, y, z, yaw, pitch)
    }
}