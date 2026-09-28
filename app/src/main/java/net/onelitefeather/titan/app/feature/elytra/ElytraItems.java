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
package net.onelitefeather.titan.app.feature.elytra;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.FireworkList;
import net.minestom.server.utils.Unit;

/**
 * The {@code elytra} module's own item stacks, kept apart from {@link ElytraModule} for
 * readability. Package-private: no other feature touches these directly.
 */
final class ElytraItems {

    /**
     * The maximum a player can craft; matches the burn duration for
     * {@code elytra.burnDurationTicks}.
     */
    private static final int FLIGHT_DURATION = 3;

    private ElytraItems() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    static final ItemStack ELYTRA = ItemStack.builder(Material.ELYTRA).customName(Component.text("Elytra", NamedTextColor.DARK_PURPLE)).set(DataComponents.UNBREAKABLE, Unit.INSTANCE).build();

    /** No explosions: exists to boost flight, not to burst into colour. */
    static final ItemStack FIREWORK = ItemStack.builder(Material.FIREWORK_ROCKET).customName(Component.text("Firework Rocket")).set(DataComponents.FIREWORKS, new FireworkList(FLIGHT_DURATION, List.of())).build();
}
