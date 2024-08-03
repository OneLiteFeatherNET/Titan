package net.onelitefeather.titan

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import com.google.inject.name.Names
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.onelitefeather.titan.function.FunctionModule

class TitanModule(private val titanExtension: TitanExtension) :
    AbstractModule() {

    override fun configure() {
        bind(TitanExtension::class.java).toInstance(titanExtension)
        bind(Pos::class.java).annotatedWith(Names.named("spawnPos"))
            .toInstance(Pos(0.5, 65.0, 0.5, -180f, 0f))
        bind(object : TypeLiteral<EventNode<Event>>() {}).annotatedWith(Names.named("titanNode"))
            .toInstance(titanExtension.eventNode)
        install(FunctionModule())
    }
}