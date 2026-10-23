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
package net.onelitefeather.titan.feature.npc;

import java.util.List;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.ResolvableProfile;
import net.onelitefeather.titan.core.npc.NpcSkin;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the figure of a join NPC or of an apparition: a Mannequin that does not move, has no
 * gravity and cannot be hurt. Its skin travels in the profile, so no client has to look it up.
 */
final class Mannequins {

    private Mannequins() {
    }

    /** A figure not yet in any instance; the caller places it and decides who sees it. */
    static Entity create(@Nullable NpcSkin skin) {
        Entity entity = new Entity(EntityType.MANNEQUIN);
        entity.editEntityMeta(MannequinMeta.class, meta -> {
            meta.setImmovable(true);
            if (skin != null) {
                meta.setProfile(profile(skin));
            }
        });
        entity.setNoGravity(true);
        return entity;
    }

    static ResolvableProfile profile(NpcSkin skin) {
        return new ResolvableProfile(new ResolvableProfile.Partial(null, null, List.of(new GameProfile.Property("textures", skin.value(), skin.signature()))));
    }
}
