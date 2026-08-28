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
package net.onelitefeather.titan.common.portal;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.deliver.DeliverType;
import net.onelitefeather.titan.common.feature.TitanFeatures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalDefinitionTest {

    private static final Vec MIN = new Vec(0, 64, 0);
    private static final Vec MAX = new Vec(4, 68, 4);

    @Test
    @DisplayName("a complete entry resolves into a portal")
    void resolvesCompleteEntry() {
        PortalDefinition definition = new PortalDefinition("survival", "server", "Survival-1", "NAVIGATOR_SURVIVAL", MIN, MAX);

        Portal portal = definition.resolve().orElseThrow();

        assertEquals("survival", portal.id());
        assertEquals(DeliverType.SERVER, portal.type());
        assertEquals("Survival-1", portal.target());
        assertEquals(TitanFeatures.NAVIGATOR_SURVIVAL, portal.feature());
        assertTrue(portal.isGated());
        assertTrue(portal.contains(new Pos(2.5, 66, 2.5)));
    }

    @Test
    @DisplayName("an entry without a type delivers to a task, and an entry without a feature is ungated")
    void appliesDefaults() {
        Portal portal = new PortalDefinition("elytra", null, "ElytraRace", null, MIN, MAX).resolve().orElseThrow();

        assertEquals(DeliverType.TASK, portal.type());
        assertNull(portal.feature());
        assertFalse(portal.isGated());
    }

    @Test
    @DisplayName("type and feature are read case-insensitively and trimmed")
    void readsValuesLeniently() {
        Portal portal = new PortalDefinition(" creative ", " Server ", " MemberBuild ", " navigator_creative ", MIN, MAX).resolve().orElseThrow();

        assertEquals("creative", portal.id());
        assertEquals(DeliverType.SERVER, portal.type());
        assertEquals("MemberBuild", portal.target());
        assertEquals(TitanFeatures.NAVIGATOR_CREATIVE, portal.feature());
    }

    @Test
    @DisplayName("the corners are normalised, so a reversed region still works")
    void normalisesCorners() {
        Portal portal = new PortalDefinition("reversed", null, "Survival", null, MAX, MIN).resolve().orElseThrow();

        assertTrue(portal.contains(new Pos(2.5, 66, 2.5)));
        assertEquals(0, portal.region().min().blockX());
        assertEquals(4, portal.region().max().blockX());
    }

    @Test
    @DisplayName("an entry naming an unknown feature is dropped instead of running ungated")
    void dropsUnknownFeature() {
        Optional<Portal> portal = new PortalDefinition("typo", null, "Survival", "NAVIGATOR_SURVIVAAL", MIN, MAX).resolve();

        assertTrue(portal.isEmpty());
    }

    @Test
    @DisplayName("an entry naming an unknown target type is dropped")
    void dropsUnknownType() {
        assertTrue(new PortalDefinition("typo", "lobby", "Survival", null, MIN, MAX).resolve().isEmpty());
    }

    @Test
    @DisplayName("an entry without an id, a target or a full region is dropped")
    void dropsIncompleteEntries() {
        assertTrue(new PortalDefinition(null, null, "Survival", null, MIN, MAX).resolve().isEmpty(), "no id");
        assertTrue(new PortalDefinition("  ", null, "Survival", null, MIN, MAX).resolve().isEmpty(), "blank id");
        assertTrue(new PortalDefinition("portal", null, null, null, MIN, MAX).resolve().isEmpty(), "no target");
        assertTrue(new PortalDefinition("portal", null, " ", null, MIN, MAX).resolve().isEmpty(), "blank target");
        assertTrue(new PortalDefinition("portal", null, "Survival", null, null, MAX).resolve().isEmpty(), "no min corner");
        assertTrue(new PortalDefinition("portal", null, "Survival", null, MIN, null).resolve().isEmpty(), "no max corner");
    }

    @Test
    @DisplayName("two identical corners are dropped, because Coris needs a region with volume")
    void dropsDegenerateRegion() {
        assertTrue(new PortalDefinition("point", null, "Survival", null, MIN, MIN).resolve().isEmpty());
    }

    @Test
    @DisplayName("a region spanning more chunk columns than the index accepts is dropped")
    void dropsOversizedRegion() {
        // 4096 columns is the limit; 64 x 65 chunks is one column row past it.
        Vec far = new Vec(64 * 16, 68, 65 * 16);

        assertTrue(new PortalDefinition("mistyped", null, "Survival", null, MIN, far).resolve().isEmpty());
    }

    @Test
    @DisplayName("a region right at the column limit is still accepted")
    void keepsRegionAtTheLimit() {
        // 64 x 64 chunk columns exactly.
        Vec far = new Vec(63 * 16 + 15, 68, 63 * 16 + 15);

        assertTrue(new PortalDefinition("large", null, "Survival", null, new Vec(0, 64, 0), far).resolve().isPresent());
    }
}
