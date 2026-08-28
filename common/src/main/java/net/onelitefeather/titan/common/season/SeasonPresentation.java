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

import net.minestom.server.item.Material;
import org.jetbrains.annotations.Contract;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the seasons a particular player is allowed to see do to what that player is shown — today,
 * the navigator icons that have been swapped.
 *
 * <p>This is the half of a season that is computed per viewer rather than written into the world,
 * and it is the half preview actually works on (US-4.07). A team member holding
 * {@link net.onelitefeather.titan.common.feature.FeatureGate#PREVIEW_PERMISSION} gets a
 * presentation built from seasons whose window has not opened, while everybody else gets the
 * presentation of the seasons that are live — with no extra permission check anywhere, because the
 * filtering already happened in the gate.
 *
 * <p>Overlap is resolved by priority and only by priority (US-4.05):
 * {@link #of(List)} folds the seasons in ascending priority, so the highest-priority season is the
 * last to write and therefore the one that wins. Load order plays no part; the caller hands the
 * list over already ordered by {@link SeasonDefinition#BY_PRIORITY}.
 *
 * @param icons the navigator destinations whose icon a season replaced, and the material it took
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public record SeasonPresentation(Map<String, Material> icons) {

    private static final SeasonPresentation NONE = new SeasonPresentation(Map.of());

    /**
     * Copies the icon map so a presentation cannot be changed after the fact.
     */
    public SeasonPresentation {
        icons = Map.copyOf(icons);
    }

    /**
     * Returns the presentation of a lobby with no season running: nothing swapped, no prefix.
     *
     * @return the empty presentation
     */
    @Contract(pure = true)
    public static SeasonPresentation none() {
        return NONE;
    }

    /**
     * Folds the player-scoped effects of the given seasons into one presentation.
     *
     * <p>The switch is exhaustive over the sealed {@link SeasonEffect} hierarchy and has no
     * {@code default}, so a new effect record does not compile until it has been decided whether it
     * changes what a player is shown (US-4.04).
     *
     * @param seasons the seasons visible to one player, in ascending priority order
     * @return the presentation for that player
     */
    @Contract(pure = true)
    public static SeasonPresentation of(List<SeasonDefinition> seasons) {
        if (seasons.isEmpty()) {
            return NONE;
        }
        Map<String, Material> icons = new LinkedHashMap<>();
        for (SeasonDefinition season : seasons) {
            for (SeasonEffect effect : season.effects(SeasonEffect.Scope.PLAYER)) {
                switch (effect) {
                    case SeasonEffect.ReplaceIcon icon -> {
                        Material material = Material.fromKey(icon.material());
                        if (material != null) {
                            icons.put(icon.destination(), material);
                        }
                    }
                    // World effects are placed once for everybody by ConfiguredSeason; there is
                    // nothing per player to compute for them.
                    case SeasonEffect.PlaceDecoration ignored -> {
                    }
                    case SeasonEffect.PlaceDisplay ignored -> {
                    }
                    case SeasonEffect.AmbientSound ignored -> {
                    }
                    case SeasonEffect.MessagePrefix ignored -> {
                    }
                }
            }
        }
        return new SeasonPresentation(icons);
    }

    /**
     * Returns the material a season put on the icon of a navigator destination.
     *
     * @param destination the navigator destination, for example {@code Survival}
     * @return the seasonal material, or an empty optional when no season touched this destination
     */
    @Contract(pure = true)
    public Optional<Material> icon(String destination) {
        return Optional.ofNullable(this.icons.get(destination));
    }

    /**
     * Returns whether any season changed anything about what a player is shown.
     *
     * @return whether this presentation differs from {@link #none()}
     */
    @Contract(pure = true)
    public boolean isEmpty() {
        return this.icons.isEmpty();
    }
}
