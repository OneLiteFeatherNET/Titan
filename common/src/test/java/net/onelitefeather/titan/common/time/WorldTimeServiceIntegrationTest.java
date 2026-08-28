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

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import static net.onelitefeather.titan.common.time.FixedInstants.BERLIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The half of {@link WorldTimeService} that touches Minestom, on a real instance.
 *
 * <p>Minestom's {@code net.minestom.server.instance.Clock} is a sealed interface, so a test double
 * for it cannot exist — which is fortunate, because the claim worth checking is that the instance's
 * own day cycle actually stops (US-2.02), and only a real instance can answer that.
 *
 * <p>Real time still never passes here: the service reads a {@link Clock} this test moves by hand.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
@ExtendWith(MicrotusExtension.class)
class WorldTimeServiceIntegrationTest {

    private static final Instant NOON = Instant.parse("2026-05-15T10:00:00Z");

    @Test
    @DisplayName("Binding stops Minestom's own day cycle and writes the current time")
    void bindingStopsTheOwnCycleAndWritesTheTime(Env env) {
        Instance instance = env.createFlatInstance();
        instance.setTime(123L);
        WorldTimeService service = WorldTimeService.create(Clock.fixed(NOON, BERLIN));

        service.bind(instance);

        assertNotNull(instance.defaultClock());
        assertEquals(0.0f, instance.defaultClock().rate(), "Minestom must not advance the clock itself once the service drives it");
        assertEquals(DayTimeStrategy.NOON_TICK, instance.getTime());
        assertSame(instance, service.boundInstance());
    }

    @Test
    @DisplayName("The frozen instance does not advance its time on its own, however long it ticks")
    void theFrozenInstanceDoesNotAdvanceOnItsOwn(Env env) {
        Instance instance = env.createFlatInstance();
        WorldTimeService service = WorldTimeService.create(Clock.fixed(NOON, BERLIN));
        service.bind(instance);

        for (int tick = 0; tick < 40; tick++) {
            env.tick();
        }

        assertEquals(DayTimeStrategy.NOON_TICK, instance.getTime(), "with a stopped rate and a fixed clock the day time must not move at all");
    }

    @Test
    @DisplayName("The time is written at most once per second, however often update is called")
    void theTimeIsWrittenAtMostOncePerSecond(Env env) {
        AtomicReference<Instant> now = new AtomicReference<>(NOON);
        Instance instance = env.createFlatInstance();
        WorldTimeService service = WorldTimeService.create(movableClock(now), BERLIN, DayTimeStrategy.linear());

        service.bind(instance);
        assertEquals(DayTimeStrategy.NOON_TICK, instance.getTime());

        // Twenty server ticks inside the same wall-clock second. A marker written between the calls
        // has to survive: if the service wrote per tick it would be overwritten immediately.
        for (int tick = 0; tick < 19; tick++) {
            now.set(now.get().plus(Duration.ofMillis(50)));
            instance.setTime(4711L);
            assertFalse(service.update(), "an update inside the same second must be skipped");
            assertEquals(4711L, instance.getTime());
        }

        now.set(NOON.plusSeconds(1));
        assertTrue(service.update(), "the first update in a new second must go through");
        assertEquals(DayTimeStrategy.NOON_TICK, instance.getTime());
    }

    @Test
    @DisplayName("A moving clock moves the lobby's day time with it")
    void aMovingClockMovesTheDayTime(Env env) {
        AtomicReference<Instant> now = new AtomicReference<>(NOON);
        Instance instance = env.createFlatInstance();
        WorldTimeService service = WorldTimeService.create(movableClock(now), BERLIN, DayTimeStrategy.linear());

        service.bind(instance);
        assertEquals(6000L, instance.getTime());

        now.set(NOON.plus(Duration.ofHours(6)));
        assertTrue(service.update());
        assertEquals(12000L, instance.getTime(), "six real hours later the lobby must be at nightfall");

        now.set(NOON.plus(Duration.ofHours(12)));
        assertTrue(service.update());
        assertEquals(18000L, instance.getTime(), "twelve real hours later the lobby must be at midnight");
    }

    @Test
    @DisplayName("The scheduled task keeps the time up to date without anyone calling update")
    void theScheduledTaskKeepsTheTimeUpToDate(Env env) {
        AtomicReference<Instant> now = new AtomicReference<>(NOON);
        Instance instance = env.createFlatInstance();
        WorldTimeService service = WorldTimeService.create(movableClock(now), BERLIN, DayTimeStrategy.linear());

        service.bind(instance);
        now.set(NOON.plus(Duration.ofHours(6)));

        // The repeat interval is one second, which Minestom's scheduler counts in server ticks.
        for (int tick = 0; tick < 25; tick++) {
            env.tick();
        }

        assertEquals(12000L, instance.getTime());
    }

    @Test
    @DisplayName("Unbinding stops the task and leaves the day time where it was")
    void unbindingStopsTheTask(Env env) {
        AtomicReference<Instant> now = new AtomicReference<>(NOON);
        Instance instance = env.createFlatInstance();
        WorldTimeService service = WorldTimeService.create(movableClock(now), BERLIN, DayTimeStrategy.linear());

        service.bind(instance);
        service.unbind();
        now.set(NOON.plus(Duration.ofHours(6)));
        for (int tick = 0; tick < 25; tick++) {
            env.tick();
        }

        assertNull(service.boundInstance());
        assertFalse(service.update());
        assertEquals(6000L, instance.getTime(), "an unbound service must not keep writing");
    }

    @Test
    @DisplayName("Binding a second instance releases the first")
    void bindingASecondInstanceReleasesTheFirst(Env env) {
        AtomicReference<Instant> now = new AtomicReference<>(NOON);
        Instance first = env.createFlatInstance();
        Instance second = env.createFlatInstance();
        WorldTimeService service = WorldTimeService.create(movableClock(now), BERLIN, DayTimeStrategy.linear());

        service.bind(first);
        service.bind(second);
        now.set(NOON.plus(Duration.ofHours(6)));
        for (int tick = 0; tick < 25; tick++) {
            env.tick();
        }

        assertSame(second, service.boundInstance());
        assertEquals(6000L, first.getTime(), "the released instance must not be written to any more");
        assertEquals(12000L, second.getTime());
    }

    private static Clock movableClock(AtomicReference<Instant> now) {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return BERLIN;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
    }
}
