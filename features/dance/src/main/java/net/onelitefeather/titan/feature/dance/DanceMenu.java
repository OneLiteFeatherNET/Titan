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
package net.onelitefeather.titan.feature.dance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogActionButton;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.dialog.DialogMetadata;
import net.minestom.server.entity.Player;
import net.minestom.server.registry.DynamicRegistry;

/**
 * The dance list: the dances a player may use, then Stop. It is registered once per language and
 * permission variant ({@link DanceList}), and each player's quick action points at his own variant.
 * The list only shows the choice; {@link DanceService} checks the permission again on a click.
 */
final class DanceMenu {

    private static final int COLUMNS = 2;

    private final DanceMessages messages;
    private final DancePermissions permissions;

    DanceMenu(DanceMessages messages, DancePermissions permissions) {
        this.messages = messages;
        this.permissions = permissions;
    }

    /**
     * Registers every static list. The client receives the registry during configuration, so this
     * runs before the server accepts a connection, after the messages are registered.
     */
    void register(DynamicRegistry<Dialog> dialogs) {
        for (Locale language : DanceList.languages()) {
            for (Set<Dance> permitted : DanceList.variants()) {
                dialogs.register(DanceList.key(language, permitted), dialog(language, permitted));
            }
        }
    }

    /** Shows the list to {@code player} alone, in the player's language. */
    void show(Player player) {
        player.showDialog(dialogFor(player.getUuid(), player.getLocale()));
    }

    /** @return the list {@code playerId} gets, its labels rendered for his language */
    Dialog.MultiAction dialogFor(UUID playerId, Locale locale) {
        return dialog(DanceList.languageOf(locale), this.permissions.permittedTo(playerId));
    }

    /** @return the key of the registered list that {@code playerId} gets in his quick action */
    Key keyFor(UUID playerId, Locale locale) {
        return DanceList.key(DanceList.languageOf(locale), this.permissions.permittedTo(playerId));
    }

    private Dialog.MultiAction dialog(Locale language, Set<Dance> permitted) {
        List<DialogActionButton> buttons = new ArrayList<>();
        for (Dance dance : Dance.values()) {
            if (permitted.contains(dance)) {
                buttons.add(danceButton(dance, DanceMessages.text(dance.labelKey(), language)));
            }
        }
        buttons.add(new DialogActionButton(DanceMessages.text(DanceMessages.STOP, language), null, DialogActionButton.DEFAULT_WIDTH, new DialogAction.Custom(DanceKeys.STOP, null)));
        return new Dialog.MultiAction(metadata(DanceMessages.text(DanceMessages.TITLE, language)), List.copyOf(buttons), null, COLUMNS);
    }

    static DialogActionButton danceButton(Dance dance, Component label) {
        return new DialogActionButton(label, null, DialogActionButton.DEFAULT_WIDTH, new DialogAction.Custom(DanceKeys.DANCE, DancePayload.of(dance)));
    }

    private static DialogMetadata metadata(Component title) {
        return new DialogMetadata(title, null, true, false, DialogAfterAction.CLOSE, List.of(), List.of());
    }
}
