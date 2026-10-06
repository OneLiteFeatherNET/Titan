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

import java.util.Optional;
import java.util.UUID;
import net.minestom.server.entity.PlayerSkin;

/**
 * Where the skin of a player comes from; swapped for a fake in tests, so none needs the network.
 */
@FunctionalInterface
public interface HeadSkins {

    /** No skin for anybody. */
    HeadSkins NONE = id -> Optional.empty();

    /**
     * The skin of the player, empty when there is none. May block on the network, so callers keep
     * it off the tick threads.
     *
     * @throws RuntimeException when the lookup itself fails
     */
    Optional<PlayerSkin> skinOf(UUID id);
}
