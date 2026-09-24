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
package net.onelitefeather.titan.app.module.item;

import java.util.Objects;
import net.kyori.adventure.key.Key;
import net.minestom.server.item.ItemStack;

/**
 * A lobby item a module hands {@link ItemRegistry} through {@code context.items().register(...)}.
 *
 * <p>{@code key} is this item's identity - {@link ItemRegistry} stamps it onto the registered
 * stack's {@link ItemRegistry#IDENTITY_TAG} so a used stack can be traced back to {@code onUse}
 * without relying on material or display name (see {@code design.md}, decision 7). {@code
 * placement} says where the item lives, if anywhere; see {@link ItemSlot}.
 */
public record LobbyItem(Key key, ItemStack itemStack, ItemSlot placement, ItemUseHandler onUse) {

    public LobbyItem {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(itemStack, "itemStack");
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(onUse, "onUse");
    }
}
