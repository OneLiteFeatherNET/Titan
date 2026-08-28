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
package net.onelitefeather.titan.common.season;

import net.onelitefeather.titan.common.feature.FeatureGate;
import net.onelitefeather.titan.common.feature.ReleaseStage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.util.NamedFeature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * One season, exactly as its file says it is. This is the whole contract between an operator and
 * the lobby: an id, whether it is switched on, how it ranks against other seasons, who may see it,
 * which world it wants, when it runs and what it does.
 *
 * <p>Everything here is a value. There is no place to put a condition, a callback or a class name,
 * and that is the point — a season is added by writing one of these files and a world directory,
 * never by writing Java.
 *
 * <p>Seasons cannot refer to each other (US-4.06). That is not enforced by a rule that could be
 * forgotten, it is a property of this record: there is no field in which one season could name
 * another, and therefore no deployment order to get right.
 *
 * @param id       the season's id, lowercase letters, digits, {@code -} and {@code _}; also the
 *                 feature name the gate evaluates it under
 * @param enabled  the kill switch; a season switched off here is invisible whatever its window says
 * @param priority which season wins where two overlap — higher applies later and therefore on top
 *                 (US-4.05)
 * @param stage    the audience the season has been released to, exactly as for a feature flag
 * @param world    the world directory this season wants the lobby to load, {@code null} to leave
 *                 the world alone
 * @param window   when the season runs
 * @param effects  what the season does, in the order the file lists them
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public record SeasonDefinition(String id, boolean enabled, int priority, ReleaseStage stage,
                               @Nullable String world, SeasonWindow window,
                               List<SeasonEffect> effects) {

    /** Ids are used as feature names, world folder names and log keys; keep them boring. */
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_-]*");

    /**
     * Orders seasons the way they are applied: lowest priority first, so the highest priority is
     * applied last and is what a player ends up seeing. Ties fall back to the id, so the order
     * never depends on the order the files happened to be read in.
     */
    public static final Comparator<SeasonDefinition> BY_PRIORITY = Comparator.comparingInt(SeasonDefinition::priority).thenComparing(SeasonDefinition::id);

    /**
     * Normalises the effect list and rejects an unusable id.
     */
    public SeasonDefinition {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("a season id must be lowercase letters, digits, '-' or '_', got '" + id + "'");
        }
        if (stage == null) {
            throw new IllegalArgumentException("season '" + id + "' needs a release stage");
        }
        if (window == null) {
            throw new IllegalArgumentException("season '" + id + "' needs a window");
        }
        effects = List.copyOf(effects);
    }

    /**
     * Builds the feature state {@link FeatureGate} evaluates this season under.
     *
     * <p>A season is not in the Togglz repository — its window comes from its own file — but it is
     * gated by the same three steps a flagged feature is, and this is what makes that possible
     * without a second implementation of any of them.
     *
     * @return a feature state carrying the kill switch, the stage and the window of this season
     */
    @Contract(value = "-> new", pure = true)
    public FeatureState toFeatureState() {
        FeatureState state = new FeatureState(new NamedFeature(this.id.toUpperCase(Locale.ROOT)), this.enabled);
        state.setParameter(FeatureGate.STAGE_PARAMETER, this.stage.id());
        return this.window.applyTo(state);
    }

    /**
     * Returns the effects of this season that have the given scope, in file order.
     *
     * @param scope the scope to filter by
     * @return the matching effects
     */
    @Contract(pure = true)
    public List<SeasonEffect> effects(SeasonEffect.Scope scope) {
        List<SeasonEffect> matching = new ArrayList<>(this.effects.size());
        for (SeasonEffect effect : this.effects) {
            if (effect.scope() == scope) {
                matching.add(effect);
            }
        }
        return List.copyOf(matching);
    }
}
