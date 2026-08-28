/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
