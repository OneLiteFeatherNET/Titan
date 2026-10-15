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

import java.util.List;
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

    private List<String> plain(Locale locale, boolean serverPicker, boolean portal) {
        return this.messages.clickHints(locale, serverPicker, portal).stream().map(line -> PlainTextComponentSerializer.plainText().serialize(line)).toList();
    }

    @DisplayName("Every click hint line renders non-empty in German and in English")
    @Test
    void everyHintIsNonEmptyInBothLocales() {
        for (Locale locale : new Locale[]{Locale.GERMAN, Locale.ENGLISH}) {
            for (boolean serverPicker : new boolean[]{true, false}) {
                for (boolean portal : new boolean[]{true, false}) {
                    for (String line : plain(locale, serverPicker, portal)) {
                        Assertions.assertFalse(line.isBlank(), "empty hint line for " + locale + " picker=" + serverPicker + " portal=" + portal);
                    }
                }
            }
        }
    }

    @DisplayName("The German hints differ from the English ones for every combination")
    @Test
    void germanDiffersFromEnglish() {
        for (boolean serverPicker : new boolean[]{true, false}) {
            for (boolean portal : new boolean[]{true, false}) {
                Assertions.assertNotEquals(plain(Locale.ENGLISH, serverPicker, portal), plain(Locale.GERMAN, serverPicker, portal), "the bundles must differ, otherwise this test proves nothing");
            }
        }
    }

    @DisplayName("A picker mode without portal has only the server-choice line in English")
    @Test
    void pickerHintInEnglish() {
        Assertions.assertEquals(List.of("Left click: choose a server"), plain(Locale.ENGLISH, true, false));
    }

    @DisplayName("A picker mode without portal has only the server-choice line in German")
    @Test
    void pickerHintInGerman() {
        Assertions.assertEquals(List.of("Linksklick: Server wählen"), plain(Locale.GERMAN, true, false));
    }

    @DisplayName("A connect mode with portal has the connect line and the portal line in English, on separate lines")
    @Test
    void connectAndPortalHintInEnglish() {
        Assertions.assertEquals(List.of("Left click: connect", "Right click: walk to portal"), plain(Locale.ENGLISH, false, true));
    }

    @DisplayName("A picker mode with portal has the server-choice line and the portal line in German, on separate lines")
    @Test
    void pickerAndPortalHintInGerman() {
        Assertions.assertEquals(List.of("Linksklick: Server wählen", "Rechtsklick: zum Portal laufen"), plain(Locale.GERMAN, true, true));
    }

    @DisplayName("A locale without a bundle falls back to English")
    @Test
    void unknownLocaleFallsBackToEnglish() {
        Assertions.assertEquals(List.of("Left click: connect"), plain(Locale.JAPAN, false, false), "a locale without a bundle must read the English text");
    }
}
