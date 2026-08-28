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
package net.onelitefeather.titan.common.navigator;

import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ExtendWith(MicrotusExtension.class)
class NavigatorLayoutTest {

    private static final int ROW = 9;

    // Instance fields, not constants: building an ItemStack needs the Minestom registries, which
    // the extension only loads once the test instance is being created.
    private final List<NavigatorEntry> publicEntries = List.of(entry("ElytraRace"), entry("Survival"), entry("Slender"), entry("Creative"));

    private final List<NavigatorEntry> withBuildServers = concat(this.publicEntries, List.of(buildServer("Build-1"), buildServer("Build-2")));

    private final UUID player = UUID.randomUUID();
    private final TestAudience audience = new TestAudience();

    @DisplayName("A team member sees the build servers next to the public entries")
    @Test
    void testBuildServersVisibleWithPermission() {
        this.audience.grant(this.player, BuildServerAccess.PERMISSION);

        List<NavigatorLayout.Placement> placements = NavigatorLayout.plan(this.withBuildServers, this.player, this.audience, ROW);

        Assertions.assertEquals(6, placements.size(), "All six entries should be offered");
        Assertions.assertEquals(List.of("Build-1", "Build-2"), destinations(placements).subList(4, 6), "The build servers should follow the public entries");
    }

    @DisplayName("Without the permission the build server entries are not shown at all")
    @Test
    void testBuildServersHiddenWithoutPermission() {
        List<NavigatorLayout.Placement> placements = NavigatorLayout.plan(this.withBuildServers, this.player, this.audience, ROW);

        Assertions.assertEquals(4, placements.size(), "Only the public entries should be offered");
        Assertions.assertFalse(destinations(placements).contains("Build-1"), "A hidden entry must not appear");
        Assertions.assertFalse(destinations(placements).contains("Build-2"), "A hidden entry must not appear");
    }

    @DisplayName("A hidden entry leaves no reserved slot: the slots stay one uninterrupted block")
    @Test
    void testHiddenEntriesLeaveNoGap() {
        List<NavigatorLayout.Placement> placements = NavigatorLayout.plan(this.withBuildServers, this.player, this.audience, ROW);

        List<Integer> slots = placements.stream().map(NavigatorLayout.Placement::slot).toList();
        Assertions.assertEquals(List.of(2, 3, 4, 5), slots, "Four visible entries should sit centred and adjacent");
        for (int index = 1; index < slots.size(); index++) {
            Assertions.assertEquals(slots.get(index - 1) + 1, slots.get(index), "Slots must not skip a position");
        }
    }

    @DisplayName("The menu without the permission is identical to a lobby that has no build servers")
    @Test
    void testHiddenMenuIsIndistinguishableFromNoBuildServers() {
        List<NavigatorLayout.Placement> withHidden = NavigatorLayout.plan(this.withBuildServers, this.player, this.audience, ROW);
        List<NavigatorLayout.Placement> withoutAny = NavigatorLayout.plan(this.publicEntries, this.player, this.audience, ROW);

        // NFR-005: the absence must not reveal that something is hidden. Nothing in the menu -
        // neither a slot nor an item - differs between the two lobbies.
        Assertions.assertEquals(withoutAny, withHidden, "The two menus must be indistinguishable");
    }

    @DisplayName("Adding or removing build servers does not move the entries of an unprivileged player")
    @Test
    void testUnprivilegedLayoutIsIndependentOfBuildServerCount() {
        List<NavigatorLayout.Placement> none = NavigatorLayout.plan(this.publicEntries, this.player, this.audience, ROW);
        List<NavigatorLayout.Placement> five = NavigatorLayout.plan(concat(this.publicEntries, List.of(buildServer("Build-1"), buildServer("Build-2"), buildServer("Build-3"), buildServer("Build-4"), buildServer("Build-5"))), this.player, this.audience, ROW);

        Assertions.assertEquals(none, five, "The count of hidden entries must not be observable");
    }

    @DisplayName("More visible entries than slots are truncated instead of shifting the menu size")
    @Test
    void testSurplusEntriesAreDropped() {
        this.audience.grant(this.player, BuildServerAccess.PERMISSION);
        List<NavigatorEntry> many = new ArrayList<>(this.publicEntries);
        for (int index = 0; index < 10; index++) {
            many.add(buildServer("Build-" + index));
        }

        List<NavigatorLayout.Placement> placements = NavigatorLayout.plan(many, this.player, this.audience, ROW);

        Assertions.assertEquals(ROW, placements.size(), "The menu should fill but never exceed the row");
        Assertions.assertEquals(0, placements.getFirst().slot(), "A full row starts at the first slot");
        Assertions.assertEquals(ROW - 1, placements.getLast().slot(), "A full row ends at the last slot");
    }

    @DisplayName("An empty entry list produces an empty layout instead of failing")
    @Test
    void testEmptyEntries() {
        Assertions.assertTrue(NavigatorLayout.plan(List.of(), this.player, this.audience, ROW).isEmpty());
    }

    private static List<String> destinations(List<NavigatorLayout.Placement> placements) {
        return placements.stream().map(placement -> placement.entry().destination()).toList();
    }

    private static List<NavigatorEntry> concat(List<NavigatorEntry> first, List<NavigatorEntry> second) {
        List<NavigatorEntry> entries = new ArrayList<>(first);
        entries.addAll(second);
        return List.copyOf(entries);
    }

    private static NavigatorEntry entry(String taskName) {
        return NavigatorEntry.task(icon(taskName), taskName);
    }

    private static NavigatorEntry buildServer(String serviceName) {
        return NavigatorEntry.restrictedServer(icon(serviceName), serviceName, BuildServerAccess.PERMISSION);
    }

    private static ItemStack icon(String name) {
        return ItemStack.builder(Material.PAPER).customName(Component.text(name)).build();
    }
}
