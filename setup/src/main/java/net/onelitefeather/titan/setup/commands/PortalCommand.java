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

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.setup.portal.DraftPreview;
import net.onelitefeather.titan.setup.portal.PortalDraft;
import net.onelitefeather.titan.setup.portal.PortalEditResult;
import net.onelitefeather.titan.setup.portal.PortalEditor;
import net.onelitefeather.titan.setup.portal.PortalMessages;
import net.onelitefeather.titan.setup.portal.PortalShow;
import net.onelitefeather.titan.setup.portal.PortalStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiFunction;

/**
 * {@code /setup portal ...}: parses arguments, hands them to the {@link PortalEditor} and prints
 * its answer. The rules live in the editor; this class holds none.
 */
public final class PortalCommand extends Command {

    private static final Argument<String> ID = ArgumentType.Word("id");
    private static final Argument<Double> RADIUS = ArgumentType.Double("value");
    private static final Argument<String[]> TASK = ArgumentType.StringArray("text");
    private static final Argument<String> PERMISSION = ArgumentType.Word("node");
    private static final Argument<String> FORM = ArgumentType.Word("kind").from("box", "ring");

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

        setDefaultExecutor((sender, context) -> sender.sendMessage(PortalMessages.usage()));
        addSyntax(this::list, ArgumentType.Literal("list"));
        addSyntax(this::show, ArgumentType.Literal("show"));
        addSyntax(edit((player, context) -> editor.corner1(player.getUuid(), context.get(ID), player.getPosition())), ID, ArgumentType.Literal("pos1"));
        addSyntax(edit((player, context) -> editor.corner2(player.getUuid(), context.get(ID), player.getPosition())), ID, ArgumentType.Literal("pos2"));
        addSyntax(edit((player, context) -> editor.shape(player.getUuid(), context.get(ID), context.get(FORM).equals("box") ? PortalDraft.Form.BOX : PortalDraft.Form.RING)), ID, ArgumentType.Literal("shape"), FORM);
        addSyntax(edit((player, context) -> editor.centre(player.getUuid(), context.get(ID), eye(player), player.getPosition().direction())), ID, ArgumentType.Literal("centre"));
        addSyntax(edit((player, context) -> editor.radius(player.getUuid(), context.get(ID), context.get(RADIUS))), ID, ArgumentType.Literal("radius"), RADIUS);
        addSyntax(edit((player, context) -> editor.disc(player.getUuid(), context.get(ID), eye(player), player.getPosition().direction(), context.get(RADIUS))), ID, ArgumentType.Literal("disc"), RADIUS);
        addSyntax(edit((player, context) -> editor.task(player.getUuid(), context.get(ID), String.join(" ", context.get(TASK)))), ID, ArgumentType.Literal("task"), TASK);
        addSyntax(edit((player, context) -> editor.permission(player.getUuid(), context.get(ID), context.get(PERMISSION))), ID, ArgumentType.Literal("permission"), PERMISSION);
        addSyntax(edit((player, context) -> editor.save(player.getUuid(), context.get(ID))), ID, ArgumentType.Literal("save"));
        addSyntax(edit((player, context) -> editor.cancel(player.getUuid(), context.get(ID))), ID, ArgumentType.Literal("cancel"));
        addSyntax(edit((player, context) -> editor.remove(player.getUuid(), context.get(ID))), ID, ArgumentType.Literal("remove"));
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
                player.sendMessage(PortalMessages.render(result));
                followPreview(player, context.get(ID), result);
            }
        };
    }

    private void followPreview(Player player, String id, PortalEditResult result) {
        switch (result) {
            case PortalEditResult.Pending ignored -> preview.start(player, id);
            case PortalEditResult.Complete ignored -> preview.start(player, id);
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
