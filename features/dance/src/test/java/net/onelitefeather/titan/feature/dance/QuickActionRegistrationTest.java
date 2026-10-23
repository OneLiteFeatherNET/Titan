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

import net.kyori.adventure.key.Key;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogTags;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.registry.RegistryTag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Checks the quick-action entry against a fresh dialog registry. The tag write goes through the
 * reflective call in {@link QuickActionRegistration}, so this test also guards that call against a
 * Minestom upgrade that renames it.
 */
class QuickActionRegistrationTest {

    @DisplayName("The quick-action dialog is registered under titan:dances")
    @Test
    void dialogIsRegisteredUnderItsKey() {
        DynamicRegistry<Dialog> dialogs = DynamicRegistry.create(Key.key("dialog"));

        QuickActionRegistration.register(dialogs);

        Assertions.assertNotNull(dialogs.getKey(DanceKeys.DIALOG), "titan:dances must be in the dialog registry");
    }

    @DisplayName("The quick-action tag contains the registered dialog")
    @Test
    void tagContainsTheDialog() {
        DynamicRegistry<Dialog> dialogs = DynamicRegistry.create(Key.key("dialog"));

        QuickActionRegistration.register(dialogs);

        RegistryTag<Dialog> tag = dialogs.getTag(DialogTags.QUICK_ACTIONS);
        RegistryKey<Dialog> key = dialogs.getKey(DanceKeys.DIALOG);
        Assertions.assertNotNull(tag, "the quick-action tag must exist after registration");
        Assertions.assertTrue(tag.contains(key), "titan:dances must be a member of minecraft:quick_actions");
    }

    @DisplayName("The quick-action dialog has exactly one button, which opens the choice")
    @Test
    void quickActionHasOneOpenButton() {
        Dialog.Notice notice = (Dialog.Notice) QuickActionRegistration.quickAction();

        Assertions.assertInstanceOf(DialogAction.Custom.class, notice.action().action(), "the one button must send a custom action");
        Assertions.assertEquals(DanceKeys.OPEN, ((DialogAction.Custom) notice.action().action()).key(), "the button opens titan:dances/open");
    }
}
