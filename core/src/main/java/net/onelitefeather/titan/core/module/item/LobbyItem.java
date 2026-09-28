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
package net.onelitefeather.titan.core.module.item;

import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.minestom.server.item.ItemStack;

/**
 * A lobby item, contributed as a {@code @Bean} by a feature's own {@code @Factory} class and
 * collected by {@link LobbyItems} through Avaje Inject's list injection. {@code key} identifies
 * the item so a used stack can be traced back to {@code onUse}, and {@code featureId} attributes a
 * failing {@code onUse} to its feature.
 */
public record LobbyItem(String featureId, Key key, ItemStack itemStack, ItemSlot placement,
                        ItemUseHandler onUse) {

    public LobbyItem {
        Objects.requireNonNull(featureId, "featureId");
        if (featureId.isBlank()) {
            throw new IllegalArgumentException("featureId must not be blank");
        }
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(itemStack, "itemStack");
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(onUse, "onUse");
    }
}
