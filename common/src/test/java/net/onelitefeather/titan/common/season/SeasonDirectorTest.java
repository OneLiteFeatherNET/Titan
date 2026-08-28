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
import net.onelitefeather.titan.common.feature.FeatureDecision;
import net.onelitefeather.titan.common.feature.FeatureGate;
import net.onelitefeather.titan.common.feature.ReleaseStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers US-4.05 (priority beats load order) and US-4.07 (preview goes through the gate).
 */
class SeasonDirectorTest {

    private static final UUID TEAM = UUID.randomUUID();
    private static final UUID PLAYER = UUID.randomUUID();

    /** Open at {@link SeasonFixtures#NOW}. */
    private static final String OPEN_FROM = "2026-10-01";
    private static final String OPEN_TO = "2026-11-05";

    /** Shut at {@link SeasonFixtures#NOW} - it has not started yet. */
    private static final String FUTURE_FROM = "2026-12-01";
    private static final String FUTURE_TO = "2026-12-27";

    private final SeasonLoader loader = SeasonLoader.create(SeasonFixtures.BERLIN);

    @Test
    @DisplayName("where two seasons overlap, the higher priority wins - whatever order they arrived in")
    void higherPriorityWinsRegardlessOfLoadOrder() {
        SeasonDefinition low = season("low", 1, OPEN_FROM, OPEN_TO, "minecraft:stone");
        SeasonDefinition high = season("high", 9, OPEN_FROM, OPEN_TO, "minecraft:carved_pumpkin");

        SeasonPresentation forwards = director(List.of(low, high)).presentationFor(PLAYER);
        SeasonPresentation backwards = director(List.of(high, low)).presentationFor(PLAYER);

        assertEquals(Material.CARVED_PUMPKIN, forwards.icon("Survival").orElseThrow());
        assertEquals(forwards.icons(), backwards.icons(), "the result must not depend on the order the files were read in");
    }

    @Test
    @DisplayName("every permutation of the same seasons produces the same order")
    void everyPermutationProducesTheSameOrder() {
        List<SeasonDefinition> seasons = new ArrayList<>(List.of(season("a", 5, OPEN_FROM, OPEN_TO, "minecraft:stone"), season("b", 1, OPEN_FROM, OPEN_TO, "minecraft:dirt"), season("c", 9, OPEN_FROM, OPEN_TO, "minecraft:carved_pumpkin")));
        List<String> expected = director(seasons).live().stream().map(SeasonDefinition::id).toList();

        for (int shuffle = 0; shuffle < 20; shuffle++) {
            Collections.shuffle(seasons);
            assertEquals(expected, director(seasons).live().stream().map(SeasonDefinition::id).toList());
        }
        assertEquals(List.of("b", "a", "c"), expected, "ascending priority, so the highest is applied last");
    }

    @Test
    @DisplayName("two seasons with the same priority fall back to the id, never to load order")
    void equalPrioritiesFallBackToTheId() {
        List<SeasonDefinition> seasons = new ArrayList<>(List.of(season("zulu", 3, OPEN_FROM, OPEN_TO, "minecraft:stone"), season("alpha", 3, OPEN_FROM, OPEN_TO, "minecraft:dirt")));

        assertEquals(List.of("alpha", "zulu"), director(seasons).live().stream().map(SeasonDefinition::id).toList());
        Collections.reverse(seasons);
        assertEquals(List.of("alpha", "zulu"), director(seasons).live().stream().map(SeasonDefinition::id).toList());
    }

    @Test
    @DisplayName("a season whose window has not opened is invisible to a player")
    void aShutSeasonIsInvisible() {
        SeasonDirector director = director(List.of(season("future", 1, FUTURE_FROM, FUTURE_TO, "minecraft:carved_pumpkin")));

        assertEquals(List.of(), director.visibleTo(PLAYER));
        assertTrue(director.presentationFor(PLAYER).isEmpty());
        assertSame(SeasonPresentation.none(), director.presentationFor(PLAYER));
    }

    @Test
    @DisplayName("a preview holder sees a season whose window has not opened")
    void aPreviewHolderSeesAShutSeason() {
        SeasonFixtures.MutableAudience audience = new SeasonFixtures.MutableAudience();
        audience.grant(TEAM, FeatureGate.PREVIEW_PERMISSION);
        SeasonDefinition future = season("future", 1, FUTURE_FROM, FUTURE_TO, "minecraft:carved_pumpkin");
        SeasonDirector director = SeasonDirector.of(SeasonFixtures.gate(audience), List.of(future));

        assertEquals(List.of(future), director.visibleTo(TEAM));
        assertEquals(Material.CARVED_PUMPKIN, director.presentationFor(TEAM).icon("Survival").orElseThrow());
        assertEquals(List.of(), director.visibleTo(PLAYER), "and nobody else does");
    }

    @Test
    @DisplayName("preview is reported as preview, so nobody mistakes it for the season being live")
    void previewIsReportedAsPreview() {
        SeasonFixtures.MutableAudience audience = new SeasonFixtures.MutableAudience();
        audience.grant(TEAM, FeatureGate.PREVIEW_PERMISSION);
        SeasonDefinition future = season("future", 1, FUTURE_FROM, FUTURE_TO, "minecraft:carved_pumpkin");
        SeasonDirector director = SeasonDirector.of(SeasonFixtures.gate(audience), List.of(future));

        assertEquals(FeatureDecision.ALLOWED_PREVIEW, director.decisionFor(future, TEAM));
        assertEquals(FeatureDecision.DENIED_WINDOW, director.decisionFor(future, PLAYER));
        assertFalse(director.live().contains(future), "preview is a permission, not a way to start the season early");
    }

    @Test
    @DisplayName("preview widens the window and nothing else - a killed season stays killed")
    void previewDoesNotBeatTheKillSwitch() {
        SeasonFixtures.MutableAudience audience = new SeasonFixtures.MutableAudience();
        audience.grant(TEAM, FeatureGate.PREVIEW_PERMISSION);
        SeasonDefinition killed = new SeasonDefinition("killed", false, 1, ReleaseStage.GA, null, season("killed", 1, FUTURE_FROM, FUTURE_TO, "minecraft:stone").window(), List.of());

        assertEquals(FeatureDecision.DENIED_KILL_SWITCH, SeasonDirector.of(SeasonFixtures.gate(audience), List.of(killed)).decisionFor(killed, TEAM));
    }

    @Test
    @DisplayName("preview does not admit somebody the release stage excludes")
    void previewDoesNotWidenTheReleaseStage() {
        SeasonFixtures.MutableAudience audience = new SeasonFixtures.MutableAudience();
        audience.grant(PLAYER, FeatureGate.PREVIEW_PERMISSION);
        SeasonDefinition internal = new SeasonDefinition("internal-only", true, 1, ReleaseStage.INTERNAL, null, season("internal-only", 1, FUTURE_FROM, FUTURE_TO, "minecraft:stone").window(), List.of());

        assertEquals(FeatureDecision.DENIED_STAGE, SeasonDirector.of(SeasonFixtures.gate(audience), List.of(internal)).decisionFor(internal, PLAYER));
    }

    @Test
    @DisplayName("a switched off season is live for nobody, whatever its window says")
    void theKillSwitchBeatsAnOpenWindow() {
        SeasonDefinition killed = new SeasonDefinition("killed", false, 1, ReleaseStage.GA, null, season("killed", 1, OPEN_FROM, OPEN_TO, "minecraft:stone").window(), List.of());

        assertEquals(List.of(), director(List.of(killed)).live());
    }

    @Test
    @DisplayName("the world of the highest-priority live season is the one that is asked for")
    void theWinningSeasonNamesTheWorld() {
        SeasonDefinition low = new SeasonDefinition("low", true, 1, ReleaseStage.GA, "autumn", season("low", 1, OPEN_FROM, OPEN_TO, "minecraft:stone").window(), List.of());
        SeasonDefinition high = new SeasonDefinition("high", true, 9, ReleaseStage.GA, "winter", season("high", 9, OPEN_FROM, OPEN_TO, "minecraft:stone").window(), List.of());

        assertEquals("winter", director(List.of(low, high)).world().orElseThrow());
        assertEquals("winter", director(List.of(high, low)).world().orElseThrow());
    }

    @Test
    @DisplayName("with no seasons installed the director answers everything with nothing")
    void noSeasonsIsAValidState() {
        SeasonDirector director = director(List.of());

        assertEquals(List.of(), director.live());
        assertEquals(List.of(), director.visibleTo(PLAYER));
        assertTrue(director.presentationFor(PLAYER).isEmpty());
        assertTrue(director.world().isEmpty());
    }

    private SeasonDirector director(List<SeasonDefinition> seasons) {
        return SeasonDirector.of(SeasonFixtures.gate(new SeasonFixtures.MutableAudience()), seasons);
    }

    private SeasonDefinition season(String id, int priority, String from, String to, String material) {
        return this.loader.parse(id, SeasonFixtures.seasonJson(id, priority, from, to, material));
    }
}
