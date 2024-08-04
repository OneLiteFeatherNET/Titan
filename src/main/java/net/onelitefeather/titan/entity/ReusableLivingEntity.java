package net.onelitefeather.titan.entity;

import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReusableLivingEntity extends LivingEntity {

    private AtomicBoolean spawned = new AtomicBoolean(false);

    public ReusableLivingEntity(@NotNull EntityType entityType) {
        super(entityType);
    }

    public ReusableLivingEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {

    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        if (!spawned.get()) {
            super.updateNewViewer(player);
            spawned.set(true);
        }
    }
}
