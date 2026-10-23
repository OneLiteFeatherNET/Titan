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
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogActionButton;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.dialog.DialogMetadata;
import net.minestom.server.entity.Player;

/**
 * The per-player dance choice: the dances the player may use, then Stop. The menu only shows the
 * choice; {@link DanceService} checks the permission again when a button is clicked.
 */
final class DanceMenu {

    private static final int COLUMNS = 2;

    private final DanceMessages messages;
    private final DancePermissions permissions;

    DanceMenu(DanceMessages messages, DancePermissions permissions) {
        this.messages = messages;
        this.permissions = permissions;
    }

    /** Shows the choice to {@code player} alone, in the player's language. */
    void show(Player player) {
        player.showDialog(dialogFor(player.getUuid(), player.getLocale()));
    }

    /** @return the choice for {@code playerId}, its labels rendered for {@code locale} */
    Dialog.MultiAction dialogFor(UUID playerId, Locale locale) {
        List<DialogActionButton> buttons = new ArrayList<>();
        for (Dance dance : Dance.values()) {
            if (this.permissions.may(playerId, dance)) {
                buttons.add(danceButton(dance, DanceMessages.text(dance.labelKey(), locale)));
            }
        }
        buttons.add(new DialogActionButton(DanceMessages.text(DanceMessages.STOP, locale), null, DialogActionButton.DEFAULT_WIDTH, new DialogAction.Custom(DanceKeys.STOP, null)));
        return new Dialog.MultiAction(metadata(DanceMessages.text(DanceMessages.TITLE, locale)), List.copyOf(buttons), null, COLUMNS);
    }

    static DialogActionButton danceButton(Dance dance, Component label) {
        return new DialogActionButton(label, null, DialogActionButton.DEFAULT_WIDTH, new DialogAction.Custom(DanceKeys.DANCE, DancePayload.of(dance)));
    }

    private static DialogMetadata metadata(Component title) {
        return new DialogMetadata(title, null, true, false, DialogAfterAction.CLOSE, List.of(), List.of());
    }
}
