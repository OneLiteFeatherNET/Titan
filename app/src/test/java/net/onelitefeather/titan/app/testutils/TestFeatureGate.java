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
package net.onelitefeather.titan.app.testutils;

import net.onelitefeather.titan.common.feature.FeatureAudience;
import net.onelitefeather.titan.common.feature.FeatureGate;
import net.onelitefeather.titan.common.feature.ReleaseStage;
import net.onelitefeather.titan.common.utils.TitanFeatures;
import org.togglz.core.activation.DefaultActivationStrategyProvider;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.mem.InMemoryStateRepository;
import org.togglz.core.user.NoOpUserProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * A real {@link FeatureGate} over an in-memory Togglz repository, with the flags and the
 * permission answers writable from a test. Nothing here is a stub of the gate itself: tests using
 * it exercise the same evaluation the lobby runs.
 */
public final class TestFeatureGate {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private final InMemoryStateRepository repository = new InMemoryStateRepository();
    private final Set<String> permissions = new HashSet<>();
    private final Set<String> groups = new HashSet<>();
    private final FeatureGate gate;

    private TestFeatureGate() {
        FeatureManager featureManager = new FeatureManagerBuilder().featureEnum(TitanFeatures.class).stateRepository(this.repository).userProvider(new NoOpUserProvider()).activationStrategyProvider(new DefaultActivationStrategyProvider()).build();
        this.gate = FeatureGate.with(featureManager, new MutableAudience(), Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC), BERLIN);
    }

    /**
     * Creates a fixture in which no feature is configured at all - every feature is therefore
     * invisible until it is released.
     *
     * @return a new fixture
     */
    public static TestFeatureGate create() {
        return new TestFeatureGate();
    }

    /**
     * Releases a feature to the given audience, with no time window.
     *
     * @param feature the feature to release
     * @param stage   the stage to put it on
     * @return this fixture
     */
    public TestFeatureGate release(TitanFeatures feature, ReleaseStage stage) {
        this.repository.setFeatureState(new FeatureState(feature, true).setParameter(FeatureGate.STAGE_PARAMETER, stage.id()));
        return this;
    }

    /**
     * Engages the kill switch of a feature, leaving its stage untouched.
     *
     * @param feature the feature to switch off
     * @return this fixture
     */
    public TestFeatureGate killSwitch(TitanFeatures feature) {
        this.repository.setFeatureState(new FeatureState(feature, false).setParameter(FeatureGate.STAGE_PARAMETER, ReleaseStage.GA.id()));
        return this;
    }

    /**
     * Grants a permission to a player.
     *
     * @param playerId   the player
     * @param permission the permission node
     * @return this fixture
     */
    public TestFeatureGate grant(UUID playerId, String permission) {
        this.permissions.add(playerId + "/" + permission);
        return this;
    }

    /**
     * Adds a player to a group.
     *
     * @param playerId the player
     * @param group    the group name
     * @return this fixture
     */
    public TestFeatureGate join(UUID playerId, String group) {
        this.groups.add(playerId + "/" + group.toLowerCase(Locale.ROOT));
        return this;
    }

    /**
     * Returns the gate under test.
     *
     * @return the gate
     */
    public FeatureGate gate() {
        return this.gate;
    }

    /** Reads the sets live, so a test may grant a permission after the gate was built. */
    private final class MutableAudience implements FeatureAudience {

        @Override
        public boolean hasPermission(UUID playerId, String permission) {
            return TestFeatureGate.this.permissions.contains(playerId + "/" + permission);
        }

        @Override
        public boolean inGroup(UUID playerId, String group) {
            return TestFeatureGate.this.groups.contains(playerId + "/" + group.toLowerCase(Locale.ROOT));
        }
    }
}
