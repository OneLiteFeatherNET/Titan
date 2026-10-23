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
package net.onelitefeather.titan.feature.npc;

import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NpcMessagesBundleTest {

    @DisplayName("Every rendered key has a German and an English text")
    @Test
    void everyKeyIsInBothLanguages() {
        for (Locale language : NpcMessages.LANGUAGES) {
            ResourceBundle bundle = ResourceBundle.getBundle("titan.npc.messages", language);
            for (String key : NpcMessages.KEYS) {
                Assertions.assertTrue(bundle.containsKey(key), "the " + language + " bundle must contain " + key);
            }
        }
    }

    @DisplayName("Every sign key is a rendered key")
    @Test
    void signKeysAreRendered() {
        Assertions.assertTrue(NpcMessages.KEYS.containsAll(NpcMessages.SIGN_KEYS), "a sign key that is not rendered would show as raw text");
    }

    @DisplayName("A player with a German client sees German, anyone else English")
    @Test
    void languageFollowsTheClient() {
        Assertions.assertEquals(Locale.GERMAN, NpcMessages.languageOf(Locale.GERMANY), "a German client gets German");
        Assertions.assertEquals(Locale.ENGLISH, NpcMessages.languageOf(Locale.FRANCE), "any other client gets English");
        Assertions.assertEquals(Locale.ENGLISH, NpcMessages.languageOf(null), "an unknown client gets English");
    }
}
