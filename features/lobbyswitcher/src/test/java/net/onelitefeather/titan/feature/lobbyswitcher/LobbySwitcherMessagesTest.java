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
package net.onelitefeather.titan.feature.lobbyswitcher;

import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LobbySwitcherMessagesTest {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @DisplayName("A German locale renders the full message in German")
    @Test
    void germanLocaleRendersGerman() {
        try (LobbySwitcherMessages messages = new LobbySwitcherMessages()) {
            messages.register();

            String text = plain(messages.click(Locale.of("de", "DE"), SwitcherState.FULL, "Lobby-2"));

            Assertions.assertTrue(text.contains("voll"), "expected the German text, was: " + text);
        }
    }

    @DisplayName("A locale without a bundle falls back to English")
    @Test
    void unknownLocaleFallsBackToEnglish() {
        try (LobbySwitcherMessages messages = new LobbySwitcherMessages()) {
            messages.register();

            String text = plain(messages.click(Locale.of("ja", "JP"), SwitcherState.FULL, "Lobby-2"));

            Assertions.assertTrue(text.contains("full"), "expected the English text, was: " + text);
        }
    }

    @DisplayName("The sent message names the target lobby")
    @Test
    void sentMessageNamesTheTarget() {
        try (LobbySwitcherMessages messages = new LobbySwitcherMessages()) {
            messages.register();

            String text = plain(messages.click(Locale.ENGLISH, SwitcherState.JOINABLE, "Lobby-2"));

            Assertions.assertTrue(text.contains("Lobby-2"), "the message must name the target, was: " + text);
        }
    }

    @DisplayName("A failed check shows the unavailable message")
    @Test
    void errorShowsUnavailable() {
        try (LobbySwitcherMessages messages = new LobbySwitcherMessages()) {
            messages.register();

            Component error = messages.click(Locale.ENGLISH, SwitcherState.ERROR, "Lobby-2");

            Assertions.assertEquals(plain(messages.render(Component.translatable(LobbySwitcherMessages.MESSAGE_UNAVAILABLE), Locale.ENGLISH)), plain(error), "an error must read as the unavailable message");
        }
    }

    @DisplayName("Closing the messages removes the store from the translator")
    @Test
    void closeRemovesTheStore() {
        LobbySwitcherMessages messages = new LobbySwitcherMessages();
        messages.register();
        String whileRegistered = plain(GlobalTranslator.render(Component.translatable(LobbySwitcherMessages.MESSAGE_FULL), Locale.ENGLISH));

        messages.close();
        String afterClose = plain(GlobalTranslator.render(Component.translatable(LobbySwitcherMessages.MESSAGE_FULL), Locale.ENGLISH));

        Assertions.assertTrue(whileRegistered.contains("full"), "precondition: the store translates, was: " + whileRegistered);
        Assertions.assertFalse(afterClose.contains("full"), "without the store the key must stay untranslated, was: " + afterClose);
    }
}
