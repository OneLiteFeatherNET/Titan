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
package net.onelitefeather.titan.feature.settings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.PlayerSettings;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.theevilreaper.aves.inventory.GlobalInventoryBuilder;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.function.InventoryClick;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;

/**
 * The settings menu: three rows. The top and bottom rows are a frame of glass panes; the middle row
 * holds one feature icon per registered option and the row below holds the switch of each option.
 * Clicking the icon or the switch flips the option. A shared inventory is built for each
 * combination of option values and language, the first time a player needs it, so the inventory a
 * player sees always matches his values and nothing is built per player.
 */
final class SettingsMenu {

    private static final InventoryType TYPE = InventoryType.CHEST_3_ROW;
    private static final ItemStack FILLER = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE).customName(Component.empty()).build();
    private static final ItemStack FRAME_LIGHT = ItemStack.builder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).customName(Component.empty()).build();
    private static final ItemStack FRAME_CYAN = ItemStack.builder(Material.CYAN_STAINED_GLASS_PANE).customName(Component.empty()).build();
    private static final InventoryClick CANCEL = (player, slot, click, stack, result) -> result.accept(ClickHolder.cancelClick());
    private static final Sound CLICK = Sound.sound(SoundEvent.UI_BUTTON_CLICK, Sound.Source.UI, 1.0f, 1.0f);

    private final PlayerSettings settings;
    private final SettingsRegistry registry;
    private final SettingsMessages messages;
    private final Map<Shared, GlobalInventoryBuilder> menus = new HashMap<>();

    SettingsMenu(PlayerSettings settings, SettingsRegistry registry, SettingsMessages messages) {
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.messages = Objects.requireNonNull(messages, "messages must not be null");
    }

    void open(Player player) {
        player.openInventory(inventoryFor(player));
    }

    /** Unregisters every shared inventory; the menu cannot be opened afterwards. */
    void close() {
        this.menus.values().forEach(GlobalInventoryBuilder::unregister);
        this.menus.clear();
    }

    private Inventory inventoryFor(Player player) {
        List<BooleanSetting> definitions = this.registry.definitions();
        long enabled = enabledMask(player.getUuid(), definitions);
        Shared shared = new Shared(definitions, enabled, SettingsMessages.languageOf(player.getLocale()));
        return this.menus.computeIfAbsent(shared, this::build).getInventory();
    }

    private long enabledMask(UUID player, List<BooleanSetting> definitions) {
        long mask = 0;
        for (int slot = 0; slot < definitions.size(); slot++) {
            if (this.settings.isEnabled(player, definitions.get(slot))) {
                mask |= 1L << slot;
            }
        }
        return mask;
    }

    private GlobalInventoryBuilder build(Shared shared) {
        GlobalInventoryBuilder builder = new GlobalInventoryBuilder(this.messages.title(shared.language()), TYPE);
        InventoryLayout layout = InventoryLayout.fromType(TYPE);
        List<BooleanSetting> definitions = shared.definitions();
        int count = definitions.size();
        Set<Integer> options = new HashSet<>();
        for (int index = 0; index < count; index++) {
            options.add(SettingsLayout.iconSlot(index, count));
            options.add(SettingsLayout.toggleSlot(index, count));
        }
        for (int slot = 0; slot < TYPE.getSize(); slot++) {
            if (!options.contains(slot)) {
                layout.setItem(slot, filler(slot), CANCEL);
            }
        }
        for (int index = 0; index < count; index++) {
            BooleanSetting setting = definitions.get(index);
            boolean enabled = (shared.enabled() & 1L << index) != 0;
            InventoryClick switchOption = switchOption(setting);
            layout.setItem(SettingsLayout.iconSlot(index, count), icon(setting, enabled, shared.language()), switchOption);
            layout.setItem(SettingsLayout.toggleSlot(index, count), toggle(enabled, shared.language()), switchOption);
        }
        builder.setLayout(layout);
        builder.invalidateLayout();
        builder.register();
        return builder;
    }

    /** Cancels the click and switches {@code setting}, both for its icon and for its switch. */
    private InventoryClick switchOption(BooleanSetting setting) {
        return (player, slot, click, stack, result) -> {
            result.accept(ClickHolder.cancelClick());
            toggle(player, setting);
        };
    }

    /**
     * Switches {@code setting} for {@code player}, plays the click and shows the menu of the new
     * combination.
     */
    void toggle(Player player, BooleanSetting setting) {
        this.settings.set(player.getUuid(), setting, !this.settings.isEnabled(player.getUuid(), setting));
        player.playSound(CLICK, Sound.Emitter.self());
        player.openInventory(inventoryFor(player));
    }

    /**
     * The glass pane of a slot that no option uses. The middle row is grey so the options stand out
     * in it; the top and bottom rows alternate light blue and cyan, like the navigator's frame.
     */
    private static ItemStack filler(int slot) {
        if (slot / SettingsLayout.COLUMNS == SettingsLayout.ICON_ROW) {
            return FILLER;
        }
        return (slot / SettingsLayout.COLUMNS + slot % SettingsLayout.COLUMNS) % 2 == 0 ? FRAME_LIGHT : FRAME_CYAN;
    }

    /** The feature icon of an option; an enabled option glints. */
    private static ItemStack icon(BooleanSetting setting, boolean enabled, Locale language) {
        ItemStack.Builder builder = ItemStack.builder(SettingsIcons.of(setting.key())).customName(SettingsMessages.name(setting, language)).lore(SettingsMessages.description(setting, language));
        if (enabled) {
            builder.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return builder.build();
    }

    /**
     * The switch below an option's icon: lime when on, grey when off, with its state as the name.
     */
    private static ItemStack toggle(boolean enabled, Locale language) {
        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        return ItemStack.builder(material).customName(SettingsMessages.toggleName(enabled, language)).lore(SettingsMessages.toggleHint(language)).build();
    }

    /**
     * The key of a shared inventory: the options it lists, which of them are on, and the language.
     */
    private record Shared(List<BooleanSetting> definitions, long enabled, Locale language) {
    }
}
