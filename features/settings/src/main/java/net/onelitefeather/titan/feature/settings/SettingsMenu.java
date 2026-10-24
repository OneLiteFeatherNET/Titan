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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.core.settings.BooleanSetting;
import net.onelitefeather.titan.core.settings.PlayerSettings;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.theevilreaper.aves.inventory.GlobalInventoryBuilder;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;

/**
 * The settings menu: one row with one item per registered option. A shared inventory is built for
 * each combination of option values and language, the first time a player needs it, so the
 * inventory a player sees always matches his values and nothing is built per player. A click
 * switches the option and opens the inventory of the new combination.
 */
final class SettingsMenu {

    private static final InventoryType ROW = InventoryType.CHEST_1_ROW;

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
        GlobalInventoryBuilder builder = new GlobalInventoryBuilder(this.messages.title(shared.language()), ROW);
        InventoryLayout layout = InventoryLayout.fromType(ROW);
        for (int slot = 0; slot < shared.definitions().size(); slot++) {
            BooleanSetting setting = shared.definitions().get(slot);
            boolean enabled = (shared.enabled() & 1L << slot) != 0;
            layout.setItem(slot, stack(setting, enabled, shared.language()), (player, clickedSlot, click, stack, result) -> {
                result.accept(ClickHolder.cancelClick());
                toggle(player, setting);
            });
        }
        builder.setLayout(layout);
        builder.invalidateLayout();
        builder.register();
        return builder;
    }

    /** Switches {@code setting} for {@code player} and shows the menu of the new combination. */
    void toggle(Player player, BooleanSetting setting) {
        this.settings.set(player.getUuid(), setting, !this.settings.isEnabled(player.getUuid(), setting));
        player.openInventory(inventoryFor(player));
    }

    private ItemStack stack(BooleanSetting setting, boolean enabled, Locale language) {
        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        return ItemStack.builder(material).customName(SettingsMessages.name(setting, language)).lore(SettingsMessages.lore(setting, enabled, language)).build();
    }

    /**
     * The key of a shared inventory: the options it lists, which of them are on, and the language.
     */
    private record Shared(List<BooleanSetting> definitions, long enabled, Locale language) {
    }
}
