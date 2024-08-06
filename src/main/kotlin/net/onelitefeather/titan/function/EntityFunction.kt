package net.onelitefeather.titan.function

import net.kyori.adventure.key.Key
import net.minestom.server.utils.NamespaceID
import net.onelitefeather.titan.entity.InstanceEntityManager
import net.onelitefeather.titan.entityhandler.ArmorStandHandler
import net.onelitefeather.titan.entityhandler.ItemFrameHandler
import net.onelitefeather.titan.entityhandler.TropicalFishHandler

class EntityFunction : TitanFunction {
    override fun initialize() {
        InstanceEntityManager.getInstance().registerHandler(
            NamespaceID.from(Key.key("minecraft:armor_stand")), ::ArmorStandHandler
        )
        InstanceEntityManager.getInstance().registerHandler(
            NamespaceID.from(Key.key("minecraft:item_frame")), ::ItemFrameHandler
        )
        InstanceEntityManager.getInstance().registerHandler(
            NamespaceID.from(Key.key("minecraft:tropical_fish")), ::TropicalFishHandler
        )
    }

    override fun terminate() {
    }
}