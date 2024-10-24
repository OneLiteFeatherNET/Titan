package net.onelitefeather.titan.function

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import net.onelitefeather.titan.utils.TitanFeatures

class FunctionModule : AbstractModule() {
    override fun configure() {
        install(ItemModule())
        val functionBinder =
            Multibinder.newSetBinder(binder(), TitanFunction::class.java)
        functionBinder.addBinding().to(WorldFunction::class.java)
        functionBinder.addBinding().to(NavigationFunction::class.java)
        functionBinder.addBinding().to(JoinFunction::class.java)
        functionBinder.addBinding().to(TickleFunction::class.java)
        functionBinder.addBinding().to(SitFunction::class.java)
        functionBinder.addBinding().to(RespawnFunction::class.java)
    }
}