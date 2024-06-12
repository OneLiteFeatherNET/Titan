package net.onelitefeather.titan.event

import net.minestom.server.entity.Entity
import net.minestom.server.event.trait.EntityEvent

class EntityDismountEvent(private val entity: Entity, val rider: Entity) : EntityEvent {
    override fun getEntity(): Entity = entity
}