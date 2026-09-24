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
package net.onelitefeather.titan.app.module.navigator;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the {@code lobby-navigator} spec scenarios that concern {@link NavigatorEntries} itself:
 * a module contributing an entry, an entry disappearing once its module is disabled, two entries
 * sharing a slot aborting startup by naming the slot and both entries' origin module, the version
 * counter, and the snapshot {@link NavigatorEntries#entries()} returns.
 */
class NavigatorEntriesTest {

    private static final ItemStack ICON = ItemStack.of(Material.FEATHER);

    private static NavigatorEntry entryAt(int slot, String destination) {
        return new NavigatorEntry(slot, ICON, Component.text(destination), destination);
    }

    @DisplayName("An entry added by a module appears in entries()")
    @Test
    void addedEntryAppearsInEntries() {
        NavigatorEntries entries = new NavigatorEntries();

        entries.add("teaser", entryAt(4, "Voyager"));

        Assertions.assertEquals(List.of(entryAt(4, "Voyager")), entries.entries());
    }

    @DisplayName("Entries added through a module's view disappear once that module's disable hook runs")
    @Test
    void entryDisappearsAfterModuleDisabled() {
        NavigatorEntries entries = new NavigatorEntries();
        List<Runnable> disableHooks = new ArrayList<>();

        NavigatorEntries.View view = entries.forModule("teaser", disableHooks::add);
        view.add(entryAt(4, "Voyager"));
        Assertions.assertEquals(1, entries.entries().size(), "the entry must be visible while the module is enabled");

        disableHooks.forEach(Runnable::run);

        Assertions.assertTrue(entries.entries().isEmpty(), "the entry must be gone once the owning module is disabled");
    }

    @DisplayName("Disabling one module does not remove another module's entries")
    @Test
    void disablingOneModuleLeavesOthersUntouched() {
        NavigatorEntries entries = new NavigatorEntries();
        List<Runnable> teaserDisableHooks = new ArrayList<>();
        entries.forModule("teaser", teaserDisableHooks::add).add(entryAt(2, "Voyager"));
        entries.forModule("navigator", hook -> {
        }).add(entryAt(4, "Survival"));

        teaserDisableHooks.forEach(Runnable::run);

        Assertions.assertEquals(List.of(entryAt(4, "Survival")), entries.entries());
    }

    @DisplayName("Two entries on the same slot make validate() abort, naming the slot and both origins")
    @Test
    void validateAbortsOnASharedSlotNamingBothOrigins() {
        NavigatorEntries entries = new NavigatorEntries();
        entries.add("navigator", entryAt(4, "Survival"));
        entries.add("teaser", entryAt(4, "Voyager"));

        NavigatorConflictException thrown = Assertions.assertThrows(NavigatorConflictException.class, entries::validate);

        Assertions.assertTrue(thrown.getMessage().contains("4"), "the message must name the slot");
        Assertions.assertTrue(thrown.getMessage().contains("Survival") && thrown.getMessage().contains("navigator"), "the message must name the first entry and its module");
        Assertions.assertTrue(thrown.getMessage().contains("Voyager") && thrown.getMessage().contains("teaser"), "the message must name the second entry and its module");
    }

    @DisplayName("Entries on different slots pass validate()")
    @Test
    void validatePassesWhenSlotsDoNotOverlap() {
        NavigatorEntries entries = new NavigatorEntries();
        entries.add("navigator", entryAt(0, "ElytraRace"));
        entries.add("navigator", entryAt(4, "Survival"));

        Assertions.assertDoesNotThrow(entries::validate);
    }

    @DisplayName("version() increments when an entry is added")
    @Test
    void versionIncrementsOnAdd() {
        NavigatorEntries entries = new NavigatorEntries();
        long before = entries.version();

        entries.add("navigator", entryAt(0, "ElytraRace"));

        Assertions.assertEquals(before + 1, entries.version());
    }

    @DisplayName("version() increments when an entry is removed")
    @Test
    void versionIncrementsOnRemove() {
        NavigatorEntries entries = new NavigatorEntries();
        entries.add("teaser", entryAt(2, "Voyager"));
        long beforeRemoval = entries.version();

        entries.removeAll("teaser");

        Assertions.assertEquals(beforeRemoval + 1, entries.version());
    }

    @DisplayName("version() does not change when removing a module that contributed nothing")
    @Test
    void versionUnchangedWhenRemovingAnUnknownModule() {
        NavigatorEntries entries = new NavigatorEntries();
        long before = entries.version();

        entries.removeAll("nobody");

        Assertions.assertEquals(before, entries.version());
    }

    @DisplayName("entries() returns an immutable snapshot")
    @Test
    void entriesSnapshotIsImmutable() {
        NavigatorEntries entries = new NavigatorEntries();
        entries.add("navigator", entryAt(0, "ElytraRace"));

        List<NavigatorEntry> snapshot = entries.entries();

        Assertions.assertThrows(UnsupportedOperationException.class, () -> snapshot.add(entryAt(1, "Other")));
    }

    @DisplayName("entries() is ordered by slot regardless of insertion order")
    @Test
    void entriesAreOrderedBySlot() {
        NavigatorEntries entries = new NavigatorEntries();
        entries.add("navigator", entryAt(8, "Creative"));
        entries.add("navigator", entryAt(0, "ElytraRace"));
        entries.add("navigator", entryAt(4, "Survival"));

        List<Integer> slots = entries.entries().stream().map(NavigatorEntry::slot).toList();

        Assertions.assertEquals(List.of(0, 4, 8), slots);
    }
}
