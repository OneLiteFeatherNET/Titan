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
package net.onelitefeather.titan.common.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static net.onelitefeather.titan.common.time.FixedInstants.BERLIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * What the service decides before any instance is involved: which clock, which zone, which mapping.
 *
 * <p>The half of the service that talks to Minestom lives in
 * {@link WorldTimeServiceIntegrationTest}, on a real instance rather than a stand-in — Minestom's
 * {@code Clock} is a sealed interface and cannot be faked, so the only honest way to assert that
 * the
 * instance's own cycle really stops is to stop a real one.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
class WorldTimeServiceTest {

    private static final Instant NOON = Instant.parse("2026-05-15T10:00:00Z");

    @Test
    @DisplayName("The current time comes from the injected clock, not from the machine")
    void theCurrentTimeComesFromTheInjectedClock() {
        WorldTimeService service = WorldTimeService.create(Clock.fixed(NOON, BERLIN));

        assertEquals(DayTimeStrategy.NOON_TICK, service.currentTicks());
        assertEquals(BERLIN, service.zone());
        assertSame(DayTimeStrategy.linear(), service.strategy());
    }

    @Test
    @DisplayName("The default is the linear mapping in the editorial zone")
    void theDefaultIsLinearInTheEditorialZone() {
        WorldTimeService service = WorldTimeService.create(Clock.systemUTC());

        assertSame(DayTimeStrategy.linear(), service.strategy());
        assertEquals(TitanTime.EDITORIAL_ZONE, service.zone());
    }

    @Test
    @DisplayName("The mapping can be swapped without the caller changing")
    void theMappingCanBeSwapped() {
        WorldTimeService linear = WorldTimeService.create(Clock.fixed(NOON, BERLIN), DayTimeStrategy.linear());
        WorldTimeService solar = WorldTimeService.create(Clock.fixed(NOON, BERLIN), DayTimeStrategy.solar());

        assertSame(DayTimeStrategy.linear(), linear.strategy());
        assertSame(DayTimeStrategy.solar(), solar.strategy());
        assertEquals(DayTimeStrategy.linear().ticksAt(NOON, BERLIN), linear.currentTicks());
        assertEquals(DayTimeStrategy.solar().ticksAt(NOON, BERLIN), solar.currentTicks());
    }

    @Test
    @DisplayName("A different zone produces a different time from the same instant")
    void theZoneIsHonoured() {
        WorldTimeService berlin = WorldTimeService.create(Clock.fixed(NOON, BERLIN), BERLIN, DayTimeStrategy.linear());
        WorldTimeService utc = WorldTimeService.create(Clock.fixed(NOON, ZoneOffset.UTC), ZoneOffset.UTC, DayTimeStrategy.linear());

        assertEquals(6000L, berlin.currentTicks());
        assertEquals(4000L, utc.currentTicks());
    }

    @Test
    @DisplayName("The clock's own zone is ignored; the service uses the zone it was given")
    void theClockZoneIsIrrelevant() {
        WorldTimeService fromUtcClock = WorldTimeService.create(Clock.fixed(NOON, ZoneOffset.UTC), BERLIN, DayTimeStrategy.linear());

        assertEquals(6000L, fromUtcClock.currentTicks());
    }

    @Test
    @DisplayName("Without a bound instance an update does nothing")
    void updateWithoutBindingDoesNothing() {
        WorldTimeService service = WorldTimeService.create(Clock.fixed(NOON, BERLIN));

        assertFalse(service.update());
        assertNull(service.boundInstance());
    }

    @Test
    @DisplayName("Unbinding without ever binding is harmless")
    void unbindingWithoutBindingIsHarmless() {
        WorldTimeService service = WorldTimeService.create(Clock.fixed(NOON, BERLIN));

        service.unbind();

        assertNull(service.boundInstance());
    }
}
