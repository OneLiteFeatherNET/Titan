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

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta.BillboardConstraints;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class LabelPreviewTest {

    private static final Vec ANCHOR = new Vec(12.5, 66, -3.5);

    private PortalEditor editor;
    private LabelPreview preview;
    private Instance instance;
    private Player player;
    private Player bystander;

    @BeforeEach
    void setUp(Env env) {
        editor = new PortalEditor(new InMemoryPortalStore());
        preview = new LabelPreview();
        instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        player = connection.connect(instance, new Pos(0.5, 40, 0.5));
        bystander = env.createConnection().connect(instance, new Pos(0.5, 40, 0.5));
    }

    private Optional<String> follow(String id) {
        return preview.follow(player, editor.draft(player.getUuid(), id).orElseThrow());
    }

    private void label(String id, String text) {
        editor.labelHere(player.getUuid(), id, new Pos(ANCHOR.x(), ANCHOR.y(), ANCHOR.z()));
        editor.labelText(player.getUuid(), id, text);
    }

    private List<Entity> displays() {
        return instance.getEntities().stream().filter(entity -> entity.getEntityType() == EntityType.TEXT_DISPLAY).toList();
    }

    private static String shown(Entity entity) {
        return PlainTextComponentSerializer.plainText().serialize(((TextDisplayMeta) entity.getEntityMeta()).getText());
    }

    @DisplayName("A draft label with position and text shows a display with the sample values")
    @Test
    void showsSampleValues() {
        editor.task(player.getUuid(), "p", "Survival");
        label("p", "<gold><task> <gray><online>/<max>");

        assertTrue(follow("p").isEmpty(), "valid text has no problem");

        Entity entity = preview.entity(player.getUuid(), "p").orElseThrow();
        assertEquals("Survival 12/50", shown(entity), "text with sample values");
        assertEquals(new Pos(ANCHOR.x(), ANCHOR.y(), ANCHOR.z()), entity.getPosition(), "at the anchor");
        assertEquals(1, displays().size(), "one display in the world");
    }

    @DisplayName("Without a task the portal id replaces <task>")
    @Test
    void portalIdReplacesMissingTask() {
        label("lobby-gate", "<task>");

        follow("lobby-gate");

        assertEquals("lobby-gate", shown(preview.entity(player.getUuid(), "lobby-gate").orElseThrow()), "id as task");
    }

    @DisplayName("A second follow updates the same entity, never adds one")
    @Test
    void followUpdatesTheSameEntity() {
        label("p", "<online>");
        follow("p");
        Entity first = preview.entity(player.getUuid(), "p").orElseThrow();

        editor.labelText(player.getUuid(), "p", "<max>");
        editor.labelHere(player.getUuid(), "p", new Pos(1, 70, 2));
        follow("p");

        assertSame(first, preview.entity(player.getUuid(), "p").orElseThrow(), "same entity");
        assertEquals("50", shown(first), "new text");
        assertEquals(new Pos(1, 70, 2), first.getPosition(), "new position");
        assertEquals(1, displays().size(), "still one display");
    }

    @DisplayName("The billboard of a saved label follows the draft")
    @Test
    void billboardOfSavedLabel() {
        PortalLabel saved = new PortalLabel(ANCHOR, "x", null, null, Billboard.FIXED, 90f);
        editor = new PortalEditor(new InMemoryPortalStore(new Portal("p", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "t", null, saved)));
        editor.labelText(player.getUuid(), "p", "y");

        follow("p");

        Entity entity = preview.entity(player.getUuid(), "p").orElseThrow();
        assertEquals(BillboardConstraints.FIXED, ((TextDisplayMeta) entity.getEntityMeta()).getBillboardRenderConstraints(), "fixed billboard");
        assertEquals(90f, entity.getPosition().yaw(), "yaw of the saved label");
    }

    @DisplayName("Offline shows the offline text, or the text with 0/0; online switches back")
    @Test
    void offlineAndBack() {
        label("p", "<online>/<max>");
        preview.offline(player.getUuid(), "p", true);
        follow("p");
        Entity entity = preview.entity(player.getUuid(), "p").orElseThrow();
        assertEquals("0/0", shown(entity), "text with zeros without offline text");

        editor.labelOffline(player.getUuid(), "p", "<red>closed");
        follow("p");
        assertEquals("closed", shown(entity), "offline text");

        preview.offline(player.getUuid(), "p", false);
        follow("p");
        assertEquals("12/50", shown(entity), "online again");
    }

    @DisplayName("Only the editing player is a viewer")
    @Test
    void onlyTheEditorSeesIt() {
        label("p", "x");
        follow("p");

        Entity entity = preview.entity(player.getUuid(), "p").orElseThrow();
        assertTrue(entity.getViewers().contains(player), "the editor sees it");
        assertFalse(entity.getViewers().contains(bystander), "the other player does not");
    }

    @DisplayName("clear removes the entity")
    @Test
    void clearRemovesTheEntity() {
        label("p", "x");
        follow("p");
        Entity entity = preview.entity(player.getUuid(), "p").orElseThrow();

        preview.clear(player.getUuid(), "p");

        assertTrue(entity.isRemoved(), "entity removed");
        assertTrue(preview.entity(player.getUuid(), "p").isEmpty(), "no longer tracked");
        assertEquals(0, displays().size(), "no display left");
    }

    @DisplayName("Removing the label removes the entity")
    @Test
    void removedLabelRemovesTheEntity() {
        label("p", "x");
        follow("p");

        editor.labelRemove(player.getUuid(), "p");
        follow("p");

        assertEquals(0, displays().size(), "no display left");
        assertEquals(0, preview.shown(), "nothing tracked");
    }

    @DisplayName("A label without position shows nothing")
    @Test
    void labelWithoutPositionShowsNothing() {
        editor.labelText(player.getUuid(), "p", "x");

        follow("p");

        assertEquals(0, displays().size(), "no display");
    }

    @DisplayName("clear of a player removes all their previews")
    @Test
    void clearOfPlayerRemovesAll() {
        label("a", "x");
        label("b", "y");
        follow("a");
        follow("b");

        preview.clear(player.getUuid());

        assertEquals(0, displays().size(), "no display left");
        assertEquals(0, preview.shown(), "nothing tracked");
    }

    @DisplayName("Invalid text keeps the last valid text and names the problem")
    @Test
    void invalidTextKeepsTheLastValidOne() {
        label("p", "<gold>Survival <online>/<max>");
        follow("p");
        Entity entity = preview.entity(player.getUuid(), "p").orElseThrow();

        editor.labelText(player.getUuid(), "p", "<gold>Survival</red>");
        Optional<String> problem = follow("p");

        assertTrue(problem.isPresent(), "the problem is reported");
        assertTrue(problem.get().contains("label.text"), "names the field: " + problem.get());
        assertEquals("Survival 12/50", shown(entity), "last valid text stays");
        assertEquals(1, displays().size(), "no second entity");
    }

    @DisplayName("Invalid text without an earlier valid one shows nothing")
    @Test
    void invalidFirstTextShowsNothing() {
        label("p", "<gold>Survival</red>");

        Optional<String> problem = follow("p");

        assertNotNull(problem.orElse(null), "the problem is reported");
        assertEquals(0, displays().size(), "nothing shown");
    }
}
