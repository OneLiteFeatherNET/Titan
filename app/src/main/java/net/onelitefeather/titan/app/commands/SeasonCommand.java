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
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class SeasonCommand extends Command {

    private static final DateTimeFormatter WINDOW_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FeatureGate featureGate;

    /**
     * Creates the command.
     *
     * @param featureGate the gate the status is read from
     */
    public SeasonCommand(FeatureGate featureGate) {
        super("season");
        this.featureGate = featureGate;
        setCondition(SeasonCommand::canUse);
        setDefaultExecutor((sender, context) -> sender.sendMessage(
                Component.text("Usage: /season status", NamedTextColor.RED)));
        addSyntax((sender, context) -> sendStatus(sender), ArgumentType.Literal("status"));
    }

    /**
     * Renders one feature as a single chat line: name, stage, window and kill switch.
     *
     * @param status the feature status to render
     * @return the line shown to the sender
     */
    static Component describe(FeatureStatus status) {
        Component line = Component.text(status.feature(), NamedTextColor.WHITE).append(Component.text(" | stage ", NamedTextColor.DARK_GRAY)).append(Component.text(status.stage().id(), stageColor(status.stage()))).append(Component.text(" | window ", NamedTextColor.DARK_GRAY)).append(describeWindow(status));
        return line.append(Component.text(" | kill switch ", NamedTextColor.DARK_GRAY)).append(status.killSwitchEngaged() ? Component.text("engaged", NamedTextColor.RED) : Component.text("off", NamedTextColor.GREEN));
    }

    private static Component describeWindow(FeatureStatus status) {
        if (!status.hasWindow()) {
            return Component.text("always", NamedTextColor.GRAY);
        }
        String from = status.from() == null ? "-∞" : WINDOW_FORMAT.format(status.from());
        String to = status.to() == null ? "∞" : WINDOW_FORMAT.format(status.to());
        return Component.text(from + " to " + to + " (" + status.zone().getId() + ", ", status.withinWindow() ? NamedTextColor.GREEN : NamedTextColor.GOLD).append(Component.text(status.withinWindow() ? "open)" : "closed)", status.withinWindow() ? NamedTextColor.GREEN : NamedTextColor.GOLD));
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
