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
package net.onelitefeather.titan.app.feature.navigator;

import java.util.List;
import java.util.Objects;
import net.minestom.server.item.Material;
import net.onelitefeather.titan.common.config.ConfigException;

/**
 * The {@code navigator} module's own configuration section: the shared inventory's title and every
 * destination shown in it.
 *
 * <p>See {@code openspec/changes/lobby-feature-modules/design.md}, decision 8, and the
 * {@code lobby-navigator} spec's "Navigator-Ziele kommen aus der Konfiguration" requirement: a new
 * destination is added purely by editing this section, with no code change.
 *
 * @param title   the shared navigator inventory's title, as a MiniMessage string
 * @param entries every destination shown in the navigator; two entries sharing a slot is only
 *                caught once every module has enabled - together with entries other modules
 *                contribute - by {@code NavigatorEntries#validate()}, not here
 */
public record NavigatorConfig(String title, List<Entry> entries) {

    /**
     * The four destinations the lobby has always shown - ElytraRace, Survival, Slender and
     * Creative.
     */
    public static final NavigatorConfig DEFAULTS = new NavigatorConfig("<yellow>Navigator", List.of(new Entry(0, "minecraft:elytra", "<!i><gradient:#fcba03:#03fc8c>ElytraRace</gradient>", "ElytraRace"), new Entry(4, "minecraft:grass_block", "<!i><green>Survival", "Survival"), new Entry(5, "minecraft:enderman_spawn_egg", "<!i><gradient:#616161:#e80000c>Slender</gradient>", "cygnus"), new Entry(8, "minecraft:wooden_axe", "<!i><rainbow>Creative</rainbow>", "MemberBuild")));

    /**
     * @throws NullPointerException if {@code title} or {@code entries} is {@code null}
     */
    public NavigatorConfig {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(entries, "entries must not be null");
        entries = List.copyOf(entries);
    }

    /**
     * One destination shown in the navigator inventory.
     *
     * @param slot        the slot this entry occupies, {@code 0}-{@code 8}, matching
     *                    {@link net.minestom.server.inventory.InventoryType#CHEST_1_ROW}
     * @param icon        the icon's material, as a namespaced key, e.g.
     *                    {@code "minecraft:grass_block"}
     * @param displayName the name shown to the player, as a MiniMessage string
     * @param destination the CloudNet task name a click on this entry delivers the player to
     */
    public record Entry(int slot, String icon, String displayName, String destination) {

        /**
         * @throws ConfigException      if {@code slot} is outside {@code 0}-{@code 8}, {@code icon}
         *                              does not name a known material, or {@code destination} is
         *                              blank
         * @throws NullPointerException if {@code icon}, {@code displayName} or {@code destination}
         *                              is {@code null}
         */
        public Entry {
            if (slot < 0 || slot > 8) {
                throw ConfigException.invalid("entries", "slot must be between 0 and 8 (CHEST_1_ROW), was " + slot);
            }
            Objects.requireNonNull(icon, "icon must not be null");
            if (Material.fromKey(icon) == null) {
                throw ConfigException.invalid("entries", "icon '" + icon + "' is not a known material");
            }
            Objects.requireNonNull(displayName, "displayName must not be null");
            Objects.requireNonNull(destination, "destination must not be null");
            if (destination.isBlank()) {
                throw ConfigException.invalid("entries", "destination must not be blank");
            }
        }
    }
}
