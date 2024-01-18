package net.onelitefeather.titan.biome

import net.minestom.server.world.biomes.BiomeParticle
import org.jglrxavpok.hephaistos.nbt.NBT
import org.jglrxavpok.hephaistos.nbt.NBTCompound
import java.util.Map

data class DustOption(val red: Float, val green: Float, val blue: Float, val scale: Float) : BiomeParticle.Option {
    override fun toNbt(): NBTCompound {
        return NBT.Compound(
            Map.of(
                "type", NBT.String("dust"),
                "color", NBT.Compound(
                    Map.of(
                    "r", NBT.Float(red),
                    "g", NBT.Float(green),
                    "b", NBT.Float(blue))
                    ),
                "scale", NBT.Float(scale)
            )
        )
    }


}
