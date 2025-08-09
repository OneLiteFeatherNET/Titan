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
package net.onelitefeather.titan.common.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.Unit;

public final class Items {

    private Items() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static final ItemStack PLAYER_ELYTRA = ItemStack.builder(Material.ELYTRA).customName(Component.text("Elytra", NamedTextColor.DARK_PURPLE)).set(DataComponents.UNBREAKABLE, Unit.INSTANCE).build();

    public static final ItemStack PLAYER_TELEPORTER = ItemStack.builder(Material.FEATHER).customName(MiniMessage.miniMessage().deserialize("<!i><aqua>Navigator")).build();

    public static final ItemStack PLAYER_FIREWORK = ItemStack.builder(Material.FIREWORK_ROCKET).customName(Component.text("Firework Rocket")).build();

    public static final ItemStack NAVIGATOR_BLANK_ITEM_STACK = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.empty()).build();

    public static final ItemStack NAVIGATOR_ELYTRA_ITEM_STACK = ItemStack.builder(Material.ELYTRA).customName(MiniMessage.miniMessage().deserialize("<!i><gradient:#fcba03:#03fc8c>ElytraRace</gradient>")).build();

    public static final ItemStack NAVIGATOR_SLENDER_ITEM_STACK = ItemStack.builder(Material.ENDERMAN_SPAWN_EGG).customName(MiniMessage.miniMessage().deserialize("<!i><gradient:#616161:#e80000c>Slender</gradient>")).build();

    public static final ItemStack NAVIGATOR_SURVIVAL_ITEM_STACK = ItemStack.builder(Material.GRASS_BLOCK).customName(MiniMessage.miniMessage().deserialize("<!i><green>Survival")).build();

    public static final ItemStack NAVIGATOR_CREATIVE_ITEM_STACK = ItemStack.builder(Material.WOODEN_AXE).customName(MiniMessage.miniMessage().deserialize("<!i><rainbow>Creative</rainbow>")).build();
}
