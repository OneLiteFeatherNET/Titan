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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.TriState;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.common.feature.FeatureGate;
import net.onelitefeather.titan.common.feature.FeatureStatus;
import net.onelitefeather.titan.common.feature.ReleaseStage;
import net.onelitefeather.titan.common.season.SeasonDefinition;
import net.onelitefeather.titan.common.season.SeasonDirector;
import org.jetbrains.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows the rollout state of every feature to the team: release stage, time window and kill switch
 * per feature (US-3.08).
 *
 * <p>Togglz ships an admin console, but it is a servlet application; a Minestom process has no
 * servlet container, so a command is what replaces it. The command is bound to
 * {@value ReleaseStage#INTERNAL_PERMISSION} — the same permission that defines the internal
 * audience — and, like {@code /stop}, is always available from the server console.
 *
 * <p>{@code /season list} does the same for the seasons in the {@code seasons} directory. They are
 * not Togglz features - a season's window lives in its own file - so they would otherwise be
 * invisible to the one command whose job is to spare an operator a trip to the log.
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 1.15.0
 */
public final class SeasonCommand extends Command {

    private static final DateTimeFormatter WINDOW_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FeatureGate featureGate;
    private final SeasonDirector seasons;

    /**
     * Creates the command.
     *
     * @param featureGate the gate the status is read from
     * @param seasons     the seasons that were loaded from the season directory
     */
    public SeasonCommand(FeatureGate featureGate, SeasonDirector seasons) {
        super("season");
        this.featureGate = featureGate;
        this.seasons = seasons;
        setCondition(SeasonCommand::canUse);
        setDefaultExecutor((sender, context) -> sender.sendMessage(
                Component.text("Usage: /season status | /season list", NamedTextColor.RED)));
        addSyntax((sender, context) -> sendStatus(sender), ArgumentType.Literal("status"));
        addSyntax((sender, context) -> sendSeasons(sender), ArgumentType.Literal("list"));
    }

    /**
     * Renders one season as a single chat line: id, priority, stage, window and kill switch.
     *
     * @param definition the season to render
     * @param live       whether the season's world effects are in the world right now
     * @return the line shown to the sender
     */
    static Component describe(SeasonDefinition definition, boolean live) {
        Component line = Component.text(definition.id(), NamedTextColor.WHITE).append(Component.text(" | priority ", NamedTextColor.DARK_GRAY)).append(Component.text(definition.priority(), NamedTextColor.AQUA)).append(Component.text(" | stage ", NamedTextColor.DARK_GRAY)).append(Component.text(definition.stage().id(), stageColor(definition.stage())));
        String from = definition.window().from() == null ? "-∞" : WINDOW_FORMAT.format(definition.window().from());
        String to = definition.window().to() == null ? "∞" : WINDOW_FORMAT.format(definition.window().to());
        line = line.append(Component.text(" | window ", NamedTextColor.DARK_GRAY)).append(Component.text(from + " to " + to + " (" + definition.window().zone().getId() + ")", live ? NamedTextColor.GREEN : NamedTextColor.GOLD));
        if (definition.world() != null) {
            line = line.append(Component.text(" | world ", NamedTextColor.DARK_GRAY)).append(Component.text(definition.world(), NamedTextColor.WHITE));
        }
        line = line.append(Component.text(" | kill switch ", NamedTextColor.DARK_GRAY)).append(definition.enabled() ? Component.text("off", NamedTextColor.GREEN) : Component.text("engaged", NamedTextColor.RED));
        return line.append(Component.text(live ? " | live" : " | not live", live ? NamedTextColor.GREEN : NamedTextColor.GRAY));
    }

    /**
     * Builds the lines {@code /season list} prints: one header plus one line per loaded season.
     *
     * @return the rendered list, in the order the seasons are applied
     */
    List<Component> seasonLines() {
        List<SeasonDefinition> definitions = this.seasons.definitions();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("Seasons (" + definitions.size() + "), lowest priority first", NamedTextColor.YELLOW));
        if (definitions.isEmpty()) {
            lines.add(Component.text("No seasons are installed; the lobby runs without seasonal content.", NamedTextColor.GRAY));
            return List.copyOf(lines);
        }
        List<SeasonDefinition> live = this.seasons.live();
        for (SeasonDefinition definition : definitions) {
            lines.add(describe(definition, live.contains(definition)));
        }
        return List.copyOf(lines);
    }

    private void sendSeasons(CommandSender sender) {
        seasonLines().forEach(sender::sendMessage);
    }

    /**
     * Renders one feature as a single chat line: name, stage, window and kill switch.
     *
     * @param status the feature status to render
     * @return the line shown to the sender
     */
    static Component describe(FeatureStatus status) {
        Component line = Component.text(status.feature(), NamedTextColor.WHITE).append(Component.text(" | stage ", NamedTextColor.DARK_GRAY)).append(describeStage(status)).append(Component.text(" | window ", NamedTextColor.DARK_GRAY)).append(describeWindow(status));
        return line.append(Component.text(" | kill switch ", NamedTextColor.DARK_GRAY)).append(status.killSwitchEngaged() ? Component.text("engaged", NamedTextColor.RED) : Component.text("off", NamedTextColor.GREEN));
    }

    private static Component describeStage(FeatureStatus status) {
        Component stage = Component.text(status.stage().id(), stageColor(status.stage()));
        if (status.stageReadable()) {
            return stage;
        }
        // The gate fell back to the narrowest stage. Say so, and name the value that was written:
        // "intern" and "premium" are both plausible typos for the ids this project actually uses.
        return stage.append(Component.text(" (unreadable: '" + status.unknownStage() + "' is not internal, lite or ga)", NamedTextColor.RED));
    }

    private static Component describeWindow(FeatureStatus status) {
        if (!status.windowReadable()) {
            // Never print "always" here: the gate is denying everyone, and a status that says the
            // feature runs unbounded would send the operator looking in the wrong place.
            return Component.text("unreadable: " + status.windowProblem(), NamedTextColor.RED);
        }
        if (!status.hasWindow()) {
            return Component.text("always", NamedTextColor.GRAY);
        }
        String from = status.from() == null ? "-∞" : WINDOW_FORMAT.format(status.from());
        String to = status.to() == null ? "∞" : WINDOW_FORMAT.format(status.to());
        NamedTextColor color = status.withinWindow() ? NamedTextColor.GREEN : NamedTextColor.GOLD;
        return Component.text(from + " to " + to + " (" + status.zone().getId() + ", ", color).append(Component.text(status.withinWindow() ? "open)" : "closed)", color));
    }

    private static NamedTextColor stageColor(ReleaseStage stage) {
        return switch (stage) {
            case INTERNAL -> NamedTextColor.RED;
            case LITE -> NamedTextColor.GOLD;
            case GA -> NamedTextColor.GREEN;
        };
    }

    private static boolean canUse(CommandSender sender, @Nullable String commandString) {
        if (!(sender instanceof Player)) {
            return true;
        }
        return sender.getOrDefault(PermissionChecker.POINTER, PermissionChecker.always(TriState.FALSE)).test(ReleaseStage.INTERNAL_PERMISSION);
    }

    /**
     * Builds the lines {@code /season status} prints: one header plus one line per feature.
     *
     * @return the rendered status, in the order the gate reports the features
     */
    List<Component> statusLines() {
        List<FeatureStatus> statuses = this.featureGate.statuses();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("Feature rollout (" + statuses.size() + ")", NamedTextColor.YELLOW));
        if (statuses.isEmpty()) {
            lines.add(Component.text("No features are registered.", NamedTextColor.GRAY));
            return List.copyOf(lines);
        }
        for (FeatureStatus status : statuses) {
            lines.add(describe(status));
        }
        return List.copyOf(lines);
    }

    private void sendStatus(CommandSender sender) {
        statusLines().forEach(sender::sendMessage);
    }
}
