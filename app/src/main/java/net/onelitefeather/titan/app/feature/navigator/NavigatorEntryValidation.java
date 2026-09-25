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

import java.util.Objects;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

/**
 * The checks behind one {@code navigator.entries} entry - slot {@code 0}-{@code 8}, a known
 * material and a non-blank destination - as pure functions, plus {@link #buildEntry} which applies
 * them to the plain values read for one entry name.
 *
 * <p>Deliberately free of any {@code io.avaje.config.Config}/{@code Configuration} type, per
 * {@code openspec/changes/avaje-config-facade/design.md}, decision 3: reading the values stays
 * {@link NavigatorModule}'s job, this class only checks and assembles them. {@link #buildEntry}
 * returns a {@link ConfiguredNavigatorEntry} rather than the platform's
 * {@link net.onelitefeather.titan.app.module.navigator.NavigatorEntry} on purpose: the latter's
 * icon is a fully built {@link net.minestom.server.item.ItemStack}, which - unlike
 * {@link Material#fromKey(String)} - needs a booted Minestom registry beyond what a plain unit test
 * provides. Deserializing the icon and display name into a renderable
 * {@link net.onelitefeather.titan.app.module.navigator.NavigatorEntry} stays
 * {@link NavigatorModule}'s job, so this class - and its {@code buildEntry} unit test - stays a
 * fast, server-free unit (design.md, decision 6).
 */
final class NavigatorEntryValidation {

    private NavigatorEntryValidation() {
    }

    /**
     * The plain, not-yet-rendered form of one {@code navigator.entries} entry {@link #buildEntry}
     * returns: the icon and display name are still the raw configuration strings, not yet resolved
     * to a {@link Material}/{@code Component} pair - see the class-level Javadoc for why.
     *
     * @param slot        the slot this entry occupies, {@code 0}-{@code 8}
     * @param icon        the icon's material, as a namespaced key, already known to
     *                    {@link Material#fromKey(String)}
     * @param displayName the name shown to the player, as a MiniMessage string
     * @param destination the CloudNet task name a click on this entry delivers the player to
     * @param feature     the name of the feature flag this entry is gated behind, or {@code null}
     *                    if it is always visible
     */
    record ConfiguredNavigatorEntry(int slot, String icon, String displayName, String destination,
                                    @Nullable String feature) {
    }

    /**
     * Builds and validates one navigator entry from the plain values read for entry {@code name}
     * under {@code navigator.entries.<name>}.
     *
     * @param name        the entry's name, the first segment of its keys under
     *                    {@code navigator.entries} (see {@link NavigatorEntryKeys#names(java.util
     *                    .Set)})
     * @param slot        the slot this entry occupies, {@code 0}-{@code 8}
     * @param icon        the icon's material, as a namespaced key
     * @param displayName the name shown to the player, as a MiniMessage string
     * @param destination the CloudNet task name a click on this entry delivers the player to
     * @param feature     the name of the feature flag this entry is gated behind, or {@code null}
     *                    if it is always visible
     * @return the validated entry
     * @throws IllegalArgumentException if {@code slot} is outside {@code 0}-{@code 8}, {@code icon}
     *                                  does not name a known material, or {@code destination} is
     *                                  blank; the message names the full key,
     *                                  {@code navigator.entries.<name>.<field>}
     * @throws NullPointerException     if {@code name}, {@code icon}, {@code displayName} or
     *                                  {@code destination} is {@code null}
     */
    static ConfiguredNavigatorEntry buildEntry(String name, int slot, String icon, String displayName, String destination, @Nullable String feature) {
        Objects.requireNonNull(name, "name must not be null");
        String prefix = "navigator.entries." + name + ".";
        requireValidSlot(prefix + "slot", slot);
        requireKnownMaterial(prefix + "icon", icon);
        Objects.requireNonNull(displayName, "displayName must not be null");
        requireNonBlankDestination(prefix + "destination", destination);
        return new ConfiguredNavigatorEntry(slot, icon, displayName, destination, feature);
    }

    /**
     * @param key  the full configuration key {@code slot} was read from, named in the exception on
     *             failure
     * @param slot the slot to check
     * @throws IllegalArgumentException if {@code slot} is outside {@code 0}-{@code 8}, matching
     *                                  {@link net.minestom.server.inventory.InventoryType#CHEST_1_ROW}
     */
    static void requireValidSlot(String key, int slot) {
        if (slot < 0 || slot > 8) {
            throw new IllegalArgumentException(key + ": slot must be between 0 and 8 (CHEST_1_ROW), was " + slot);
        }
    }

    /**
     * @param key  the full configuration key {@code icon} was read from, named in the exception on
     *             failure
     * @param icon the icon's material, as a namespaced key
     * @throws IllegalArgumentException if {@code icon} does not name a material known to
     *                                  {@link Material#fromKey(String)}
     * @throws NullPointerException     if {@code icon} is {@code null}
     */
    static void requireKnownMaterial(String key, String icon) {
        Objects.requireNonNull(icon, "icon must not be null");
        if (Material.fromKey(icon) == null) {
            throw new IllegalArgumentException(key + ": icon '" + icon + "' is not a known material");
        }
    }

    /**
     * @param key         the full configuration key {@code destination} was read from, named in
     *                    the exception on failure
     * @param destination the destination to check
     * @throws IllegalArgumentException if {@code destination} is blank
     * @throws NullPointerException     if {@code destination} is {@code null}
     */
    static void requireNonBlankDestination(String key, String destination) {
        Objects.requireNonNull(destination, "destination must not be null");
        if (destination.isBlank()) {
            throw new IllegalArgumentException(key + ": destination must not be blank");
        }
    }
}
