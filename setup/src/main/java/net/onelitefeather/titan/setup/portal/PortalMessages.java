/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.setup.portal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import net.onelitefeather.titan.core.portal.PortalProblem;
import net.onelitefeather.titan.core.portal.PortalShape;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Chat text of the portal editor, in the style of {@code MapCommand}: English MiniMessage with
 * {@code <prefix>}, green for success, red for errors. Text a player typed (task, permission) or
 * that came from a map file goes in as {@code unparsed}, so tags in it are never evaluated.
 */
public final class PortalMessages {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private PortalMessages() {
    }

    public static Component render(PortalEditResult result) {
        return switch (result) {
            case PortalEditResult.Saved saved -> saved("Saved", saved.portal());
            case PortalEditResult.Updated updated -> saved("Updated", updated.portal());
            case PortalEditResult.Pending pending ->
                MINI.deserialize("<prefix> <yellow>Portal <id> is not complete yet, missing: <missing>.", Placeholder.unparsed("id", pending.id()), Placeholder.unparsed("missing", describeMissing(pending.missing())));
            case PortalEditResult.Complete complete -> complete(complete.id());
            case PortalEditResult.LabelUpdated updated -> labelUpdated(updated);
            case PortalEditResult.Removed removed ->
                MINI.deserialize("<prefix> <green>Removed portal <id>.", Placeholder.unparsed("id", removed.id()));
            case PortalEditResult.Rejected rejected -> rejected(rejected);
            case PortalEditResult.Invalid invalid ->
                MINI.deserialize("<prefix> <red><reason>", Placeholder.unparsed("reason", capitalised(invalid.reason())));
            case PortalEditResult.Cancelled cancelled ->
                MINI.deserialize("<prefix> <green>Discarded the draft of portal <id>.", Placeholder.unparsed("id", cancelled.id()));
            case PortalEditResult.Unknown unknown ->
                MINI.deserialize("<prefix> <red>There is no portal or draft <id>.", Placeholder.unparsed("id", unknown.id()));
        };
    }

    /**
     * Saved portals first, then, separated, the player's open drafts with what each still needs.
     */
    public static Component list(List<Portal> portals, List<PortalDraft> drafts) {
        if (portals.isEmpty() && drafts.isEmpty()) {
            return MINI.deserialize("<prefix> <yellow>There are no portals and no drafts.");
        }
        List<Component> lines = new ArrayList<>();
        if (!portals.isEmpty()) {
            lines.add(MINI.deserialize("<prefix> <green>Portals (<count>):", Placeholder.unparsed("count", String.valueOf(portals.size()))));
            for (Portal portal : portals) {
                lines.add(MINI.deserialize("<gray> - <white><id></white>: <shape>, task <task>, permission <permission><label>", portalTags(portal)));
            }
        }
        if (!drafts.isEmpty()) {
            lines.add(MINI.deserialize("<prefix> <yellow>Drafts (<count>):", Placeholder.unparsed("count", String.valueOf(drafts.size()))));
            for (PortalDraft draft : drafts) {
                String state = draft.complete() ? "complete, not saved" : "missing " + describeMissing(draft.missing());
                lines.add(MINI.deserialize("<gray> - <white><id></white>: <state>", Placeholder.unparsed("id", draft.id()), Placeholder.unparsed("state", state)));
            }
        }
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    public static Component nothingToShow() {
        return MINI.deserialize("<prefix> <yellow>There are no saved portals to show.");
    }

    public static Component showing(int count) {
        return MINI.deserialize("<prefix> <green>Showing <count> portal(s) for 8 seconds.", Placeholder.unparsed("count", String.valueOf(count)));
    }

    public static Component defaultRadiusHint() {
        return MINI.deserialize("<prefix> <yellow>No radius yet: the preview ring uses radius 3 around your eyes. Set one with <command>.", Placeholder.unparsed("command", "radius <r>"));
    }

    public static Component usage() {
        return MINI.deserialize("<prefix> <red>Usage: <usage>", Placeholder.unparsed("usage", "/setup portal list | show | create <id> | <id> pos1 | pos2 | shape box|ring | centre | radius <r> | disc <r> | task <task> | permission <perm|none> | save | cancel | remove | label here|text <mm>|offline <mm>|source <type> [name]|remove"));
    }

    /** {@code portal 'id': reason}, the validator's wording for one problem. */
    public static Component problem(PortalProblem problem) {
        return MINI.deserialize("<red> - <label>: <reason>", Placeholder.unparsed("label", problem.portalLabel()), Placeholder.unparsed("reason", problem.reason()));
    }

    private static Component saved(String verb, Portal portal) {
        return MINI.deserialize("<prefix> <green><verb> portal <id>: <shape>, task <task>, permission <permission><label>.", TagResolver.resolver(Placeholder.unparsed("verb", verb), portalTags(portal)));
    }

    private static TagResolver portalTags(Portal portal) {
        return TagResolver.resolver(Placeholder.unparsed("id", portal.id()), Placeholder.unparsed("shape", describe(portal.shape())), Placeholder.unparsed("task", portal.task()), Placeholder.unparsed("permission", portal.permission() == null ? "none" : portal.permission()), Placeholder.unparsed("label", portal.label() == null ? "" : ", label " + describe(portal.label())));
    }

    /** The text preview kept its last valid text because the shown text is unusable. */
    public static Component previewProblem(String reason) {
        return MINI.deserialize("<prefix> <red>The label preview keeps its last valid text: <reason>", Placeholder.unparsed("reason", reason));
    }

    private static Component labelUpdated(PortalEditResult.LabelUpdated updated) {
        if (!updated.label().isSet()) {
            return MINI.deserialize("<prefix> <green>Portal <id> has no label in its draft.", Placeholder.unparsed("id", updated.id()));
        }
        Component state = MINI.deserialize("<prefix> <green>Label of portal <id>: position <position>, text <text>, offline text <offline>, source <source>.", TagResolver.resolver(Placeholder.unparsed("id", updated.id()), Placeholder.unparsed("position", updated.label().position() == null ? "unset" : point(updated.label().position())), Placeholder.unparsed("text", orUnset(updated.label().text())), Placeholder.unparsed("offline", orUnset(updated.label().offlineText())), Placeholder.unparsed("source", updated.label().source() == null ? "the portal's task" : describe(updated.label().source()))));
        if (updated.missing().isEmpty()) {
            return state;
        }
        return state.appendNewline().append(MINI.deserialize("<yellow>Portal <id> is not complete yet, missing: <missing>.", Placeholder.unparsed("id", updated.id()), Placeholder.unparsed("missing", describeMissing(updated.missing()))));
    }

    private static String describe(PortalLabel label) {
        return (label.position() == null ? "without position" : "at " + point(label.position())) + " '" + label.text() + "'";
    }

    private static String describe(LabelSource source) {
        return switch (source) {
            case LabelSource.Task task -> "task " + orUnset(task.name());
            case LabelSource.Group group -> "group " + orUnset(group.name());
            case LabelSource.Service service -> "service " + orUnset(service.name());
            case LabelSource.Local ignored -> "local";
            case LabelSource.Unknown unknown -> "unknown " + orUnset(unknown.type());
        };
    }

    private static String orUnset(@Nullable String value) {
        return value == null ? "unset" : value;
    }

    private static Component complete(String id) {
        // Built with the API rather than a <click> tag so that nothing typed can end up in the command.
        Component button = Component.text("[save]", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/setup portal " + id + " save"));
        return MINI.deserialize("<prefix> <green>Portal <id> is complete, not saved yet.", Placeholder.unparsed("id", id)).appendSpace().append(button);
    }

    private static Component rejected(PortalEditResult.Rejected rejected) {
        List<Component> lines = new ArrayList<>();
        lines.add(MINI.deserialize("<prefix> <red>Portal <id> was not saved:", Placeholder.unparsed("id", rejected.id())));
        rejected.problems().forEach(problem -> lines.add(problem(problem)));
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    private static String describe(PortalShape shape) {
        return switch (shape) {
            case Box box -> "box " + point(box.min()) + " to " + point(box.max());
            case Disc disc ->
                "ring centre " + point(disc.center()) + ", radius " + number(disc.radius()) + ", normal " + point(disc.normal());
        };
    }

    private static String describeMissing(List<Missing> missing) {
        return missing.stream().map(PortalMessages::describe).collect(Collectors.joining(", "));
    }

    private static String describe(Missing missing) {
        return switch (missing) {
            case FORM -> "shape (box or ring)";
            case CORNER_1 -> "first corner";
            case CORNER_2 -> "second corner";
            case CENTRE -> "centre";
            case RADIUS -> "radius";
            case TASK -> "task";
            case LABEL_POSITION -> "label position (use 'label here')";
            case LABEL_TEXT -> "label text (use 'label text <minimessage>')";
        };
    }

    private static String point(Vec point) {
        return "(" + number(point.x()) + ", " + number(point.y()) + ", " + number(point.z()) + ")";
    }

    /** Whole numbers without a decimal point, others to at most three places. */
    private static String number(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String capitalised(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
