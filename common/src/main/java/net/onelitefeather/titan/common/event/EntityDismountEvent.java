package net.onelitefeather.titan.common.event;

import net.minestom.server.entity.Entity;
import net.minestom.server.event.trait.EntityEvent;
import org.jetbrains.annotations.NotNull;

public record EntityDismountEvent(Entity rider, Entity vehicle) implements EntityEvent {
    @Override
    public @NotNull Entity getEntity() {
        return vehicle;
    }
}
