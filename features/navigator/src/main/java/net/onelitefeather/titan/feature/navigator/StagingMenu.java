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
package net.onelitefeather.titan.feature.navigator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

/**
 * The staging submenu: one row of nine slots with the staging entries on slots 0 to 3 and a back
 * button on slot 8. The main grid's staging hub opens it. {@link NavigatorModule} lays it out and
 * owns its clicks; this class holds the fixed parts.
 */
final class StagingMenu {

    static final InventoryType TYPE = InventoryType.CHEST_1_ROW;
    static final int SIZE = 9;
    static final int BACK_SLOT = 8;
    private static final String HUB_GRADIENT = "<gradient:#616161:#ffaa00>";

    private StagingMenu() {
    }

    static Component title(Locale language) {
        return NavigatorMessages.text(NavigatorMessages.STAGING_MENU_TITLE, language);
    }

    /** The hub entry on the main grid: a structure block named Staging, with the team-only hint. */
    static ItemStack hub(Locale language) {
        Component name = MiniMessage.miniMessage().deserialize("<!i>" + HUB_GRADIENT + "<name></gradient>", Placeholder.component("name", title(language)));
        List<Component> lore = new ArrayList<>(NavigatorMessages.modeLore(HUB_GRADIENT, OptionalInt.empty(), OptionalInt.empty(), List.of(), language));
        lore.add(NavigatorMessages.hintLine(NavigatorMessages.STAGING_HINT, language));
        return ItemStack.builder(Material.STRUCTURE_BLOCK).customName(name).lore(List.copyOf(lore)).build();
    }

    /** The back button, which returns to the navigator. */
    static ItemStack back(Locale language) {
        return ServerPicker.backItem(language);
    }
}
