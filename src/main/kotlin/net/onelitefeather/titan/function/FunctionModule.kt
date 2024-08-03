package net.onelitefeather.titan.function

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder

class FunctionModule : AbstractModule() {
    override fun configure() {
        install(ItemFunction())
        install(CloudFunction())
        val functionBinder = Multibinder.newSetBinder(binder(), TitanFunction::class.java)
        functionBinder.addBinding().to(BlockFunction::class.java)
        functionBinder.addBinding().to(PreventFunction::class.java)
        functionBinder.addBinding().to(WorldFunction::class.java)
        functionBinder.addBinding().to(NavigationFunction::class.java)
        functionBinder.addBinding().to(JoinFunction::class.java)
        functionBinder.addBinding().to(DeathFunction::class.java)
        functionBinder.addBinding().to(TickleFunction::class.java)
        functionBinder.addBinding().to(ElytraFunction::class.java)
        functionBinder.addBinding().to(SitFunction::class.java)
    }
}