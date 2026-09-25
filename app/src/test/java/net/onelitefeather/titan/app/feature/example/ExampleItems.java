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
package net.onelitefeather.titan.app.feature.example;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

/**
 * The {@code example} module's own item stacks and fixed messages, kept apart from
 * {@link ExampleModule} for readability - the same split {@code ElytraItems} uses. Package-private:
 * no other feature touches these directly, see {@code design.md}, decision 9 ("Tags/Items gehören
 * dem Feature").
 */
final class ExampleItems {

    private ExampleItems() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /** The hotbar item that triggers a greeting when used. */
    static final ItemStack GREETING_TOKEN = ItemStack.builder(Material.FEATHER).customName(Component.text("Greeting Token", NamedTextColor.YELLOW)).build();

    /**
     * Sent instead of a fresh greeting while a player is still on cooldown. Built once, here, and
     * reused on every dispatch, rather than a new {@link Component} being built for every use on
     * the tick thread - see {@code docs/lobby-modules.md}'s tick-thread rule "cache packets and
     * components".
     */
    static final Component ON_COOLDOWN = Component.text("You were just greeted - try again in a moment.", NamedTextColor.GRAY);
}
