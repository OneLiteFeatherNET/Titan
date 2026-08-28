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

import net.onelitefeather.titan.common.feature.FeatureDecision;
import net.onelitefeather.titan.common.feature.FeatureGate;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Decides which seasons are running and keeps the world in step with that decision.
 *
 * <p>Three things live here and nowhere else, and all three are the stable part of the design —
 * they do not change when a season changes:
 *
 * <ol>
 * <li><b>Who decides.</b> Every question of the form "is this season on" goes to
 * {@link FeatureGate}. There is no second kill switch, no second stage check and no second
 * reading of the clock (US-4.07).</li>
 * <li><b>In which order.</b> Seasons are applied in ascending {@link SeasonDefinition#priority()},
 * so the highest priority is applied last and therefore wins. Two files with the same priority
 * fall back to the id. Nothing anywhere depends on the order the files were read in
 * (US-4.05).</li>
 * <li><b>What is undone.</b> {@link #synchronize(SeasonCanvas)} takes every running season back
 * out before putting the new set in, rather than trying to unwind one season out of the middle
 * of a stack of overlapping ones. That is slightly wasteful and completely predictable: the
 * world becomes a function of the set of live seasons and of nothing else.</li>
 * </ol>
 *
 * <p><b>What preview can and cannot do.</b> A player-scoped effect — a navigator icon, a message
 * prefix — is chosen while something is being shown to one person, so a preview holder sees it and
 * nobody else does; that is {@link #presentationFor(UUID)}. A world-scoped effect is a block in a
 * shared world, and no permission can put a block there for one player only. World effects
 * therefore follow {@link FeatureGate#decideForServer} — the kill switch and the window, decided
 * once for the lobby. Previewing decoration means starting a lobby with the season's window open,
 * which the release stage then keeps to the team; it does not mean walking into the live lobby and
 * seeing pumpkins nobody else sees.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class SeasonDirector {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeasonDirector.class);

    private final FeatureGate gate;
    private final List<SeasonDefinition> definitions;
    private final Map<String, ConfiguredSeason> contents = new LinkedHashMap<>();

    private SeasonDirector(FeatureGate gate, List<SeasonDefinition> definitions) {
        this.gate = gate;
        List<SeasonDefinition> sorted = new ArrayList<>(definitions);
        sorted.sort(SeasonDefinition.BY_PRIORITY);
        this.definitions = List.copyOf(sorted);
        for (SeasonDefinition definition : this.definitions) {
            this.contents.put(definition.id(), ConfiguredSeason.of(definition));
        }
    }

    /**
     * Creates a director over a fixed set of seasons.
     *
     * @param gate        the release gate every season is evaluated by
     * @param definitions the seasons, in any order
     * @return the director
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static SeasonDirector of(FeatureGate gate, List<SeasonDefinition> definitions) {
        return new SeasonDirector(gate, definitions);
    }

    /**
     * Creates a director over the seasons in a directory, or over no season at all when the
     * directory is not there (NFR-003).
     *
     * @param gate      the release gate every season is evaluated by
     * @param directory the directory season files are read from
     * @param zone      the zone a season file that names none is planned in
     * @return the director
     * @throws SeasonConfigurationException when a season file cannot be read
     */
    public static SeasonDirector load(FeatureGate gate, Path directory, ZoneId zone) {
        return new SeasonDirector(gate, SeasonLoader.create(zone).loadAll(directory));
    }

    /**
     * Returns every season that was loaded, in ascending priority order.
     *
     * @return the loaded seasons
     */
    @Contract(pure = true)
    public List<SeasonDefinition> definitions() {
        return this.definitions;
    }

    /**
     * Returns the seasons that are live for the lobby as a whole, in ascending priority order.
     *
     * @return the seasons whose world effects belong in the world right now
     */
    @Contract(pure = true)
    public List<SeasonDefinition> live() {
        List<SeasonDefinition> live = new ArrayList<>();
        for (SeasonDefinition definition : this.definitions) {
            if (this.gate.decideForServer(definition.toFeatureState()).isAllowed()) {
                live.add(definition);
            }
        }
        return List.copyOf(live);
    }

    /**
     * Returns the seasons one player may see, in ascending priority order. A holder of
     * {@link FeatureGate#PREVIEW_PERMISSION} also sees seasons whose window is shut.
     *
     * @param playerId the player's unique id
     * @return the seasons visible to that player
     */
    @Contract(pure = true)
    public List<SeasonDefinition> visibleTo(UUID playerId) {
        List<SeasonDefinition> visible = new ArrayList<>();
        for (SeasonDefinition definition : this.definitions) {
            if (decisionFor(definition, playerId).isAllowed()) {
                visible.add(definition);
            }
        }
        return List.copyOf(visible);
    }

    /**
     * Evaluates one season for one player and reports which step decided the outcome.
     *
     * @param definition the season to evaluate
     * @param playerId   the player's unique id
     * @return the gate's decision, including whether it rests on the preview permission
     */
    @Contract(pure = true)
    public FeatureDecision decisionFor(SeasonDefinition definition, UUID playerId) {
        return this.gate.decide(definition.toFeatureState(), playerId);
    }

    /**
     * Returns what the seasons this player may see do to what they are shown.
     *
     * @param playerId the player's unique id
     * @return the navigator icons and message prefix in force for that player
     */
    @Contract(pure = true)
    public SeasonPresentation presentationFor(UUID playerId) {
        return SeasonPresentation.of(visibleTo(playerId));
    }

    /**
     * Returns the world directory the winning live season asks the lobby to load.
     *
     * <p>Consuming this is the job of the world selection in spec stage 1; the director only
     * answers the question, because which world is loaded has to be decided once at startup and
     * cannot be changed underneath players who are standing in it.
     *
     * @return the world of the highest-priority live season that names one
     */
    @Contract(pure = true)
    public Optional<String> world() {
        String world = null;
        for (SeasonDefinition definition : live()) {
            if (definition.world() != null) {
                world = definition.world();
            }
        }
        return Optional.ofNullable(world);
    }

    /**
     * Brings the world in line with the seasons that are live right now.
     *
     * <p>Safe to call repeatedly — the lobby calls it on a timer, so a window that opens or a kill
     * switch that is thrown takes effect without a restart (NFR-004). When the live set has not
     * changed, nothing is touched.
     *
     * @param canvas the world to apply seasons to
     * @return whether anything changed
     */
    public boolean synchronize(SeasonCanvas canvas) {
        List<SeasonDefinition> live = live();
        List<String> wanted = live.stream().map(SeasonDefinition::id).toList();
        List<String> running = new ArrayList<>();
        for (SeasonDefinition definition : this.definitions) {
            if (content(definition.id()).active()) {
                running.add(definition.id());
            }
        }
        if (wanted.equals(running)) {
            return false;
        }
        LOGGER.info("Seasons changing from {} to {}", running.isEmpty() ? "none" : running, wanted.isEmpty() ? "none" : wanted);
        // Take everything down before putting anything up. Unwinding one season out of the middle
        // of an overlapping stack would restore whatever the season above it had written, which is
        // correct only by accident; a full rebuild is a few block writes a year and always right.
        deactivateAll();
        for (SeasonDefinition definition : live) {
            content(definition.id()).activate(canvas);
        }
        return true;
    }

    /**
     * Takes every running season back out of the world. Called on shutdown so a lobby that stops
     * mid-season does not leave its decoration in the world files.
     */
    public void deactivateAll() {
        List<SeasonDefinition> reversed = new ArrayList<>(this.definitions);
        Collections.reverse(reversed);
        for (SeasonDefinition definition : reversed) {
            content(definition.id()).deactivate();
        }
    }

    /**
     * Returns the content object of one season, for tests and for the smoke run of an archived
     * season.
     *
     * @param id the season id
     * @return the content
     * @throws IllegalArgumentException when no season with that id was loaded
     */
    @Contract(pure = true)
    public ConfiguredSeason content(String id) {
        ConfiguredSeason content = this.contents.get(id);
        if (content == null) {
            throw new IllegalArgumentException("no season with the id '" + id + "' is loaded");
        }
        return content;
    }
}
