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

import net.onelitefeather.titan.common.utils.TitanFeatures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.togglz.core.activation.DefaultActivationStrategyProvider;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.file.FileBasedStateRepository;
import org.togglz.core.repository.mem.InMemoryStateRepository;
import org.togglz.core.user.NoOpUserProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureGateTest {

    private static final TitanFeatures FEATURE = TitanFeatures.NAVIGATOR_ELYTRA;
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

    private static final UUID TEAM = UUID.randomUUID();
    private static final UUID LITE = UUID.randomUUID();
    private static final UUID ANYONE = UUID.randomUUID();

    /** A window that is open at {@link #NOW}. */
    private static final String OPEN_FROM = "2026-10-01";
    private static final String OPEN_TO = "2026-11-05";

    /** A window that has already closed at {@link #NOW}. */
    private static final String CLOSED_FROM = "2026-01-01";
    private static final String CLOSED_TO = "2026-02-01";

    private InMemoryStateRepository repository;
    private FeatureGate gate;

    @BeforeEach
    void setUp() {
        this.repository = new InMemoryStateRepository();
        FeatureManager featureManager = new FeatureManagerBuilder().featureEnum(TitanFeatures.class).stateRepository(this.repository).userProvider(new NoOpUserProvider()).activationStrategyProvider(new DefaultActivationStrategyProvider()).build();
        TestFeatureAudience audience = new TestFeatureAudience().grantPermission(TEAM, ReleaseStage.INTERNAL_PERMISSION).joinGroup(LITE, ReleaseStage.LITE_GROUP);
        this.gate = FeatureGate.with(featureManager, audience, Clock.fixed(NOW, ZoneOffset.UTC), BERLIN);
    }

    private void configure(boolean enabled, ReleaseStage stage, String from, String to) {
        FeatureState state = new FeatureState(FEATURE, enabled).setStrategyId(SeasonWindowActivationStrategy.ID).setParameter(FeatureGate.STAGE_PARAMETER, stage.id());
        if (from != null) {
            state.setParameter(SeasonWindowActivationStrategy.PARAM_FROM, from);
        }
        if (to != null) {
            state.setParameter(SeasonWindowActivationStrategy.PARAM_TO, to);
        }
        this.repository.setFeatureState(state);
    }

    @Test
    @DisplayName("the kill switch beats an open window and a general release")
    void killSwitchBeatsStageAndWindow() {
        configure(false, ReleaseStage.GA, OPEN_FROM, OPEN_TO);

        assertEquals(FeatureDecision.DENIED_KILL_SWITCH, this.gate.decide(FEATURE, ANYONE));
        assertEquals(FeatureDecision.DENIED_KILL_SWITCH, this.gate.decide(FEATURE, LITE));
        assertEquals(FeatureDecision.DENIED_KILL_SWITCH, this.gate.decide(FEATURE, TEAM));
        assertFalse(this.gate.isVisibleTo(FEATURE, TEAM));
    }

    @Test
    @DisplayName("lite players see a feature that has not reached general release")
    void liteSeesWhatGaHasNotReached() {
        configure(true, ReleaseStage.LITE, OPEN_FROM, OPEN_TO);

        assertTrue(this.gate.isVisibleTo(FEATURE, LITE));
        assertTrue(this.gate.isVisibleTo(FEATURE, TEAM));
        assertEquals(FeatureDecision.DENIED_STAGE, this.gate.decide(FEATURE, ANYONE));
    }

    @Test
    @DisplayName("a player without permissions sees nothing that is not on ga")
    void withoutPermissionsOnlyGaIsVisible() {
        configure(true, ReleaseStage.INTERNAL, null, null);
        assertEquals(FeatureDecision.DENIED_STAGE, this.gate.decide(FEATURE, ANYONE));

        configure(true, ReleaseStage.LITE, null, null);
        assertEquals(FeatureDecision.DENIED_STAGE, this.gate.decide(FEATURE, ANYONE));

        configure(true, ReleaseStage.GA, null, null);
        assertEquals(FeatureDecision.ALLOWED, this.gate.decide(FEATURE, ANYONE));
    }

    @Test
    @DisplayName("the stage is evaluated before the window, so the stage is the reported reason")
    void stageIsEvaluatedBeforeTheWindow() {
        configure(true, ReleaseStage.INTERNAL, CLOSED_FROM, CLOSED_TO);

        // Both the stage and the window would deny this player; the fixed order reports the stage.
        assertEquals(FeatureDecision.DENIED_STAGE, this.gate.decide(FEATURE, ANYONE));
        // The team member passes the stage, so the window becomes the deciding step.
        assertEquals(FeatureDecision.DENIED_WINDOW, this.gate.decide(FEATURE, TEAM));
    }

    @Test
    @DisplayName("a closed window hides a feature that is generally released")
    void closedWindowHidesAGeneralRelease() {
        configure(true, ReleaseStage.GA, CLOSED_FROM, CLOSED_TO);

        assertEquals(FeatureDecision.DENIED_WINDOW, this.gate.decide(FEATURE, ANYONE));
    }

    @Test
    @DisplayName("a feature without a stage parameter stays internal")
    void missingStageFallsBackToInternal() {
        this.repository.setFeatureState(new FeatureState(FEATURE, true));

        assertEquals(FeatureDecision.DENIED_STAGE, this.gate.decide(FEATURE, ANYONE));
        assertEquals(FeatureDecision.ALLOWED, this.gate.decide(FEATURE, TEAM));
    }

    @Test
    @DisplayName("an unknown stage name stays internal instead of widening the audience")
    void unknownStageFallsBackToInternal() {
        this.repository.setFeatureState(
                new FeatureState(FEATURE, true).setParameter(FeatureGate.STAGE_PARAMETER, "everyone"));

        assertEquals(FeatureDecision.DENIED_STAGE, this.gate.decide(FEATURE, ANYONE));
        assertEquals(ReleaseStage.INTERNAL, this.gate.status(FEATURE).stage());
    }

    @Test
    @DisplayName("a feature nobody configured is invisible, not public")
    void unconfiguredFeatureIsInvisible() {
        assertEquals(FeatureDecision.DENIED_KILL_SWITCH, this.gate.decide(FEATURE, TEAM));
    }

    @Test
    @DisplayName("the status reports kill switch, stage and window of every feature")
    void statusReportsKillSwitchStageAndWindow() {
        configure(true, ReleaseStage.LITE, OPEN_FROM, OPEN_TO);

        FeatureStatus status = this.gate.status(FEATURE);

        assertEquals(FEATURE.name(), status.feature());
        assertFalse(status.killSwitchEngaged());
        assertEquals(ReleaseStage.LITE, status.stage());
        assertTrue(status.hasWindow());
        assertTrue(status.withinWindow());
        assertEquals(BERLIN, status.zone());
        assertEquals(TitanFeatures.values().length, this.gate.statuses().size());
    }

    @Test
    @DisplayName("a feature without a window reports no bounds and counts as open")
    void statusOfAWindowlessFeature() {
        configure(true, ReleaseStage.GA, null, null);

        FeatureStatus status = this.gate.status(FEATURE);

        assertFalse(status.hasWindow());
        assertNull(status.from());
        assertNull(status.to());
        assertTrue(status.withinWindow());
    }

    @Test
    @DisplayName("stage and window are read from a real flags.properties")
    void readsStageAndWindowFromAFlagFile(@TempDir Path directory) throws IOException {
        Path flags = directory.resolve("flags.properties");
        Files.writeString(flags, """
                NAVIGATOR_ELYTRA = true
                NAVIGATOR_ELYTRA.strategy = season-window
                NAVIGATOR_ELYTRA.param.stage = lite
                NAVIGATOR_ELYTRA.param.from = 2026-10-01
                NAVIGATOR_ELYTRA.param.to = 2026-11-05
                NAVIGATOR_ELYTRA.param.zone = Europe/Berlin
                """);
        FeatureManager featureManager = new FeatureManagerBuilder().featureEnum(TitanFeatures.class).stateRepository(new FileBasedStateRepository(flags.toFile())).userProvider(new NoOpUserProvider()).activationStrategyProvider(new DefaultActivationStrategyProvider()).build();
        FeatureGate fileGate = FeatureGate.with(featureManager, new TestFeatureAudience().grantPermission(TEAM, ReleaseStage.INTERNAL_PERMISSION).joinGroup(LITE, ReleaseStage.LITE_GROUP), Clock.fixed(NOW, ZoneOffset.UTC), BERLIN);

        assertEquals(FeatureDecision.ALLOWED, fileGate.decide(FEATURE, LITE));
        assertEquals(FeatureDecision.DENIED_STAGE, fileGate.decide(FEATURE, ANYONE));

        FeatureStatus status = fileGate.status(FEATURE);
        assertEquals(ReleaseStage.LITE, status.stage());
        assertEquals(BERLIN, status.zone());
        assertTrue(status.withinWindow());
    }

    @Test
    @DisplayName("polling reports a stage change once and stays silent afterwards")
    void pollingReportsEachStageChangeOnce() {
        configure(true, ReleaseStage.INTERNAL, null, null);
        assertTrue(this.gate.pollStageTransitions().isEmpty(), "the first walk only seeds the memory");

        configure(true, ReleaseStage.LITE, null, null);
        List<StageTransition> transitions = this.gate.pollStageTransitions();

        assertEquals(1, transitions.size());
        assertEquals(ReleaseStage.INTERNAL, transitions.getFirst().from());
        assertEquals(ReleaseStage.LITE, transitions.getFirst().to());
        assertEquals(NOW, transitions.getFirst().at().toInstant());
        assertTrue(this.gate.pollStageTransitions().isEmpty());
    }
}
