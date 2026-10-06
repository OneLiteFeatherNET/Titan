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
package net.onelitefeather.titan.feature.jumprun.head;

import java.util.List;
import java.util.UUID;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.ResolvableProfile;

/** The skin of a team member, resolved once: who it belongs to and the textures to show. */
public record HeadSkin(UUID id, String textures, String signature) {

    /** The profile a head shows: the textures travel with it, so no client has to look them up. */
    public ResolvableProfile profile() {
        return new ResolvableProfile(new ResolvableProfile.Partial(null, id, List.of(new GameProfile.Property("textures", textures, signature))));
    }
}
