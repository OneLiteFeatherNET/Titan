package net.onelitefeather.titan.function

import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.utils.NamespaceID
import net.onelitefeather.titan.blockhandler.BedHandler
import net.onelitefeather.titan.blockhandler.JukeboxHandler
import net.onelitefeather.titan.blockhandler.BeaconHandler
import net.onelitefeather.titan.blockhandler.SignHandler
import net.onelitefeather.titan.blockhandler.BannerHandler
import net.onelitefeather.titan.blockhandler.SkullHandler
import net.onelitefeather.titan.blockhandler.CandleHandler

class BlockFunction : TitanFunction {
    override fun initialize() {
        MinecraftServer.getBlockManager().registerHandler(
            NamespaceID.from(Key.key("minecraft:bed")),
            ::BedHandler
        )
        MinecraftServer.getBlockManager().registerHandler(
            NamespaceID.from(Key.key("minecraft:jukebox")),
            ::JukeboxHandler
        )
        MinecraftServer.getBlockManager().registerHandler(
            NamespaceID.from(Key.key("minecraft:beacon")),
            ::BeaconHandler
        )
        MinecraftServer.getBlockManager().registerHandler(
            NamespaceID.from(Key.key("minecraft:sign")),
            ::SignHandler
        )
        MinecraftServer.getBlockManager().registerHandler(
            NamespaceID.from(Key.key("minecraft:banner")),
            ::BannerHandler
        )
        MinecraftServer.getBlockManager().registerHandler(
            NamespaceID.from(Key.key("minecraft:skull")),
            ::SkullHandler
        )
        MinecraftServer.getBlockManager().registerHandler(
            NamespaceID.from(Key.key("minecraft:candle")),
            ::CandleHandler
        )
    }

    override fun terminate() {
    }
}