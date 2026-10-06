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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.setup.portal.editor.PortalFlow.FlowButton;
import net.onelitefeather.titan.setup.portal.editor.PortalFlow.FlowStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalFlowTest {

    private static final String BASE = "/setup portal gate ";

    private static PortalDraft draft() {
        return new PortalDraft("gate");
    }

    private static PortalDraft boxWithCorners() {
        PortalDraft draft = draft();
        draft.corner1(new Vec(0, 0, 0));
        draft.corner2(new Vec(1, 1, 1));
        return draft;
    }

    private static PortalDraft ringWithRadius() {
        PortalDraft draft = draft();
        draft.centre(new Vec(0, 70, 0), new Vec(0, 0, 1));
        draft.radius(3);
        return draft;
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static List<String> commands(FlowStep step) {
        return step.buttons().stream().map(FlowButton::command).toList();
    }

    /** Every click event found in the rendered component, in order. */
    private static List<ClickEvent<?>> clicks(Component component) {
        List<ClickEvent<?>> found = new ArrayList<>();
        if (component.clickEvent() != null) {
            found.add(component.clickEvent());
        }
        component.children().forEach(child -> found.addAll(clicks(child)));
        return found;
    }

    private static List<ClickEvent<?>> expectedClicks(FlowStep step) {
        return step.buttons().stream().<ClickEvent<?>>map(button -> (button.kind() == PortalFlow.Kind.RUN ? ClickEvent.runCommand(button.command()) : ClickEvent.suggestCommand(button.command()))).toList();
    }

    @Test
    @DisplayName("Without a shape the flow offers box and ring")
    void chooseShape() {
        FlowStep step = PortalFlow.render(draft(), List.of());

        assertEquals(List.of(BASE + "shape box", BASE + "shape ring"), commands(step));
        assertTrue(step.buttons().stream().allMatch(button -> button.kind() == PortalFlow.Kind.RUN), "shape buttons run at once");
        assertTrue(plain(step.message()).contains("[Box]") && plain(step.message()).contains("[Ring]"), plain(step.message()));
    }

    @Test
    @DisplayName("A box asks for corner 1, then corner 2")
    void boxCorners() {
        PortalDraft draft = draft();
        draft.form(PortalDraft.Form.BOX);
        assertEquals(List.of(BASE + "pos1"), commands(PortalFlow.render(draft, List.of())));

        draft.corner1(new Vec(0, 0, 0));
        assertEquals(List.of(BASE + "pos2"), commands(PortalFlow.render(draft, List.of())));
    }

    @Test
    @DisplayName("A ring asks the player to stand in the centre and look through it")
    void ringCentre() {
        PortalDraft draft = draft();
        draft.form(PortalDraft.Form.RING);

        FlowStep step = PortalFlow.render(draft, List.of());

        assertEquals(List.of(BASE + "centre"), commands(step));
        assertTrue(plain(step.message()).contains("look through"), plain(step.message()));
    }

    @Test
    @DisplayName("After the centre the radius suggestions and a free input follow")
    void ringRadius() {
        PortalDraft draft = draft();
        draft.centre(new Vec(0, 70, 0), new Vec(0, 0, 1));

        FlowStep step = PortalFlow.render(draft, List.of());

        assertEquals(List.of(BASE + "radius 2", BASE + "radius 3", BASE + "radius 5", BASE + "radius 8", BASE + "radius "), commands(step));
        assertEquals(PortalFlow.Kind.SUGGEST, step.buttons().getLast().kind(), "other... fills the input");
        assertTrue(step.buttons().subList(0, 4).stream().allMatch(button -> button.kind() == PortalFlow.Kind.RUN));
    }

    @Test
    @DisplayName("The task step offers each known task once, at most eight, plus a free input")
    void taskButtons() {
        List<String> known = List.of("A", "B", "A", "C", "D", "E", "F", "G", "H", "I", "J");

        FlowStep step = PortalFlow.render(boxWithCorners(), known);

        assertEquals(List.of(BASE + "task A", BASE + "task B", BASE + "task C", BASE + "task D", BASE + "task E", BASE + "task F", BASE + "task G", BASE + "task H", BASE + "task "), commands(step));
        assertEquals(PortalFlow.Kind.SUGGEST, step.buttons().getLast().kind());
    }

    @Test
    @DisplayName("Without known tasks only the free input is offered")
    void taskWithoutKnown() {
        assertEquals(List.of(BASE + "task "), commands(PortalFlow.render(ringWithRadius(), List.of())));
    }

    @Test
    @DisplayName("A task with tags and quotes stays literal text in label and command")
    void taskStaysLiteral() {
        String task = "<click:run_command:'/op me'>Go</click> 'x'";

        FlowStep step = PortalFlow.render(boxWithCorners(), List.of(task));

        assertEquals(BASE + "task " + task, commands(step).getFirst(), "the command carries the text as is");
        assertTrue(plain(step.message()).contains(task), "the label shows the text as is: " + plain(step.message()));
        assertEquals(expectedClicks(step), clicks(step.message()), "no click event except the buttons' own");
    }

    @Test
    @DisplayName("Tasks with control characters are not offered as buttons")
    void controlCharactersAreNotOffered() {
        FlowStep step = PortalFlow.render(boxWithCorners(), List.of("bad\n/op me", "fine"));

        assertEquals(List.of(BASE + "task fine", BASE + "task "), commands(step));
    }

    @Test
    @DisplayName("Once the draft is complete the permission is asked: none or typed")
    void permissionStep() {
        PortalDraft draft = boxWithCorners();
        draft.task("Somewhere");

        FlowStep step = PortalFlow.render(draft, List.of());

        assertEquals(List.of(BASE + "permission none", BASE + "permission "), commands(step));
        assertEquals(PortalFlow.Kind.RUN, step.buttons().getFirst().kind());
        assertEquals(PortalFlow.Kind.SUGGEST, step.buttons().getLast().kind());
    }

    @Test
    @DisplayName("After the permission the summary offers save and cancel")
    void summary() {
        PortalDraft draft = boxWithCorners();
        draft.task("Somewhere");
        draft.permission(null);

        FlowStep step = PortalFlow.render(draft, List.of());

        assertEquals(List.of(BASE + "save", BASE + "cancel"), commands(step));
        assertTrue(plain(step.message()).contains("gate"), plain(step.message()));
    }

    @Test
    @DisplayName("Every step's clickable parts are exactly its buttons")
    void componentMatchesButtons() {
        PortalDraft summary = boxWithCorners();
        summary.task("T");
        summary.permission("titan.x");
        for (PortalDraft draft : List.of(draft(), boxWithCorners(), ringWithRadius(), summary)) {
            FlowStep step = PortalFlow.render(draft, List.of("Known"));
            assertEquals(expectedClicks(step), clicks(step.message()), "clicks match buttons for " + draft.missing());
            assertFalse(step.buttons().isEmpty());
        }
    }
}
