package net.onelitefeather.titan

import com.google.inject.Guice
import com.google.inject.Inject
import net.minestom.server.MinecraftServer
import net.minestom.server.extensions.Extension
import net.onelitefeather.titan.commands.EndCommand
import net.onelitefeather.titan.function.TitanFunction

class TitanExtension : Extension() {

    @Inject
    private lateinit var functions: Set<@JvmSuppressWildcards TitanFunction>

    override fun initialize() {
        val injector = Guice.createInjector(
            TitanModule(this)
        )
        injector.injectMembers(this)
        functions.forEach { it.initialize() }
        MinecraftServer.getCommandManager().register(EndCommand())
    }

    override fun terminate() {
        functions.forEach { it.terminate() }
    }

}