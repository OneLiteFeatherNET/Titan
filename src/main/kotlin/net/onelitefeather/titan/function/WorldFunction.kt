package net.onelitefeather.titan.function

import com.google.inject.Inject
import com.google.inject.name.Named
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.utils.chunk.ChunkUtils
import net.onelitefeather.titan.instance.EntityAnvilLoader
import net.onelitefeather.titan.utils.TitanFeatures
import java.nio.file.Path
import java.nio.file.Paths

class WorldFunction : TitanFunction {

    private val basePath: Path = Paths.get("worlds")
    private val lobbyInstance = MinecraftServer.getInstanceManager()
        .createInstanceContainer(EntityAnvilLoader(createWorldPath()))

    @Named("spawnPos")
    @Inject
    lateinit var spawnPos: Pos


    fun provideLobbyWorld(): InstanceContainer {
        if (!ChunkUtils.isLoaded(lobbyInstance, spawnPos)) {
            lobbyInstance.loadChunk(spawnPos).whenComplete { _, throwable ->
                if (throwable != null) {
                    throw RuntimeException(
                        "Unable to load spawn chunk",
                        throwable
                    )
                }
            }
        }
        return lobbyInstance
    }

    private fun createWorldPath(): Path {
        if (TitanFeatures.HALLOWEEN.isActive) {
            return basePath.resolve("halloween")
        }
        if (TitanFeatures.WINTER.isActive) {
            return basePath.resolve("winter")
        }
        return basePath.resolve("world")
    }

    override fun initialize() {
    }

    override fun terminate() {
    }

}