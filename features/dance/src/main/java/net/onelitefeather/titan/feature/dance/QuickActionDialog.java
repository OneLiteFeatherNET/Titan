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

import java.util.List;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogActionButton;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.dialog.DialogMetadata;

/**
 * The static quick-action dialog. Registry data reaches every client identically during
 * configuration,
 * so the dialog keeps a neutral text and only opens the per-player choice on click.
 */
final class QuickActionDialog {

    private QuickActionDialog() {
    }

    /** @return the one-button dialog the quick action shows */
    static Dialog quickAction() {
        DialogMetadata metadata = new DialogMetadata(Component.text("Dances"), null, true, false, DialogAfterAction.CLOSE, List.of(), List.of());
        return new Dialog.Notice(metadata, new DialogActionButton(Component.text("Open"), null, DialogActionButton.DEFAULT_WIDTH, new DialogAction.Custom(DanceKeys.OPEN, null)));
    }
}
