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
package net.onelitefeather.titan.common.resourcepack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeldPackRegistryTest {

    private final HeldPackRegistry registry = new HeldPackRegistry();
    private final UUID player = UUID.randomUUID();
    private final UUID basePack = UUID.randomUUID();
    private final UUID seasonPack = UUID.randomUUID();

    @Test
    @DisplayName("A pushed pack counts as held before the client answered")
    void testPushedPackIsHeld() {
        this.registry.markSent(this.player, PackSlot.BASE, this.basePack);

        assertTrue(this.registry.holds(this.player, PackSlot.BASE, this.basePack));
        assertFalse(this.registry.entry(this.player, PackSlot.BASE).orElseThrow().confirmed());
    }

    @Test
    @DisplayName("A confirmed pack keeps its identifier")
    void testConfirm() {
        this.registry.markSent(this.player, PackSlot.SEASON, this.seasonPack);
        this.registry.confirm(this.player, PackSlot.SEASON);

        HeldPackRegistry.Entry entry = this.registry.entry(this.player, PackSlot.SEASON).orElseThrow();
        assertEquals(this.seasonPack, entry.packId());
        assertTrue(entry.confirmed());
    }

    @Test
    @DisplayName("Slots are independent, forgetting the season leaves the base pack alone")
    void testSlotsAreIndependent() {
        this.registry.markSent(this.player, PackSlot.BASE, this.basePack);
        this.registry.markSent(this.player, PackSlot.SEASON, this.seasonPack);

        this.registry.forget(this.player, PackSlot.SEASON);

        assertTrue(this.registry.holds(this.player, PackSlot.BASE, this.basePack));
        assertTrue(this.registry.entry(this.player, PackSlot.SEASON).isEmpty());
    }

    @Test
    @DisplayName("A status report is routed to a slot by its pack identifier")
    void testSlotLookupByPackId() {
        this.registry.markSent(this.player, PackSlot.BASE, this.basePack);
        this.registry.markSent(this.player, PackSlot.SEASON, this.seasonPack);

        assertEquals(PackSlot.BASE, this.registry.slotOf(this.player, this.basePack));
        assertEquals(PackSlot.SEASON, this.registry.slotOf(this.player, this.seasonPack));
        assertNull(this.registry.slotOf(this.player, UUID.randomUUID()));
        assertNull(this.registry.slotOf(UUID.randomUUID(), this.basePack));
    }

    @Test
    @DisplayName("A slot remembers the pack that was pushed, not the one that is configured now")
    void testSlotKeepsTheOlderSeason() {
        UUID lastYear = UUID.randomUUID();
        this.registry.markSent(this.player, PackSlot.SEASON, lastYear);

        UUID thisYear = UUID.randomUUID();
        assertFalse(this.registry.holds(this.player, PackSlot.SEASON, thisYear));
        assertEquals(lastYear, this.registry.entry(this.player, PackSlot.SEASON).orElseThrow().packId());
    }

    @Test
    @DisplayName("A disconnect drops the whole player")
    void testForgetPlayer() {
        this.registry.markSent(this.player, PackSlot.BASE, this.basePack);
        this.registry.markSent(this.player, PackSlot.SEASON, this.seasonPack);

        this.registry.forget(this.player);

        assertEquals(0, this.registry.trackedPlayers());
        assertTrue(this.registry.entry(this.player, PackSlot.BASE).isEmpty());
    }

    @Test
    @DisplayName("Forgetting the last slot drops the player as well, so the book cannot grow forever")
    void testEmptyPlayerIsRemoved() {
        this.registry.markSent(this.player, PackSlot.BASE, this.basePack);

        this.registry.forget(this.player, PackSlot.BASE);

        assertEquals(0, this.registry.trackedPlayers());
    }
}
