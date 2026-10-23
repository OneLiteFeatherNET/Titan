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

import java.lang.reflect.Method;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogActionButton;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.dialog.DialogMetadata;
import net.minestom.server.dialog.DialogTags;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.registry.RegistryTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Puts the static quick-action dialog into the dialog registry and its quick-action tag. Registry
 * data reaches every client identically during configuration, so the dialog keeps a neutral text
 * and only opens the per-player choice on click.
 *
 * <p>Minestom 2026.10.07-26.2 has no public way to add an entry to a tag: the only writer of a
 * backed tag is the package-private {@code RegistryTagImpl.Backed#add}. It is called once, by
 * reflection; if that fails, the column logs a warning and the {@code /tanz} command still works.
 */
final class QuickActionRegistration {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuickActionRegistration.class);

    private static final String BACKED_TAG = "net.minestom.server.registry.RegistryTagImpl$Backed";
    private static final String ADD = "add";

    private QuickActionRegistration() {
    }

    /** Registers the dialog and adds it to the quick-action tag. */
    static void register(DynamicRegistry<Dialog> dialogs) {
        RegistryKey<Dialog> key = dialogs.register(DanceKeys.DIALOG, quickAction());
        addToQuickActions(dialogs, key);
    }

    /** @return the one-button dialog the quick action shows */
    static Dialog quickAction() {
        DialogMetadata metadata = new DialogMetadata(Component.text("Dances"), null, true, false, DialogAfterAction.CLOSE, List.of(), List.of());
        return new Dialog.Notice(metadata, new DialogActionButton(Component.text("Open"), null, DialogActionButton.DEFAULT_WIDTH, new DialogAction.Custom(DanceKeys.OPEN, null)));
    }

    private static void addToQuickActions(DynamicRegistry<Dialog> dialogs, RegistryKey<Dialog> key) {
        RegistryTag<Dialog> tag = dialogs.getOrCreateTag(DialogTags.QUICK_ACTIONS);
        try {
            Method add = Class.forName(BACKED_TAG).getDeclaredMethod(ADD, RegistryKey.class);
            add.setAccessible(true);
            add.invoke(tag, key);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Could not add {} to the quick-action tag; the /tanz command still opens the dances: {}", DanceKeys.DIALOG, e.toString());
        }
    }
}
