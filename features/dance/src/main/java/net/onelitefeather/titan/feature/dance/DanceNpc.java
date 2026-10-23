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
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.player.ResolvableProfile;

/**
 * The dancer as the others see him: a mannequin in his skin, standing where he stood, with his name
 * above it. While it lives the real player is hidden from every other player, so the NPC is the
 * only body that dances; the dancer's own view of it comes from the camera.
 */
final class DanceNpc {

    /** Hides the real dancer from every viewer; the NPC replaces him for all of them. */
    private static final Predicate<Player> NOBODY = viewer -> false;

    private final Player dancer;
    private final LivingEntity entity;

    private DanceNpc(Player dancer, LivingEntity entity) {
        this.dancer = dancer;
        this.entity = entity;
    }

    /**
     * Spawns the NPC at {@code anchor} for everyone in the instance and hides the real dancer from
     * the others, including players who join during the dance.
     *
     * @throws RuntimeException if the spawn fails; the entity is removed and the dancer shown again
     */
    static DanceNpc spawn(Player dancer, Instance instance, Pos anchor) {
        LivingEntity entity = new LivingEntity(EntityType.MANNEQUIN);
        MannequinMeta meta = (MannequinMeta) entity.getEntityMeta();
        meta.setProfile(profileOf(dancer));
        // Immovable: other players cannot push it off the spot where the dancer stands.
        meta.setImmovable(true);
        // The client shows the name tag of any living entity that carries a visible custom name.
        entity.set(DataComponents.CUSTOM_NAME, Component.text(dancer.getUsername()));
        entity.setCustomNameVisible(true);
        try {
            entity.setInstance(instance, anchor).join();
            dancer.updateViewableRule(NOBODY);
        } catch (RuntimeException e) {
            try {
                dancer.updateViewableRule(null);
            } finally {
                entity.remove();
            }
            throw e;
        }
        return new DanceNpc(dancer, entity);
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

    /**
     * Removes the NPC and shows the real dancer to everyone again. The dancer's visibility is
     * restored even if the removal fails.
     */
    void close() {
        try {
            this.entity.remove();
        } finally {
            if (this.dancer.isOnline()) {
                this.dancer.updateViewableRule(null);
            }
        }
    }
}
