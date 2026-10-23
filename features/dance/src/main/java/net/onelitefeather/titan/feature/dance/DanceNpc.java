/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.dance;

import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.player.ResolvableProfile;

/**
 * The dancer's stand-in: a mannequin in his skin, standing where he stood and shown to him alone.
 * It takes every step the dancer takes, so the camera has a dancing figure to look at.
 */
final class DanceNpc {

    private final LivingEntity entity;

    private DanceNpc(LivingEntity entity) {
        this.entity = entity;
    }

    /**
     * Spawns the NPC at {@code anchor}, visible to {@code dancer} only.
     *
     * @throws RuntimeException if the spawn fails; the entity is removed again first
     */
    static DanceNpc spawn(Player dancer, Instance instance, Pos anchor) {
        LivingEntity entity = new LivingEntity(EntityType.MANNEQUIN);
        entity.setAutoViewable(false);
        MannequinMeta meta = (MannequinMeta) entity.getEntityMeta();
        meta.setProfile(profileOf(dancer));
        // Immovable: other players cannot push it off the spot where the dancer stands.
        meta.setImmovable(true);
        try {
            entity.setInstance(instance, anchor).join();
            entity.addViewer(dancer);
        } catch (RuntimeException e) {
            entity.remove();
            throw e;
        }
        return new DanceNpc(entity);
    }

    /**
     * @return the dancer's skin when he has one, otherwise his name and id, which the client
     *         resolves itself
     */
    static ResolvableProfile profileOf(Player dancer) {
        PlayerSkin skin = dancer.getSkin();
        if (skin != null) {
            return new ResolvableProfile(skin);
        }
        return new ResolvableProfile(new ResolvableProfile.Partial(dancer.getUsername(), dancer.getUuid(), List.of()));
    }

    /** @return the living entity that takes the dance steps */
    LivingEntity entity() {
        return this.entity;
    }

    /** Removes the NPC from the instance and from the dancer's view. */
    void close() {
        this.entity.remove();
    }
}
