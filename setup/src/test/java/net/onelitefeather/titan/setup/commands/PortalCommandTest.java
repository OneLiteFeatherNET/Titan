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
package net.onelitefeather.titan.setup.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandResult;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.common.map.MapEntry;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.LabelSource;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import net.onelitefeather.titan.setup.listener.PortalDisconnectListener;
import net.onelitefeather.titan.setup.portal.DiscPlacement;
import net.onelitefeather.titan.setup.portal.DraftPreview;
import net.onelitefeather.titan.setup.portal.MapProviderPortalStore;
import net.onelitefeather.titan.setup.portal.PortalDraft;
import net.onelitefeather.titan.setup.portal.PortalEditor;
import net.onelitefeather.titan.setup.portal.PortalShow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class PortalCommandTest {

    private static final Portal OLD = new Portal("old", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), "Old task", null);
    private static final Pos STANDING = new Pos(10.7, 64.2, -3.5, 90, 20);

    @TempDir
    Path base;

    private MapProvider provider;
    private MapProviderPortalStore store;
    private PortalEditor editor;
    private DraftPreview preview;
    private PortalShow show;
    private CommandManager commands;
    private TestConnection connection;
    private Player player;

    @BeforeEach
    void setUp(Env env) throws IOException {
        Path directory = Files.createDirectories(base.resolve("worlds").resolve("world"));
        Files.writeString(directory.resolve(MapEntry.MAP_FILE_NAME), """
                {"name":"world","spawn":{"x":1.5,"y":65,"z":2.5,"yaw":90,"pitch":0},"builders":["alice"],
                 "portals":[{"id":"old","task":"Old task","shape":{"type":"box","min":{"x":0,"y":0,"z":0},"max":{"x":1,"y":1,"z":1}}}]}""");
        provider = MapProvider.create(base, env.process().instance().createInstanceContainer(), Optional.of("world"));
        store = new MapProviderPortalStore(provider);
        editor = new PortalEditor(store);
        preview = new DraftPreview(editor);
        show = new PortalShow();
        commands = env.process().command();
        commands.register(new SetupCommand(provider, new PortalCommand(editor, store, preview, show)));
        env.process().eventHandler().addListener(PlayerDisconnectEvent.class, new PortalDisconnectListener(editor, preview, show));
        connection = env.createConnection();
        player = connection.connect(env.createFlatInstance(), STANDING);
    }

    @DisplayName("pos1 and pos2 take the block under the player, rounded down")
    @Test
    void cornersTakeTheBlockUnderThePlayer() {
        run("setup portal p pos1");
        run("setup portal p pos2");

        PortalDraft draft = draft("p");
        assertEquals(new Vec(10, 64, -4), draft.corner1(), "corner 1 is the floored block");
        assertEquals(new Vec(10, 64, -4), draft.corner2(), "corner 2 is the floored block");
    }

    @DisplayName("shape picks the form")
    @Test
    void shapePicksTheForm() {
        run("setup portal p shape ring");

        assertEquals(PortalDraft.Form.RING, draft("p").form(), "ring chosen");
    }

    @DisplayName("centre takes eye position and look direction, without a radius")
    @Test
    void centreTakesEyeAndLook() {
        run("setup portal p centre");

        PortalDraft draft = draft("p");
        assertEquals(DiscPlacement.centre(STANDING.add(0, player.getEyeHeight(), 0)), draft.centre(), "centre from the eye");
        assertEquals(DiscPlacement.normal(STANDING.direction()), draft.normal(), "normal from the look direction");
        assertNull(draft.radius(), "centre does not set a radius");
    }

    @DisplayName("radius changes only the radius")
    @Test
    void radiusKeepsTheCentre() {
        run("setup portal p centre");
        Vec centre = draft("p").centre();
        player.teleport(new Pos(50, 70, 50));

        run("setup portal p radius 4");

        assertEquals(4.0, draft("p").radius(), "radius set");
        assertEquals(centre, draft("p").centre(), "the centre stays where it was set");
    }

    @DisplayName("disc sets centre, normal and radius in one step")
    @Test
    void discSetsEverything() {
        run("setup portal p disc 3");

        PortalDraft draft = draft("p");
        assertEquals(DiscPlacement.centre(STANDING.add(0, player.getEyeHeight(), 0)), draft.centre(), "centre from the eye");
        assertEquals(DiscPlacement.normal(STANDING.direction()), draft.normal(), "normal from the look direction");
        assertEquals(3.0, draft.radius(), "radius from the argument");
    }

    @DisplayName("task joins all words")
    @Test
    void taskJoinsTheWords() {
        run("setup portal p task Go to the <red>lobby");

        assertEquals("Go to the <red>lobby", draft("p").task(), "words joined, tags stay literal text");
    }

    @DisplayName("permission sets and none removes it")
    @Test
    void permissionSetsAndRemoves() {
        run("setup portal p permission titan.portal.p");
        assertEquals("titan.portal.p", draft("p").permission(), "permission set");

        run("setup portal p permission none");

        assertNull(draft("p").permission(), "none removes the permission");
    }

    @DisplayName("save writes a complete draft to the map and cancel and remove work")
    @Test
    void saveCancelAndRemove() {
        run("setup portal p pos1");
        run("setup portal p pos2");
        run("setup portal p task Somewhere");
        assertEquals(List.of(OLD), store.portals(), "nothing is saved before save");

        run("setup portal p save");
        assertEquals(List.of("old", "p"), store.portals().stream().map(Portal::id).toList(), "save appends the portal");

        run("setup portal p remove");
        assertEquals(List.of(OLD), store.portals(), "remove deletes the saved portal");

        run("setup portal q pos1");
        run("setup portal q cancel");
        assertTrue(editor.drafts(player.getUuid()).isEmpty(), "cancel discards the draft");
    }

    @DisplayName("list shows saved portals and, separately, the player's drafts")
    @Test
    void listShowsPortalsAndDrafts() {
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);
        run("setup portal draftone pos1");
        run("setup portal list");

        List<String> lines = plain(chat);
        String list = lines.getLast();
        assertTrue(list.contains("old"), "the saved portal is listed: " + list);
        assertTrue(list.contains("Drafts"), "drafts have their own section: " + list);
        assertTrue(list.contains("draftone"), "the draft is listed: " + list);
    }

    @DisplayName("show outlines the saved portals")
    @Test
    void showStartsTheOutline(Env env) {
        run("setup portal show");

        assertEquals(1, show.running(), "the show runs for the player");
    }

    @DisplayName("Wrong arguments are refused and leave the draft alone")
    @Test
    void wrongArgumentsAreRefused() {
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);

        run("setup portal p disc abc");
        run("setup portal p disc");
        run("setup portal p permission");
        run("setup portal p task");
        run("setup portal p shape sphere");

        assertTrue(editor.drafts(player.getUuid()).isEmpty(), "no bad command created a draft");
        List<String> replies = plain(chat);
        assertEquals(5, replies.size(), "every bad command is answered");
        assertTrue(replies.stream().allMatch(reply -> reply.contains("Usage")), "with the usage: " + replies);
    }

    @DisplayName("The console cannot use the command")
    @Test
    void consoleIsRefused() {
        CommandSender console = commands.getConsoleSender();

        CommandResult result = commands.execute(console, "setup portal p pos1");

        assertNotEquals(CommandResult.Type.SUCCESS, result.getType(), "playerOnly stops the console");
        assertTrue(editor.drafts(player.getUuid()).isEmpty(), "no draft appeared");
    }

    @DisplayName("Leaving discards the drafts and stops the preview and show")
    @Test
    void leavingCleansUp(Env env) {
        run("setup portal p pos1");
        run("setup portal show");
        assertEquals(1, preview.running(), "the preview runs while the draft is open");

        player.remove();

        assertTrue(editor.drafts(player.getUuid()).isEmpty(), "drafts are gone");
        assertEquals(0, preview.running(), "preview stopped");
        assertEquals(0, show.running(), "show stopped");
    }

    @DisplayName("Saving stops the preview")
    @Test
    void savingStopsThePreview() {
        run("setup portal p pos1");
        run("setup portal p pos2");
        run("setup portal p task T");
        assertEquals(1, preview.running(), "preview runs for the open draft");

        run("setup portal p save");

        assertEquals(0, preview.running(), "preview ends with the draft");
    }

    @DisplayName("create starts a guided draft that shows the shape step")
    @Test
    void createShowsTheShapeStep() {
        Component step = send("setup portal create gate");

        assertTrue(draft("gate").guided(), "the draft is guided");
        assertEquals(List.of("/setup portal gate shape box", "/setup portal gate shape ring"), clickCommands(step), "box and ring buttons");
    }

    @DisplayName("Clicking the buttons walks a box through the flow and save persists it")
    @Test
    void buttonsWalkABoxToSave() {
        send("setup portal create gate");

        Component corner1 = send("setup portal gate shape box");
        assertEquals(List.of("/setup portal gate pos1"), clickCommands(corner1), "corner 1 next");
        Component corner2 = send("setup portal gate pos1");
        assertEquals(List.of("/setup portal gate pos2"), clickCommands(corner2), "corner 2 next");
        Component tasks = send("setup portal gate pos2");
        assertTrue(clickCommands(tasks).contains("/setup portal gate task Old task"), "the task of the saved portal is offered: " + clickCommands(tasks));
        Component permission = send("setup portal gate task Old task");
        assertEquals(List.of("/setup portal gate permission none", "/setup portal gate permission "), clickCommands(permission), "permission next");
        Component summary = send("setup portal gate permission none");
        assertEquals(List.of("/setup portal gate save", "/setup portal gate cancel"), clickCommands(summary), "summary");
        run("setup portal gate save");

        assertEquals(List.of("old", "gate"), store.portals().stream().map(Portal::id).toList(), "save persisted the portal");
        assertEquals(new Box(new Vec(10, 64, -4), new Vec(10, 64, -4)), store.portals().getLast().shape(), "corners from the clicks");
        assertTrue(editor.drafts(player.getUuid()).isEmpty(), "the draft is gone after save");
    }

    @DisplayName("Clicking the buttons walks a ring through centre, radius and task")
    @Test
    void buttonsWalkARing() {
        send("setup portal create ringy");

        Component centre = send("setup portal ringy shape ring");
        assertEquals(List.of("/setup portal ringy centre"), clickCommands(centre), "centre next");
        Component radius = send("setup portal ringy centre");
        assertTrue(clickCommands(radius).contains("/setup portal ringy radius 5"), "radius buttons: " + clickCommands(radius));
        Component task = send("setup portal ringy radius 5");

        assertEquals(5.0, draft("ringy").radius(), "the radius button set the radius");
        assertTrue(clickCommands(task).contains("/setup portal ringy task Old task"), "task step follows");
    }

    @DisplayName("cancel from the flow discards the draft and saves nothing")
    @Test
    void cancelDiscards() {
        send("setup portal create gate");
        send("setup portal gate shape box");
        assertTrue(draft("gate").guided(), "the draft is guided");

        run("setup portal gate cancel");

        assertTrue(editor.drafts(player.getUuid()).isEmpty(), "cancel discards the draft");
        assertEquals(List.of(OLD), store.portals(), "nothing saved");
    }

    @DisplayName("A hand-typed edit of a plain draft does not send a flow step")
    @Test
    void plainDraftsGetNoSteps() {
        Component reply = send("setup portal p pos1");

        assertEquals(0, clickCommands(reply).size(), "only the not-complete text, no buttons");
    }

    @DisplayName("Tab completion offers ids of saved portals and own drafts after the command")
    @Test
    void completesIds() {
        run("setup portal create gate");

        assertTrue(suggestions("setup portal ").containsAll(List.of("old", "gate")), "ids offered: " + suggestions("setup portal "));
    }

    @DisplayName("Tab completion offers radii, tasks and none at their arguments")
    @Test
    void completesArguments() {
        assertEquals(List.of("1", "2", "3", "5", "8"), suggestions("setup portal p radius "), "radii");
        assertEquals(List.of("Old task"), suggestions("setup portal p task "), "tasks of the saved portals");
        assertEquals(List.of("none"), suggestions("setup portal p permission "), "none");
    }

    @DisplayName("create is reserved and cannot be used as an id")
    @Test
    void createIsReserved() {
        run("setup portal create create");

        assertTrue(editor.drafts(player.getUuid()).isEmpty(), "reserved id refused");
    }

    @DisplayName("/setup map setspawn still works and keeps the portals")
    @Test
    void mapCommandIsUnchanged() {
        run("setup map setspawn");

        assertEquals(STANDING, provider.getActiveLobby().spawn(), "spawn saved as before");
        assertEquals(List.of(OLD), provider.getActiveLobby().portals(), "portals kept");
    }

    @DisplayName("label here anchors the draft at the player's exact position")
    @Test
    void labelHereTakesThePlayersPosition(Env env) {
        run("setup portal p label here");
        env.tick();

        assertEquals(new Vec(10.7, 64.2, -3.5), draft("p").labelPosition(), "exact position, not the block");
        assertEquals(List.of(OLD), store.portals(), "nothing saved");
        assertEquals(1, preview.running(), "the preview follows the draft");
    }

    @DisplayName("label text and offline take every word")
    @Test
    void labelTextAndOfflineTakeAllWords() {
        run("setup portal p label text <gold>Survival <gray><online>/<max>");
        run("setup portal p label offline <red>Survival startet gleich");

        assertEquals("<gold>Survival <gray><online>/<max>", draft("p").labelText(), "text with spaces");
        assertEquals("<red>Survival startet gleich", draft("p").labelOffline(), "offline text with spaces");
    }

    @DisplayName("label source takes a type and, where needed, a name")
    @Test
    void labelSourceTakesTypeAndName() {
        run("setup portal p label source group Games");
        assertEquals(new LabelSource.Group("Games"), draft("p").labelSource(), "group Games");

        run("setup portal p label source local");

        assertEquals(new LabelSource.Local(), draft("p").labelSource(), "local needs no name");
    }

    @DisplayName("label source without a name or with an unknown type is refused in chat")
    @Test
    void labelSourceMistakesAreAnswered() {
        run("setup portal p label source group Games");

        Component noName = send("setup portal p label source task");
        Component unknownType = send("setup portal p label source proxy x");

        assertTrue(PlainTextComponentSerializer.plainText().serialize(noName).contains("needs a name"), "name demanded");
        assertTrue(PlainTextComponentSerializer.plainText().serialize(unknownType).contains("task, group, service, local"), "types named");
        assertEquals(new LabelSource.Group("Games"), draft("p").labelSource(), "draft unchanged");
    }

    @DisplayName("label with missing arguments is refused with the usage")
    @Test
    void labelWithMissingArgumentsIsRefused() {
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);

        run("setup portal p label");
        run("setup portal p label text");
        run("setup portal p label offline");
        run("setup portal p label source");
        run("setup portal p label bogus");

        assertTrue(editor.drafts(player.getUuid()).isEmpty(), "no bad command created a draft");
        List<String> replies = plain(chat);
        assertEquals(5, replies.size(), "every bad command is answered");
        assertTrue(replies.stream().allMatch(reply -> reply.contains("Usage")), "with the usage: " + replies);
    }

    @DisplayName("The console cannot use the label syntaxes")
    @Test
    void consoleCannotEditLabels() {
        CommandSender console = commands.getConsoleSender();

        for (String command : List.of("setup portal p label here", "setup portal p label text Hi", "setup portal p label source local", "setup portal p label remove")) {
            assertNotEquals(CommandResult.Type.SUCCESS, commands.execute(console, command).getType(), command + " is refused");
        }
        assertTrue(editor.drafts(player.getUuid()).isEmpty(), "no draft appeared");
    }

    @DisplayName("A label survives save and a later setspawn, in the map file")
    @Test
    void labelSurvivesSaveAndSetspawn() throws IOException {
        run("setup portal p pos1");
        run("setup portal p pos2");
        run("setup portal p task Somewhere");
        run("setup portal p label here");
        run("setup portal p label text <gold>Hi <online>");
        run("setup portal p label source group Games");
        assertFalse(mapFile().contains("\"label\""), "nothing in the file before save");

        run("setup portal p save");
        run("setup map setspawn");

        PortalLabel label = provider.getActiveLobby().portals().getLast().label();
        assertNotNull(label, "the label is in the active map");
        assertEquals(new Vec(10.7, 64.2, -3.5), label.position(), "anchor kept");
        assertEquals("<gold>Hi <online>", label.text(), "text kept");
        assertEquals(new LabelSource.Group("Games"), label.source(), "source kept");
        String file = mapFile();
        assertTrue(file.contains("\"label\"") && file.contains("Hi"), "the label is written: " + file);
        assertNull(provider.getActiveLobby().portals().getFirst().label(), "the other portal has no label");
    }

    @DisplayName("label remove followed by save drops the saved label")
    @Test
    void labelRemoveThenSaveDropsIt() throws IOException {
        run("setup portal old label here");
        run("setup portal old label text Hi");
        run("setup portal old save");
        assertNotNull(store.portals().getFirst().label(), "label saved");

        run("setup portal old label remove");
        assertNotNull(store.portals().getFirst().label(), "still saved until save");
        run("setup portal old save");

        assertNull(store.portals().getFirst().label(), "label gone");
        assertFalse(mapFile().contains("\"label\""), "no label block in the file");
    }

    @DisplayName("A label without text is not saved and names the missing text")
    @Test
    void incompleteLabelIsNotSaved() {
        run("setup portal old label here");

        Component reply = send("setup portal old save");

        assertTrue(PlainTextComponentSerializer.plainText().serialize(reply).contains("label text"), "text named as missing");
        assertEquals(List.of(OLD), store.portals(), "store unchanged");
    }

    @DisplayName("Tab completion offers the source types")
    @Test
    void completesSourceTypes() {
        assertEquals(List.of("task", "group", "service", "local"), suggestions("setup portal p label source "), "source types");
    }

    /** Runs the command as the player and returns the one chat message it answered with. */
    private Component send(String command) {
        Collector<SystemChatPacket> chat = connection.trackIncoming(SystemChatPacket.class);
        run(command);
        List<SystemChatPacket> replies = chat.collect();
        assertEquals(1, replies.size(), "one reply to '" + command + "'");
        return replies.getFirst().message();
    }

    private static List<String> clickCommands(Component component) {
        List<String> commands = new ArrayList<>();
        if (component.clickEvent() != null && component.clickEvent().payload() instanceof ClickEvent.Payload.Text text) {
            commands.add(text.value());
        }
        component.children().forEach(child -> commands.addAll(clickCommands(child)));
        return commands;
    }

    private List<String> suggestions(String input) {
        Suggestion suggestion = commands.parseCommand(player, input + '\0').suggestion(player);
        assertNotNull(suggestion, "a suggestion for '" + input + "'");
        return suggestion.getEntries().stream().map(SuggestionEntry::getEntry).toList();
    }

    private void run(String command) {
        commands.execute(player, command);
    }

    private String mapFile() throws IOException {
        return Files.readString(base.resolve("worlds").resolve("world").resolve(MapEntry.MAP_FILE_NAME));
    }

    private PortalDraft draft(String id) {
        PortalDraft found = editor.drafts(player.getUuid()).stream().filter(draft -> draft.id().equals(id)).findFirst().orElse(null);
        assertNotNull(found, "draft " + id + " exists");
        return found;
    }

    private static List<String> plain(Collector<SystemChatPacket> chat) {
        return chat.collect().stream().map(packet -> PlainTextComponentSerializer.plainText().serialize(packet.message())).collect(Collectors.toList());
    }
}
