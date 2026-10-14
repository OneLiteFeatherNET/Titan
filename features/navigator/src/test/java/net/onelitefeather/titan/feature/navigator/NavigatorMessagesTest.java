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

    private String plain(Locale locale, boolean serverPicker, boolean portal) {
        return PlainTextComponentSerializer.plainText().serialize(this.messages.clickHint(locale, serverPicker, portal));
    }

    @DisplayName("Every click hint renders non-empty in German and in English")
    @Test
    void everyHintIsNonEmptyInBothLocales() {
        for (Locale locale : new Locale[]{Locale.GERMAN, Locale.ENGLISH}) {
            for (boolean serverPicker : new boolean[]{true, false}) {
                for (boolean portal : new boolean[]{true, false}) {
                    Assertions.assertFalse(plain(locale, serverPicker, portal).isBlank(), "empty hint for " + locale + " picker=" + serverPicker + " portal=" + portal);
                }
            }
        }
    }

    @DisplayName("The German hint differs from the English one for every combination")
    @Test
    void germanDiffersFromEnglish() {
        for (boolean serverPicker : new boolean[]{true, false}) {
            for (boolean portal : new boolean[]{true, false}) {
                Assertions.assertNotEquals(plain(Locale.ENGLISH, serverPicker, portal), plain(Locale.GERMAN, serverPicker, portal), "the bundles must differ, otherwise this test proves nothing");
            }
        }
    }

    @DisplayName("A picker mode without portal names the server choice in English")
    @Test
    void pickerHintInEnglish() {
        Assertions.assertEquals("Left click: choose a server", plain(Locale.ENGLISH, true, false));
    }

    @DisplayName("A picker mode without portal names the server choice in German")
    @Test
    void pickerHintInGerman() {
        Assertions.assertEquals("Linksklick: Server wählen", plain(Locale.GERMAN, true, false));
    }

    @DisplayName("A mode without picker and with portal names the connect and the portal click in English")
    @Test
    void connectAndPortalHintInEnglish() {
        Assertions.assertEquals("Left click: connect · Right click: walk to portal", plain(Locale.ENGLISH, false, true));
    }

    @DisplayName("A locale without a bundle falls back to English")
    @Test
    void unknownLocaleFallsBackToEnglish() {
        Assertions.assertEquals(plain(Locale.ENGLISH, false, false), plain(Locale.JAPAN, false, false));
    }
}
