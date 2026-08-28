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
package net.onelitefeather.titan.common.feature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.togglz.core.activation.DefaultActivationStrategyProvider;
import org.togglz.core.activation.Parameter;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.mem.InMemoryStateRepository;
import org.togglz.core.spi.ActivationStrategy;
import org.togglz.core.user.NoOpUserProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonWindowActivationStrategyTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

    private final SeasonWindowActivationStrategy strategy = new SeasonWindowActivationStrategy(Clock.fixed(NOW, ZoneOffset.UTC), BERLIN);

    private static FeatureState state() {
        return new FeatureState(TitanFeatures.NAVIGATOR_ELYTRA, true);
    }

    @Test
    @DisplayName("a state without bounds is always inside its window")
    void noBoundsMeansAlwaysOpen() {
        assertTrue(this.strategy.isWithinWindow(state()));
    }

    @Test
    @DisplayName("the window opens on its start and closes on its end")
    void windowIsInclusiveAtTheStartAndExclusiveAtTheEnd() {
        // 12:00Z is 14:00 in Berlin on 15.10.2026 (CEST).
        assertTrue(this.strategy.isWithinWindow(state().setParameter(SeasonWindowActivationStrategy.PARAM_FROM, "2026-10-15T14:00").setParameter(SeasonWindowActivationStrategy.PARAM_TO, "2026-10-15T14:01")));
        assertFalse(this.strategy.isWithinWindow(state().setParameter(SeasonWindowActivationStrategy.PARAM_FROM, "2026-10-15T14:01")));
        assertFalse(this.strategy.isWithinWindow(state().setParameter(SeasonWindowActivationStrategy.PARAM_TO, "2026-10-15T14:00")));
    }

    @Test
    @DisplayName("a bare date is read as the start of that day")
    void plainDatesAreReadAsStartOfDay() {
        assertTrue(this.strategy.isWithinWindow(state().setParameter(SeasonWindowActivationStrategy.PARAM_FROM, "2026-10-15").setParameter(SeasonWindowActivationStrategy.PARAM_TO, "2026-11-05")));
        assertEquals(LocalDateTime.parse("2026-10-15T00:00"), this.strategy.from(state().setParameter(SeasonWindowActivationStrategy.PARAM_FROM, "2026-10-15")).orElseThrow());
    }

    @Test
    @DisplayName("one-sided windows stay open on the missing side")
    void oneSidedWindowsStayOpen() {
        assertTrue(this.strategy.isWithinWindow(state().setParameter(SeasonWindowActivationStrategy.PARAM_FROM, "2026-01-01")));
        assertTrue(this.strategy.isWithinWindow(state().setParameter(SeasonWindowActivationStrategy.PARAM_TO, "2027-01-01")));
    }

    @Test
    @DisplayName("the zone parameter decides which local time the bounds mean")
    void zoneParameterIsHonoured() {
        FeatureState berlin = state().setParameter(SeasonWindowActivationStrategy.PARAM_FROM, "2026-10-15T13:00").setParameter(SeasonWindowActivationStrategy.PARAM_ZONE, "Europe/Berlin");
        FeatureState utc = state().setParameter(SeasonWindowActivationStrategy.PARAM_FROM, "2026-10-15T13:00").setParameter(SeasonWindowActivationStrategy.PARAM_ZONE, "UTC");
        // Berlin is on summer time in October: 12:00Z is 14:00 local, so the window is open there
        // and still closed in UTC. This is the difference Togglz' ReleaseDateActivationStrategy
        // cannot express.
        assertTrue(this.strategy.isWithinWindow(berlin));
        assertFalse(this.strategy.isWithinWindow(utc));
        assertEquals(ZoneId.of("UTC"), this.strategy.zoneOf(utc));
        assertEquals(BERLIN, this.strategy.zoneOf(state()));
    }

    @Test
    @DisplayName("an unreadable bound switches the feature off instead of widening it")
    void unreadableParametersFailClosed() {
        assertFalse(this.strategy.isWithinWindow(state().setParameter(SeasonWindowActivationStrategy.PARAM_FROM, "1. Oktober")));
        assertFalse(this.strategy.isWithinWindow(state().setParameter(SeasonWindowActivationStrategy.PARAM_ZONE, "Mars/Olympus")));
    }

    @Test
    @DisplayName("an unreadable bound is named, a readable one reports no problem")
    void windowProblemNamesTheOffendingParameter() {
        assertNull(this.strategy.windowProblem(state()));
        assertNull(this.strategy.windowProblem(state().setParameter(SeasonWindowActivationStrategy.PARAM_FROM, "2026-10-01").setParameter(SeasonWindowActivationStrategy.PARAM_TO, "2026-11-05T04:00")));

        String badFrom = this.strategy.windowProblem(state().setParameter(SeasonWindowActivationStrategy.PARAM_FROM, "1. Oktober"));
        assertNotNull(badFrom);
        assertTrue(badFrom.contains(SeasonWindowActivationStrategy.PARAM_FROM), badFrom);
        assertTrue(badFrom.contains("1. Oktober"), badFrom);

        String badZone = this.strategy.windowProblem(state().setParameter(SeasonWindowActivationStrategy.PARAM_ZONE, "Mars/Olympus"));
        assertNotNull(badZone);
        assertTrue(badZone.contains("Mars/Olympus"), badZone);
    }

    @Test
    @DisplayName("the strategy declares exactly from, to and zone, all optional")
    void declaresThreeOptionalParameters() {
        List<String> names = new ArrayList<>();
        for (Parameter parameter : this.strategy.getParameters()) {
            names.add(parameter.getName());
            assertTrue(parameter.isOptional(), parameter.getName() + " must be optional");
        }
        assertEquals(List.of(SeasonWindowActivationStrategy.PARAM_FROM, SeasonWindowActivationStrategy.PARAM_TO, SeasonWindowActivationStrategy.PARAM_ZONE), names);
        assertEquals(SeasonWindowActivationStrategy.ID, this.strategy.getId());
    }

    @Test
    @DisplayName("a feature manager dispatches to the strategy it found through the service file")
    void aFeatureManagerDispatchesToTheRegisteredStrategy() {
        InMemoryStateRepository repository = new InMemoryStateRepository();
        FeatureManager featureManager = new FeatureManagerBuilder().featureEnum(TitanFeatures.class).stateRepository(repository).userProvider(new NoOpUserProvider()).activationStrategyProvider(new DefaultActivationStrategyProvider()).build();

        // The strategy instance used here is the one the ServiceLoader built, so it runs on the
        // system clock. The bounds are deliberately decades wide: this test is about the wiring,
        // not about the arithmetic, which the tests above cover with a fixed clock.
        repository.setFeatureState(state().setStrategyId(SeasonWindowActivationStrategy.ID).setParameter(SeasonWindowActivationStrategy.PARAM_FROM, "2000-01-01").setParameter(SeasonWindowActivationStrategy.PARAM_TO, "2099-01-01"));
        assertTrue(featureManager.isActive(TitanFeatures.NAVIGATOR_ELYTRA));

        repository.setFeatureState(state().setStrategyId(SeasonWindowActivationStrategy.ID).setParameter(SeasonWindowActivationStrategy.PARAM_FROM, "2000-01-01").setParameter(SeasonWindowActivationStrategy.PARAM_TO, "2001-01-01"));
        assertFalse(featureManager.isActive(TitanFeatures.NAVIGATOR_ELYTRA));
    }

    @Test
    @DisplayName("the strategy is discovered through the Togglz activation-strategy service file")
    void isRegisteredAsAService() {
        List<String> ids = new ArrayList<>();
        for (ActivationStrategy loaded : ServiceLoader.load(ActivationStrategy.class, SeasonWindowActivationStrategyTest.class.getClassLoader())) {
            ids.add(loaded.getId());
        }
        assertTrue(ids.contains(SeasonWindowActivationStrategy.ID), "META-INF/services/org.togglz.core.spi.ActivationStrategy must list the season window; found " + ids);
    }
}
