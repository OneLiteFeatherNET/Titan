package net.onelitefeather.titan.blockhandler

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.utils.NamespaceID

class BedHandler : BlockHandler {
    override fun getNamespaceId(): NamespaceID  = NamespaceID.from(Key.key("minecraft:bed"))
}