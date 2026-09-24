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

import java.util.function.Consumer;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;

/**
 * Default {@link ModuleItems}. Registration and its matching unregistration are one unit, exactly
 * like {@code ModuleCommandsImpl}: every {@link #register} call immediately queues its own cleanup
 * through {@code onDisable}, so {@code ModuleRegistry} does not need to know anything about items
 * specifically.
 */
final class ModuleItemsImpl implements ModuleItems {

    private final String moduleId;
    private final ItemRegistry registry;
    private final Consumer<Runnable> onDisable;

    ModuleItemsImpl(String moduleId, ItemRegistry registry, Consumer<Runnable> onDisable) {
        this.moduleId = moduleId;
        this.registry = registry;
        this.onDisable = onDisable;
    }

    @Override
    public ItemStack register(LobbyItem item) {
        ItemStack stamped = this.registry.register(this.moduleId, item);
        this.onDisable.accept(() -> this.registry.unregister(item.key()));
        return stamped;
    }

    @Override
    public void equip(Player player) {
        this.registry.equip(player);
    }
}
