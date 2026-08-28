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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageTransitionLoggerTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

    private final StageTransitionLogger logger = new StageTransitionLogger(Clock.fixed(NOW, ZoneOffset.UTC), BERLIN);

    @Test
    @DisplayName("the first observation seeds the memory instead of faking a transition")
    void firstObservationIsNotATransition() {
        assertTrue(this.logger.observe("NAVIGATOR_ELYTRA", ReleaseStage.INTERNAL).isEmpty());
    }

    @Test
    @DisplayName("a changed stage is reported with timestamp, old stage and new stage")
    void changedStageIsReported() {
        this.logger.observe("NAVIGATOR_ELYTRA", ReleaseStage.INTERNAL);

        Optional<StageTransition> transition = this.logger.observe("NAVIGATOR_ELYTRA", ReleaseStage.LITE);

        assertTrue(transition.isPresent());
        assertEquals("NAVIGATOR_ELYTRA", transition.orElseThrow().feature());
        assertEquals(ReleaseStage.INTERNAL, transition.orElseThrow().from());
        assertEquals(ReleaseStage.LITE, transition.orElseThrow().to());
        assertEquals(NOW, transition.orElseThrow().at().toInstant());
        assertEquals(BERLIN, transition.orElseThrow().at().getZone());
    }

    @Test
    @DisplayName("an unchanged stage is not reported again")
    void unchangedStageIsSilent() {
        this.logger.observe("NAVIGATOR_ELYTRA", ReleaseStage.GA);
        assertTrue(this.logger.observe("NAVIGATOR_ELYTRA", ReleaseStage.GA).isEmpty());
    }

    @Test
    @DisplayName("features are tracked independently")
    void featuresAreTrackedIndependently() {
        this.logger.observe("NAVIGATOR_ELYTRA", ReleaseStage.INTERNAL);
        assertTrue(this.logger.observe("NAVIGATOR_SLENDER", ReleaseStage.GA).isEmpty());
        assertTrue(this.logger.observe("NAVIGATOR_ELYTRA", ReleaseStage.GA).isPresent());
    }
}
