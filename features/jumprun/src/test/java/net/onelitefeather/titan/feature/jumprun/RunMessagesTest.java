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
package net.onelitefeather.titan.feature.jumprun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunMessagesTest {

    private RunMessages messages;

    @BeforeEach
    void register() {
        messages = new RunMessages();
        messages.register();
    }

    @AfterEach
    void unregister() {
        messages.close();
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static long sourceCount() {
        long count = 0;
        for (var ignored : GlobalTranslator.translator().sources()) {
            count++;
        }
        return count;
    }

    @Test
    void germanLocaleRendersGermanEndMessageWithScore() {
        assertEquals("Jump & Run » Lauf beendet. Punkte: 9", plain(messages.endScore(Locale.GERMANY, 9)));
    }

    @Test
    void englishLocaleRendersEnglishEndMessageWithScore() {
        assertEquals("Jump & Run » Run over. Score: 9", plain(messages.endScore(Locale.US, 9)));
    }

    @Test
    void unknownLocaleFallsBackToEnglish() {
        assertEquals("Jump & Run » Run over. Score: 9", plain(messages.endScore(Locale.JAPAN, 9)));
    }

    @Test
    void recordMessageCarriesTheScore() {
        assertTrue(plain(messages.endRecord(Locale.US, 15)).contains("15"), "record message shows the score");
    }

    @Test
    void actionBarCarriesTheScore() {
        assertTrue(plain(messages.scoreActionBar(Locale.US, 7)).endsWith("7"), "action bar shows the score");
    }

    @Test
    void noSpaceMessageIsRenderedNotEmpty() {
        assertFalse(plain(messages.noSpace(Locale.JAPAN)).isBlank(), "no-space message must render");
    }

    @Test
    void closeRemovesTheStoreFromTheGlobalTranslator() {
        long registered = sourceCount();
        messages.close();
        assertEquals(registered - 1, sourceCount(), "close() removes exactly its own store");
        messages.register();
        assertEquals(registered, sourceCount(), "register() adds it again");
    }
}
