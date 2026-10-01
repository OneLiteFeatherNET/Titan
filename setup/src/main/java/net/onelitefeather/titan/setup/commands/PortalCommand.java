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
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.setup.portal.DraftPreview;
import net.onelitefeather.titan.setup.portal.PortalCompletions;
import net.onelitefeather.titan.setup.portal.PortalDraft;
import net.onelitefeather.titan.setup.portal.PortalEditResult;
import net.onelitefeather.titan.setup.portal.PortalEditor;
import net.onelitefeather.titan.setup.portal.PortalFlow;
import net.onelitefeather.titan.setup.portal.PortalMessages;
import net.onelitefeather.titan.setup.portal.PortalShow;
import net.onelitefeather.titan.setup.portal.PortalStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * {@code /setup portal ...}: parses arguments, hands them to the {@link PortalEditor} and prints
 * its answer. The rules live in the editor; this class holds none.
 */
public final class PortalCommand extends Command {

    private final Argument<String> id = ArgumentType.Word("id");
    // Same name as id, so context.get(id) reads it too; only 'create <id>' names a new portal, so
    // it must not suggest the existing ones.
    private final Argument<String> newId = ArgumentType.Word("id");
    private final Argument<Double> radius = ArgumentType.Double("value");
    private final Argument<String[]> task = ArgumentType.StringArray("text");
    private final Argument<String[]> labelText = ArgumentType.StringArray("minimessage");
    private final Argument<String> sourceType = ArgumentType.Word("type");
    private final Argument<String> sourceName = ArgumentType.Word("name");
    private final Argument<String> permission = ArgumentType.Word("node");
    private final Argument<String> form = ArgumentType.Word("kind").from("box", "ring");

    private final PortalEditor editor;
    private final PortalStore store;
    private final DraftPreview preview;
    private final PortalShow show;

    public PortalCommand(PortalEditor editor, PortalStore store, DraftPreview preview, PortalShow show) {
        super("portal");
        this.editor = editor;
        this.store = store;
        this.preview = preview;
        this.show = show;

        id.setSuggestionCallback((sender, context, suggestion) -> suggest(sender, suggestion, player -> PortalCompletions.ids(store.portals(), editor.drafts(player.getUuid()))));
        radius.setSuggestionCallback((sender, context, suggestion) -> suggest(sender, suggestion, player -> PortalCompletions.radii()));
        task.setSuggestionCallback((sender, context, suggestion) -> suggest(sender, suggestion, player -> PortalCompletions.tasks(store.portals())));
        // Free word, not 'from(...)': the editor answers an unknown type with the allowed ones.
        sourceType.setSuggestionCallback((sender, context, suggestion) -> suggest(sender, suggestion, player -> PortalCompletions.sourceTypes()));
        permission.setSuggestionCallback((sender, context, suggestion) -> suggest(sender, suggestion, player -> PortalCompletions.permissions()));

        setDefaultExecutor((sender, context) -> sender.sendMessage(PortalMessages.usage()));
        addSyntax(this::list, ArgumentType.Literal("list"));
        addSyntax(this::show, ArgumentType.Literal("show"));
        addSyntax(edit((player, context) -> editor.create(player.getUuid(), context.get(id))), ArgumentType.Literal("create"), newId);
        addSyntax(edit((player, context) -> editor.corner1(player.getUuid(), context.get(id), player.getPosition())), id, ArgumentType.Literal("pos1"));
        addSyntax(edit((player, context) -> editor.corner2(player.getUuid(), context.get(id), player.getPosition())), id, ArgumentType.Literal("pos2"));
        addSyntax(edit((player, context) -> editor.shape(player.getUuid(), context.get(id), context.get(form).equals("box") ? PortalDraft.Form.BOX : PortalDraft.Form.RING)), id, ArgumentType.Literal("shape"), form);
        addSyntax(edit((player, context) -> editor.centre(player.getUuid(), context.get(id), eye(player), player.getPosition().direction())), id, ArgumentType.Literal("centre"));
        addSyntax(edit((player, context) -> editor.radius(player.getUuid(), context.get(id), context.get(radius))), id, ArgumentType.Literal("radius"), radius);
        addSyntax(edit((player, context) -> editor.disc(player.getUuid(), context.get(id), eye(player), player.getPosition().direction(), context.get(radius))), id, ArgumentType.Literal("disc"), radius);
        addSyntax(edit((player, context) -> editor.task(player.getUuid(), context.get(id), String.join(" ", context.get(task)))), id, ArgumentType.Literal("task"), task);
        addSyntax(edit((player, context) -> editor.permission(player.getUuid(), context.get(id), context.get(permission))), id, ArgumentType.Literal("permission"), permission);
        Argument<String> label = ArgumentType.Literal("label");
        addSyntax(edit((player, context) -> editor.labelHere(player.getUuid(), context.get(id), player.getPosition())), id, label, ArgumentType.Literal("here"));
        addSyntax(edit((player, context) -> editor.labelText(player.getUuid(), context.get(id), String.join(" ", context.get(labelText)))), id, label, ArgumentType.Literal("text"), labelText);
        addSyntax(edit((player, context) -> editor.labelOffline(player.getUuid(), context.get(id), String.join(" ", context.get(labelText)))), id, label, ArgumentType.Literal("offline"), labelText);
        addSyntax(edit((player, context) -> editor.labelSource(player.getUuid(), context.get(id), context.get(sourceType), null)), id, label, ArgumentType.Literal("source"), sourceType);
        addSyntax(edit((player, context) -> editor.labelSource(player.getUuid(), context.get(id), context.get(sourceType), context.get(sourceName))), id, label, ArgumentType.Literal("source"), sourceType, sourceName);
        addSyntax(edit((player, context) -> editor.labelRemove(player.getUuid(), context.get(id))), id, label, ArgumentType.Literal("remove"));
        addSyntax(edit((player, context) -> editor.save(player.getUuid(), context.get(id))), id, ArgumentType.Literal("save"));
        addSyntax(edit((player, context) -> editor.cancel(player.getUuid(), context.get(id))), id, ArgumentType.Literal("cancel"));
        addSyntax(edit((player, context) -> editor.remove(player.getUuid(), context.get(id))), id, ArgumentType.Literal("remove"));
    }

    private void list(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (sender instanceof Player player) {
            player.sendMessage(PortalMessages.list(store.portals(), editor.drafts(player.getUuid())));
        }
    }

    private void show(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (sender instanceof Player player) {
            List<Portal> portals = store.portals();
            player.sendMessage(show.start(player, portals) ? PortalMessages.showing(portals.size()) : PortalMessages.nothingToShow());
        }
    }

    /** One editor call per syntax; the answer is printed and the preview follows the draft. */
    private CommandExecutor edit(BiFunction<Player, CommandContext, PortalEditResult> action) {
        return (sender, context) -> {
            if (sender instanceof Player player) {
                PortalEditResult result = action.apply(player, context);
                player.sendMessage(message(player, context.get(id), result));
                followPreview(player, context.get(id), result);
            }
        };
    }

    /** A guided draft that is still going gets its next step instead of the plain progress text. */
    private Component message(Player player, String draftId, PortalEditResult result) {
        boolean going = result instanceof PortalEditResult.Pending || result instanceof PortalEditResult.Complete;
        Optional<PortalDraft> guided = going ? editor.draft(player.getUuid(), draftId).filter(PortalDraft::guided) : Optional.empty();
        return guided.map(draft -> PortalFlow.render(draft, PortalCompletions.tasks(store.portals())).message()).orElseGet(() -> PortalMessages.render(result));
    }

    private static void suggest(CommandSender sender, Suggestion suggestion, Function<Player, List<String>> source) {
        if (sender instanceof Player player) {
            source.apply(player).forEach(entry -> suggestion.addEntry(new SuggestionEntry(entry)));
        }
    }

    private void followPreview(Player player, String id, PortalEditResult result) {
        switch (result) {
            case PortalEditResult.Pending ignored -> preview.start(player, id);
            case PortalEditResult.Complete ignored -> preview.start(player, id);
            case PortalEditResult.LabelUpdated ignored -> preview.start(player, id);
            case PortalEditResult.Saved ignored -> preview.stop(player.getUuid(), id);
            case PortalEditResult.Updated ignored -> preview.stop(player.getUuid(), id);
            case PortalEditResult.Removed ignored -> preview.stop(player.getUuid(), id);
            case PortalEditResult.Cancelled ignored -> preview.stop(player.getUuid(), id);
            case PortalEditResult.Rejected ignored -> {
            }
            case PortalEditResult.Invalid ignored -> {
            }
            case PortalEditResult.Unknown ignored -> {
            }
        }
    }

    private static Pos eye(Player player) {
        return player.getPosition().add(0, player.getEyeHeight(), 0);
    }
}
