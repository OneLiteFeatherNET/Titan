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
package net.onelitefeather.titan.setup.portal.editor;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.ArrayList;
import java.util.List;

/**
 * The guided flow as a pure function: the next step follows from what the draft still lacks, so
 * there is no state machine of its own. Buttons only carry ordinary {@code /setup portal ...}
 * commands, and they are built with the component API, never with {@code <click>} tags, so text a
 * player typed cannot end up inside a command.
 */
public final class PortalFlow {

    /** At most this many known tasks become buttons. */
    static final int MAX_TASKS = 8;

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** {@code RUN} sends the command at once, {@code SUGGEST} only fills the chat input. */
    public enum Kind {
        RUN, SUGGEST
    }

    public record FlowButton(String label, String command, Kind kind) {
        Component component() {
            ClickEvent click = kind == Kind.RUN ? ClickEvent.runCommand(command) : ClickEvent.suggestCommand(command);
            return Component.text("[" + label + "]", NamedTextColor.AQUA).clickEvent(click);
        }
    }

    public record FlowStep(Component message, List<FlowButton> buttons) {
        public FlowStep {
            buttons = List.copyOf(buttons);
        }
    }

    private PortalFlow() {
    }

    /**
     * @param knownTasks tasks of the portals in the world; duplicates and unusable entries are
     *                   dropped
     */
    public static FlowStep render(PortalDraft draft, List<String> knownTasks) {
        String base = "/setup portal " + draft.id() + " ";
        List<Missing> missing = draft.missing();
        if (missing.isEmpty()) {
            return draft.permissionChosen() ? summary(draft, base) : permission(draft, base);
        }
        return switch (missing.getFirst()) {
            case FORM ->
                step(draft, "choose a shape.", List.of(run("Box", base + "shape box"), run("Ring", base + "shape ring")));
            case CORNER_1 ->
                step(draft, "stand in the first corner block.", List.of(run("set corner 1", base + "pos1")));
            case CORNER_2 ->
                step(draft, "stand in the opposite corner block.", List.of(run("set corner 2", base + "pos2")));
            case CENTRE ->
                step(draft, "stand in the centre of the ring and look through it.", List.of(run("set centre", base + "centre")));
            case RADIUS -> step(draft, "choose the radius.", radii(base));
            case TASK -> step(draft, "choose what the portal does.", tasks(base, knownTasks));
            case LABEL_POSITION ->
                step(draft, "stand where the label floats.", List.of(run("set label position", base + "label here"), run("remove label", base + "label remove")));
            case LABEL_TEXT ->
                step(draft, "enter the label text.", List.of(new FlowButton("enter…", base + "label text ", Kind.SUGGEST), run("remove label", base + "label remove")));
        };
    }

    private static FlowStep permission(PortalDraft draft, String base) {
        return step(draft, "who may use it?", List.of(run("none", base + "permission none"), new FlowButton("enter…", base + "permission ", Kind.SUGGEST)));
    }

    private static FlowStep summary(PortalDraft draft, String base) {
        String permission = draft.permission() == null ? "none" : draft.permission();
        String text = "ready to save, task " + draft.task() + ", permission " + permission + ".";
        return step(draft, text, List.of(run("save", base + "save"), run("cancel", base + "cancel")));
    }

    private static List<FlowButton> radii(String base) {
        List<FlowButton> buttons = new ArrayList<>();
        for (double radius : DraftOutline.RADIUS_SUGGESTIONS) {
            String value = PortalCompletions.number(radius);
            buttons.add(run(value, base + "radius " + value));
        }
        buttons.add(new FlowButton("other…", base + "radius ", Kind.SUGGEST));
        return buttons;
    }

    private static List<FlowButton> tasks(String base, List<String> knownTasks) {
        List<FlowButton> buttons = new ArrayList<>();
        knownTasks.stream()
                // A control character in a run_command makes the client refuse the whole click.
                .filter(task -> !task.isBlank() && task.chars().noneMatch(Character::isISOControl)).distinct().limit(MAX_TASKS).forEach(task -> buttons.add(run(task, base + "task " + task)));
        buttons.add(new FlowButton("other…", base + "task ", Kind.SUGGEST));
        return buttons;
    }

    private static FlowButton run(String label, String command) {
        return new FlowButton(label, command, Kind.RUN);
    }

    private static FlowStep step(PortalDraft draft, String text, List<FlowButton> buttons) {
        Component message = MINI.deserialize("<prefix> <yellow>Portal <id>: <text>", Placeholder.unparsed("id", draft.id()), Placeholder.unparsed("text", text));
        for (FlowButton button : buttons) {
            message = message.appendSpace().append(button.component());
        }
        return new FlowStep(message, buttons);
    }
}
