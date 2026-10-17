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
package net.onelitefeather.titan.feature.navigator;

import java.util.Locale;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Each test registers its own message store and removes it again, so no test sees another's. */
class NavigatorMessagesTest {

    private NavigatorMessages messages;

    @BeforeEach
    void register() {
        this.messages = new NavigatorMessages();
        this.messages.register();
    }

    @AfterEach
    void unregister() {
        this.messages.close();
    }

    private static String plain(String key, Locale locale) {
        return PlainTextComponentSerializer.plainText().serialize(NavigatorMessages.text(key, locale));
    }

    @DisplayName("Every key renders non-empty in German and in English")
    @Test
    void everyKeyIsNonEmptyInBothLocales() {
        for (Locale locale : new Locale[]{Locale.GERMAN, Locale.ENGLISH}) {
            for (String key : NavigatorMessages.KEYS) {
                Assertions.assertFalse(plain(key, locale).isBlank(), "the key " + key + " renders empty in " + locale);
            }
        }
    }

    @DisplayName("The German texts differ from the English ones, except the title, which is the same word in both")
    @Test
    void germanDiffersFromEnglish() {
        for (String key : NavigatorMessages.KEYS) {
            if (!NavigatorMessages.TITLE.equals(key)) {
                Assertions.assertNotEquals(plain(key, Locale.ENGLISH), plain(key, Locale.GERMAN), "the bundles must differ for " + key + ", otherwise this test proves nothing");
            }
        }
    }

    @DisplayName("A locale without a bundle reads the English text")
    @Test
    void unknownLocaleFallsBackToEnglish() {
        Assertions.assertEquals("Connect", plain(NavigatorMessages.ACTION_CONNECT, Locale.JAPAN), "a locale without a bundle must read the English text");
    }

    @DisplayName("The shift line is named Shift click in English and Shift-Klick in German")
    @Test
    void shiftLineIsNamedInBothLanguages() {
        Assertions.assertEquals("Shift click", plain(NavigatorMessages.KEY_SHIFT, Locale.ENGLISH), "the English key of the shift line");
        Assertions.assertEquals("Shift-Klick", plain(NavigatorMessages.KEY_SHIFT, Locale.GERMAN), "the German key of the shift line");
    }
}
