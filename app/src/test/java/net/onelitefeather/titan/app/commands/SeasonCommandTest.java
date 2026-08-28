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
package net.onelitefeather.titan.app.commands;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.util.TriState;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.feature.FeatureAudience;
import net.onelitefeather.titan.common.feature.FeatureGate;
import net.onelitefeather.titan.common.feature.ReleaseStage;
import net.onelitefeather.titan.common.feature.SeasonWindowActivationStrategy;
import net.onelitefeather.titan.common.feature.TitanFeatures;
import net.onelitefeather.titan.common.season.SeasonDefinition;
import net.onelitefeather.titan.common.season.SeasonDirector;
import net.onelitefeather.titan.common.season.SeasonLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MicrotusExtension.class)
class SeasonCommandTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

    private InMemoryStateRepository repository;
    private FeatureGate gate;
    private SeasonCommand command;

    @BeforeEach
    void setUp() {
        this.repository = new InMemoryStateRepository();
        FeatureManager featureManager = new FeatureManagerBuilder().featureEnum(TitanFeatures.class).stateRepository(this.repository).userProvider(new NoOpUserProvider()).activationStrategyProvider(new DefaultActivationStrategyProvider()).build();
        FeatureGate gate = FeatureGate.with(featureManager, FeatureAudience.denyAll(), Clock.fixed(NOW, ZoneOffset.UTC), BERLIN);
        this.gate = gate;
        this.command = new SeasonCommand(gate, SeasonDirector.of(gate, List.of()));
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private String lineFor(TitanFeatures feature) {
        return this.command.statusLines().stream().map(SeasonCommandTest::plain).filter(candidate -> candidate.startsWith(feature.name())).findFirst().orElseThrow();
    }

    private static Player playerWith(Env env, Instance instance, boolean permitted) {
        Player player = spy(env.createPlayer(instance));
        doReturn(PermissionChecker.always(permitted ? TriState.TRUE : TriState.FALSE)).when(player).getOrDefault(eq(PermissionChecker.POINTER), any());
        return player;
    }

    @Test
    @DisplayName("the season list names every loaded season and whether it is live")
    void seasonListNamesEverySeasonAndWhetherItIsLive() {
        SeasonLoader loader = SeasonLoader.create(BERLIN);
        SeasonDefinition open = loader.parse("open", """
                { "id": "open", "priority": 5, "stage": "ga", "world": "lantern-nights",
                  "window": { "from": "2026-10-01", "to": "2026-11-05", "zone": "Europe/Berlin" } }
                """);
        SeasonDefinition later = loader.parse("later", """
                { "id": "later", "priority": 9, "stage": "internal",
                  "window": { "from": "2026-12-01", "to": "2026-12-27", "zone": "Europe/Berlin" } }
                """);
        SeasonCommand listing = new SeasonCommand(this.gate, SeasonDirector.of(this.gate, List.of(later, open)));

        List<String> lines = listing.seasonLines().stream().map(SeasonCommandTest::plain).toList();

        assertEquals(3, lines.size());
        assertTrue(lines.getFirst().contains("Seasons (2)"), lines.getFirst());
        assertTrue(lines.get(1).startsWith("open"), "lowest priority first, whatever order they were handed over in: " + lines);
        assertTrue(lines.get(1).contains("priority 5"), lines.get(1));
        assertTrue(lines.get(1).contains("world lantern-nights"), lines.get(1));
        assertTrue(lines.get(1).endsWith("live"), lines.get(1));
        assertTrue(lines.get(2).startsWith("later"), lines.get(2));
        assertTrue(lines.get(2).endsWith("not live"), lines.get(2));
    }

    @Test
    @DisplayName("a lobby with no seasons says so instead of printing an empty list")
    void seasonListSaysWhenNoSeasonIsInstalled() {
        List<String> lines = this.command.seasonLines().stream().map(SeasonCommandTest::plain).toList();

        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains("No seasons are installed"), lines.get(1));
    }

    @Test
    @DisplayName("only holders of titan.feature.internal may run the command")
    void onlyTheTeamMayRunTheCommand(Env env) {
        CommandCondition condition = this.command.getCondition();
        assertNotNull(condition);
        Instance instance = env.createFlatInstance();

        assertTrue(condition.canUse(playerWith(env, instance, true), null));
        assertFalse(condition.canUse(playerWith(env, instance, false), null));
    }

    @Test
    @DisplayName("the server console may always run the command")
    void theConsoleMayAlwaysRunTheCommand() {
        assertTrue(this.command.getCondition().canUse(mock(CommandSender.class), null));
    }

    @Test
    @DisplayName("the status lists every feature with stage, window and kill switch")
    void statusListsStageWindowAndKillSwitch() {
        this.repository.setFeatureState(new FeatureState(TitanFeatures.NAVIGATOR_ELYTRA, true).setStrategyId(SeasonWindowActivationStrategy.ID).setParameter(FeatureGate.STAGE_PARAMETER, ReleaseStage.LITE.id()).setParameter(SeasonWindowActivationStrategy.PARAM_FROM, "2026-10-01").setParameter(SeasonWindowActivationStrategy.PARAM_TO, "2026-11-05"));

        List<Component> lines = this.command.statusLines();

        assertEquals(TitanFeatures.values().length + 1, lines.size());
        assertTrue(plain(lines.getFirst()).contains("Feature rollout (" + TitanFeatures.values().length + ")"));
        String elytra = lines.stream().map(SeasonCommandTest::plain).filter(line -> line.startsWith(TitanFeatures.NAVIGATOR_ELYTRA.name())).findFirst().orElseThrow();
        assertTrue(elytra.contains("stage lite"), elytra);
        assertTrue(elytra.contains("2026-10-01 00:00 to 2026-11-05 00:00 (Europe/Berlin, open)"), elytra);
        assertTrue(elytra.contains("kill switch off"), elytra);
    }

    @Test
    @DisplayName("an unreadable window is printed as unreadable, not as 'always'")
    void unreadableWindowIsPrintedAsUnreadable() {
        this.repository.setFeatureState(new FeatureState(TitanFeatures.NAVIGATOR_ELYTRA, true).setStrategyId(SeasonWindowActivationStrategy.ID).setParameter(FeatureGate.STAGE_PARAMETER, ReleaseStage.GA.id()).setParameter(SeasonWindowActivationStrategy.PARAM_FROM, "1. Oktober"));

        String line = lineFor(TitanFeatures.NAVIGATOR_ELYTRA);

        // The gate denies everyone; the status has to point at the typo instead of claiming the
        // feature runs unbounded.
        assertTrue(line.contains("window unreadable"), line);
        assertTrue(line.contains("1. Oktober"), line);
        assertFalse(line.contains("window always"), line);
    }

    @Test
    @DisplayName("an unknown stage id is printed next to the stage that was applied instead")
    void unknownStageIsPrinted() {
        this.repository.setFeatureState(new FeatureState(TitanFeatures.NAVIGATOR_ELYTRA, true).setParameter(FeatureGate.STAGE_PARAMETER, "intern"));

        String line = lineFor(TitanFeatures.NAVIGATOR_ELYTRA);

        assertTrue(line.contains("stage internal"), line);
        assertTrue(line.contains("'intern' is not internal, lite or ga"), line);
    }

    @Test
    @DisplayName("a switched off feature without a window is reported as such")
    void switchedOffFeatureIsReported() {
        this.repository.setFeatureState(new FeatureState(TitanFeatures.NAVIGATOR_SLENDER, false).setParameter(FeatureGate.STAGE_PARAMETER, ReleaseStage.GA.id()));

        String line = this.command.statusLines().stream().map(SeasonCommandTest::plain).filter(candidate -> candidate.startsWith(TitanFeatures.NAVIGATOR_SLENDER.name())).findFirst().orElseThrow();

        assertTrue(line.contains("stage ga"), line);
        assertTrue(line.contains("window always"), line);
        assertTrue(line.contains("kill switch engaged"), line);
    }
}
