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

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * US-4.08: every season the repository ships is run end to end, every build.
 *
 * <p>The requirement comes from a specific and repeated failure. Hypixel shipped the same
 * Santa-Says bug two Decembers running, because a season that lies dark for eleven months is
 * exercised by nothing — not by the people playing, not by the people building the next feature,
 * and not by a test suite that only ever covers the code somebody is currently touching.
 *
 * <p>So this test does not test a season. It tests <em>every</em> season in the {@code seasons}
 * directory, by walking the same core path the lobby walks: read the file, put the season into a
 * running world, look at the world, take the season back out, look again. A season added next year
 * is covered by it the moment its file is committed — which is the only version of "a test case per
 * pack" that survives contact with a small team.
 *
 * <p>It also fails when a season's end date is more than twelve months in the past (NFR-010). That
 * is not tidiness either: an expired season is exactly the code that will be re-activated one day
 * by somebody who assumes it still works.
 */
@ExtendWith(MicrotusExtension.class)
class SeasonSmokeTest {

    /** How far past its end a season may sit before the build insists somebody looks at it. */
    private static final int STALE_AFTER_MONTHS = 12;

    @Test
    @DisplayName("every shipped season loads, applies itself to a world, and takes itself back out")
    void everyShippedSeasonSurvivesActivationAndDeactivation(Env env) {
        List<SeasonDefinition> seasons = shipped();
        Assertions.assertFalse(seasons.isEmpty(), "the repository ships at least the example season; if that changed, this test has stopped covering anything");

        List<Executable> checks = new ArrayList<>();
        for (SeasonDefinition season : seasons) {
            checks.add(() -> smoke(env, season));
        }
        assertAll(checks);
    }

    @Test
    @DisplayName("no shipped season is more than a year past its end date")
    void noShippedSeasonHasGoneStale() {
        // Deliberately the real clock. The point is that this build, run today, notices that a
        // season has been sitting unreviewed since last year.
        LocalDateTime staleBefore = LocalDate.now().atStartOfDay().minusMonths(STALE_AFTER_MONTHS);
        List<String> stale = new ArrayList<>();
        for (SeasonDefinition season : shipped()) {
            LocalDateTime end = season.window().to();
            if (end != null && end.isBefore(staleBefore)) {
                stale.add(season.id() + " ended at " + end);
            }
        }
        assertEquals(List.of(), stale, "a season more than " + STALE_AFTER_MONTHS + " months past its end has to be reviewed and re-dated, or deleted");
    }

    /**
     * Walks one season through the path it will be walked through when somebody reactivates it: put
     * it into a world, check the world actually changed, take it back out, check the world is back
     * to what it was, and check that the per-player half of it produces something for a viewer who
     * may see it.
     */
    private static void smoke(Env env, SeasonDefinition season) {
        Instance instance = env.createFlatInstance();
        RecordingSeasonCanvas canvas = new RecordingSeasonCanvas(MinestomSeasonCanvas.of(instance));
        ConfiguredSeason content = ConfiguredSeason.of(season);

        Map<Pos, Block> before = new LinkedHashMap<>();
        for (SeasonEffect effect : season.effects(SeasonEffect.Scope.WORLD)) {
            if (effect instanceof SeasonEffect.PlaceDecoration decoration) {
                // Through the canvas, which loads the chunk first: at startup nothing is loaded
                // yet, and reading straight from the instance would throw before the season ran.
                before.put(decoration.position(), canvas.blockAt(decoration.position()));
            }
        }
        long displaysBefore = displays(instance);

        content.activate(canvas);
        env.tick();

        assertTrue(content.active(), season.id() + " reports itself inactive after being activated");
        for (SeasonEffect effect : season.effects(SeasonEffect.Scope.WORLD)) {
            if (effect instanceof SeasonEffect.PlaceDecoration decoration) {
                Block placed = instance.getBlock(decoration.position());
                assertTrue(placed.compare(resolve(season, decoration.block())), season.id() + " did not place " + decoration.block().asString() + " at " + decoration.position());
            }
        }
        long expectedDisplays = season.effects(SeasonEffect.Scope.WORLD).stream().filter(SeasonEffect.PlaceDisplay.class::isInstance).count();
        assertEquals(displaysBefore + expectedDisplays, displays(instance), season.id() + " did not spawn all of its displays");

        content.deactivate();
        env.tick();

        assertFalse(content.active(), season.id() + " reports itself active after being deactivated");
        for (Map.Entry<Pos, Block> entry : before.entrySet()) {
            assertTrue(instance.getBlock(entry.getKey()).compare(entry.getValue()), season.id() + " left " + instance.getBlock(entry.getKey()).key().asString() + " behind at " + entry.getKey() + "; expected " + entry.getValue().key().asString());
        }
        assertEquals(displaysBefore, displays(instance), season.id() + " left a display behind");
        for (SeasonCanvas.Handle handle : canvas.handles()) {
            assertFalse(handle.alive(), season.id() + " left a scheduled task running");
        }

        // The per-player half: whatever the file promised has to actually come out the other end.
        SeasonPresentation presentation = SeasonPresentation.of(List.of(season));
        assertEquals(season.effects(SeasonEffect.Scope.PLAYER).isEmpty(), presentation.isEmpty(), season.id() + " declares player-facing effects that produce nothing");
    }

    private static Block resolve(SeasonDefinition season, net.kyori.adventure.key.Key key) {
        Block block = Block.fromKey(key);
        if (block == null) {
            return fail(season.id() + " names the unknown block " + key.asString() + "; the loader should have refused it");
        }
        return block;
    }

    private static long displays(Instance instance) {
        return instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.TEXT_DISPLAY).count();
    }

    /**
     * Reads the seasons this repository ships, from the {@code seasons} directory at its root.
     *
     * <p>Found by walking up from the working directory rather than hard-coded, because the working
     * directory of a Gradle test is the module and not the repository.
     */
    private static List<SeasonDefinition> shipped() {
        return SeasonLoader.create(SeasonFixtures.BERLIN).loadAll(repositoryRoot().resolve(SeasonLoader.DIRECTORY));
    }

    private static Path repositoryRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve("settings.gradle.kts"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        return fail("could not find the repository root from " + Path.of("").toAbsolutePath());
    }
}
