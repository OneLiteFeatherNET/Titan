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

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogActionButton;
import net.minestom.server.registry.DynamicRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The static dance lists: their keys, how many there are, and what each one offers. */
class DanceListTest {

    private DanceMessages messages;

    @BeforeEach
    void register() {
        this.messages = new DanceMessages();
        this.messages.register();
    }

    @AfterEach
    void unregister() {
        this.messages.close();
    }

    @DisplayName("Every language and permission variant has its own list, two languages times two variants")
    @Test
    void listsAreBounded() {
        Assertions.assertEquals(2, DanceList.languages().size(), "German and English");
        Assertions.assertEquals(2, DanceList.variants().size(), "the free dances, with and without spin");
    }

    @DisplayName("The list keys name the language and the permission variant")
    @Test
    void keysNameLanguageAndVariant() {
        Assertions.assertEquals(Key.key("titan", "dances/de"), DanceList.key(Locale.GERMAN, EnumSet.of(Dance.WAVE)), "German without spin");
        Assertions.assertEquals(Key.key("titan", "dances/en_spin"), DanceList.key(Locale.ENGLISH, EnumSet.allOf(Dance.class)), "English with spin");
    }

    @DisplayName("A language other than German falls back to English")
    @Test
    void otherLanguagesFallBackToEnglish() {
        Assertions.assertEquals(Locale.GERMAN, DanceList.languageOf(Locale.GERMANY), "German regions are German");
        Assertions.assertEquals(Locale.ENGLISH, DanceList.languageOf(Locale.FRANCE), "French falls back to English");
        Assertions.assertEquals(Locale.ENGLISH, DanceList.languageOf(Locale.US), "English is English");
    }

    @DisplayName("Registering the lists puts one dialog under each key")
    @Test
    void registrationPutsEveryListUnderItsKey() {
        DynamicRegistry<Dialog> dialogs = DynamicRegistry.create(Key.key("titan", "test_dance_lists"));
        new DanceMenu(this.messages, new DancePermissions(new FakePermissionService().grant("titan.dance.spin"))).register(dialogs);

        for (Locale language : DanceList.languages()) {
            for (Set<Dance> permitted : DanceList.variants()) {
                Assertions.assertNotNull(dialogs.getKey(DanceList.key(language, permitted)), "a list must be registered for " + language + " " + permitted);
            }
        }
    }

    @DisplayName("Each registered list offers exactly its permitted dances, then Stop, in its language")
    @Test
    void eachRegisteredListOffersItsDances() {
        DynamicRegistry<Dialog> dialogs = DynamicRegistry.create(Key.key("titan", "test_dance_lists"));
        new DanceMenu(this.messages, new DancePermissions(new FakePermissionService())).register(dialogs);

        Dialog.MultiAction german = (Dialog.MultiAction) dialogs.get(DanceList.key(Locale.GERMAN, EnumSet.of(Dance.WAVE, Dance.GROOVE, Dance.PARTY)));
        Assertions.assertEquals(List.of("Winken", "Groove", "Party", "Stopp"), labels(german), "the German list without spin");

        Dialog.MultiAction english = (Dialog.MultiAction) dialogs.get(DanceList.key(Locale.ENGLISH, EnumSet.allOf(Dance.class)));
        Assertions.assertEquals(List.of("Wave", "Groove", "Party", "Spin", "Stop"), labels(english), "the English list with spin");
    }

    private static List<String> labels(Dialog.MultiAction dialog) {
        return dialog.actions().stream().map(DialogActionButton::label).map(label -> PlainTextComponentSerializer.plainText().serialize(label)).toList();
    }
}
