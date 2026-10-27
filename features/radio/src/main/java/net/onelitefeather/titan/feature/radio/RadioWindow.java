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
package net.onelitefeather.titan.feature.radio;

import java.util.Locale;
import java.util.Objects;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.theevilreaper.aves.inventory.PersonalTranslatedInventoryBuilder;

/**
 * One player's radio window, built for the player's language. Aves caches a translated inventory
 * per locale and asks for the inventory without a locale while it registers, so every request
 * returns the one inventory of this window. Aves fixes the title at creation: a page or category
 * change therefore opens a new window.
 */
final class RadioWindow extends PersonalTranslatedInventoryBuilder {

    static final InventoryType TYPE = InventoryType.CHEST_6_ROW;

    private final Locale locale;

    RadioWindow(Player player, Locale locale) {
        super(TYPE, player);
        this.locale = Objects.requireNonNull(locale, "locale must not be null");
    }

    /** The language the window was built for. */
    Locale locale() {
        return this.locale;
    }

    @Override
    public Inventory getInventory(Locale ignored) {
        return super.getInventory(this.locale);
    }
}
