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

import net.onelitefeather.titan.common.utils.ThreadHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.togglz.core.Feature;
import org.togglz.core.context.FeatureContext;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.repository.FeatureState;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Decides whether a player sees a feature. This is the only type in Titan that talks to Togglz;
 * navigator entries, seasonal content and portals ask the gate instead of a
 * {@link FeatureManager}.
 *
 * <p>The evaluation order is fixed by US-3.07 and is walked in exactly this order:
 *
 * <ol>
 * <li><b>kill switch</b> — a disabled feature is invisible to everyone, whatever its stage and
 * window say. A feature that was never enabled is disabled, so an unconfigured feature stays
 * dark rather than going public.</li>
 * <li><b>release stage</b> — {@code internal} needs {@value ReleaseStage#INTERNAL_PERMISSION},
 * {@code lite} additionally admits the {@value ReleaseStage#LITE_GROUP} group, {@code ga}
 * admits everyone. The stage is the feature-state parameter {@value #STAGE_PARAMETER}; a
 * feature without it is treated as {@link ReleaseStage#DEFAULT}.</li>
 * <li><b>time window</b> — evaluated by {@link SeasonWindowActivationStrategy}. A feature with
 * no window is always within it.</li>
 * </ol>
 *
 * <p>The three steps form a conjunction, so the order does not change the answer — it decides
 * which step is reported as the reason, and it is what {@code /season status} and the tests rely
 * on.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class FeatureGate {

    /** Feature-state parameter holding the release stage of a feature. */
    public static final String STAGE_PARAMETER = "stage";

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureGate.class);

    private final Supplier<FeatureManager> featureManager;
    private final FeatureAudience audience;
    private final SeasonWindowActivationStrategy window;
    private final StageTransitionLogger transitions;

    private FeatureGate(Supplier<FeatureManager> featureManager, FeatureAudience audience, Clock clock, ZoneId zone) {
        this.featureManager = featureManager;
        this.audience = audience;
        this.window = new SeasonWindowActivationStrategy(clock, zone);
        this.transitions = new StageTransitionLogger(clock, zone);
    }

    /**
     * Creates a gate backed by the ambient Togglz feature manager. The manager is resolved lazily
     * and on a thread whose context classloader can see Titan's service files, because
     * {@link FeatureContext} goes through the {@link java.util.ServiceLoader}.
     *
     * @param audience the source of permission and group answers
     * @param clock    the time source used for windows and transition timestamps
     * @param zone     the zone seasons are planned in when a feature names none
     * @return a gate on the application's feature manager
     */
    public static FeatureGate create(FeatureAudience audience, Clock clock, ZoneId zone) {
        return new FeatureGate(new LazyFeatureManager(), audience, clock, zone);
    }

    /**
     * Creates a gate on an explicitly supplied feature manager. Used where the manager is already
     * at hand — tests above all.
     *
     * @param featureManager the manager to read feature states from
     * @param audience       the source of permission and group answers
     * @param clock          the time source used for windows and transition timestamps
     * @param zone           the zone seasons are planned in when a feature names none
     * @return a gate on the given feature manager
     */
    public static FeatureGate with(FeatureManager featureManager, FeatureAudience audience, Clock clock, ZoneId zone) {
        return new FeatureGate(() -> featureManager, audience, clock, zone);
    }

    /**
     * Checks whether the given player currently sees the feature.
     *
     * @param feature  the feature to check
     * @param playerId the player's unique id
     * @return whether the feature is visible to that player
     */
    public boolean isVisibleTo(Feature feature, UUID playerId) {
        return decide(feature, playerId).isAllowed();
    }

    /**
     * Evaluates the feature for a player and reports which step decided the outcome.
     *
     * @param feature  the feature to check
     * @param playerId the player's unique id
     * @return the decision, naming the step that denied the feature when it is not allowed
     */
    public FeatureDecision decide(Feature feature, UUID playerId) {
        FeatureState state = state(feature);
        if (state == null || !state.isEnabled()) {
            return FeatureDecision.DENIED_KILL_SWITCH;
        }
        if (!stageOf(state).admits(playerId, this.audience)) {
            return FeatureDecision.DENIED_STAGE;
        }
        if (!this.window.isWithinWindow(state)) {
            return FeatureDecision.DENIED_WINDOW;
        }
        return FeatureDecision.ALLOWED;
    }

    /**
     * Reads the operator-facing status of one feature and records a stage transition when the
     * stage has moved since the last look.
     *
     * @param feature the feature to describe
     * @return kill switch, stage and window of the feature
     */
    public FeatureStatus status(Feature feature) {
        FeatureState state = state(feature);
        if (state == null) {
            return new FeatureStatus(feature.name(), true, ReleaseStage.DEFAULT, null, null, null, SeasonWindowActivationStrategy.DEFAULT_ZONE, false, null);
        }
        ReleaseStage stage = stageOf(state);
        this.transitions.observe(feature.name(), stage);
        String windowProblem = this.window.windowProblem(state);
        LocalDateTime from = this.window.from(state).orElse(null);
        LocalDateTime to = this.window.to(state).orElse(null);
        // An unreadable window is never an open one - keep the two answers from contradicting
        // each other rather than relying on isWithinWindow to fail closed on its own.
        boolean open = windowProblem == null && this.window.isWithinWindow(state);
        return new FeatureStatus(feature.name(), !state.isEnabled(), stage, unknownStageOf(state), from, to, zoneOf(state), open, windowProblem);
    }

    /**
     * Reads the status of every known feature, ordered by name so the command output is stable.
     *
     * @return one status per feature the feature manager knows
     */
    public List<FeatureStatus> statuses() {
        List<FeatureStatus> statuses = new ArrayList<>();
        for (Feature feature : this.featureManager.get().getFeatures()) {
            statuses.add(status(feature));
        }
        statuses.sort(Comparator.comparing(FeatureStatus::feature));
        return List.copyOf(statuses);
    }

    /**
     * Walks every feature once and logs the stage transitions that happened since the previous
     * walk. Meant to be scheduled, so a stage change is recorded even while nobody is online to
     * trigger an evaluation.
     *
     * @return the transitions observed in this walk
     */
    public List<StageTransition> pollStageTransitions() {
        List<StageTransition> observed = new ArrayList<>();
        for (Feature feature : this.featureManager.get().getFeatures()) {
            FeatureState state = state(feature);
            if (state == null) {
                continue;
            }
            this.transitions.observe(feature.name(), stageOf(state)).ifPresent(observed::add);
        }
        return List.copyOf(observed);
    }

    private ReleaseStage stageOf(FeatureState state) {
        String configured = state.getParameter(STAGE_PARAMETER);
        Optional<ReleaseStage> stage = ReleaseStage.fromId(configured);
        if (stage.isEmpty() && configured != null && !configured.isBlank()) {
            LOGGER.warn("Feature {} is configured with the unknown release stage '{}'; falling back to {}", state.getFeature().name(), configured, ReleaseStage.DEFAULT.id());
        }
        return stage.orElse(ReleaseStage.DEFAULT);
    }

    /**
     * Returns the configured stage id when it is not one of the three known ones. The gate itself
     * falls back to {@link ReleaseStage#DEFAULT}, but an operator who wrote {@code intern} instead
     * of {@code internal} needs to see the typo rather than a stage they did not configure.
     */
    private static @Nullable String unknownStageOf(FeatureState state) {
        String configured = state.getParameter(STAGE_PARAMETER);
        if (configured == null || configured.isBlank()) {
            return null;
        }
        return ReleaseStage.fromId(configured).isPresent() ? null : configured.trim();
    }

    private ZoneId zoneOf(FeatureState state) {
        try {
            return this.window.zoneOf(state);
        } catch (RuntimeException exception) {
            return SeasonWindowActivationStrategy.DEFAULT_ZONE;
        }
    }

    private @Nullable FeatureState state(Feature feature) {
        return this.featureManager.get().getFeatureState(feature);
    }

    /**
     * Resolves and caches the ambient feature manager on a thread whose context classloader can
     * see Titan's {@code META-INF/services} entries.
     */
    private static final class LazyFeatureManager implements Supplier<FeatureManager>, ThreadHelper {

        private volatile @Nullable FeatureManager delegate;

        @Override
        public FeatureManager get() {
            FeatureManager current = this.delegate;
            if (current == null) {
                current = syncThreadForServiceLoader(FeatureContext::getFeatureManager);
                this.delegate = current;
            }
            return current;
        }
    }
}
