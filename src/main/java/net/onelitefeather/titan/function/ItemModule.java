package net.onelitefeather.titan.function;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.Unbreakable;

public final class ItemModule {

    private ItemModule() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static final ItemStack PLAYER_ELYTRA = ItemStack.builder(Material.ELYTRA)
            .customName(Component.text("Elytra", NamedTextColor.DARK_PURPLE))
            .set(ItemComponent.UNBREAKABLE, new Unbreakable(false))
            .build();

    public static final ItemStack PLAYER_TELEPORTER = ItemStack.builder(Material.FEATHER)
            .customName(MiniMessage.miniMessage().deserialize("<!i><aqua>Navigator"))
            .build();

    public static final ItemStack PLAYER_FIREWORK = ItemStack.builder(Material.FIREWORK_ROCKET)
            .customName(Component.text("Firework Rocket"))
            .build();

    public static final ItemStack NAVIGATOR_BLANK_ITEM_STACK = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE)
            .customName(Component.empty())
            .build();

    public static final ItemStack NAVIGATOR_ELYTRA_ITEM_STACK = ItemStack.builder(Material.ELYTRA)
            .customName(MiniMessage.miniMessage().deserialize("<!i><gradient:#fcba03:#03fc8c>ElytraRace</gradient>"))
            .build();

    public static final ItemStack NAVIGATOR_SLENDER_ITEM_STACK = ItemStack.builder(Material.ENDERMAN_SPAWN_EGG)
            .customName(MiniMessage.miniMessage().deserialize("<!i><gradient:#616161:#e80000c>Slender</gradient>"))
            .build();

    public static final ItemStack NAVIGATOR_SURVIVAL_ITEM_STACK = ItemStack.builder(Material.GRASS_BLOCK)
            .customName(MiniMessage.miniMessage().deserialize("<!i><green>Survival"))
            .build();

    public static final ItemStack NAVIGATOR_CREATIVE_ITEM_STACK = ItemStack.builder(Material.WOODEN_AXE)
            .customName(MiniMessage.miniMessage().deserialize("<!i><rainbow>Creative</rainbow>"))
            .build();
}
