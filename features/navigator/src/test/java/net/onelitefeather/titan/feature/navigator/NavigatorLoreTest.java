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
import java.util.OptionalInt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The lore of one mode entry as plain text, so the order and the wording are checked in both
 * languages.
 */
class NavigatorLoreTest {

    private static final String ELYTRA_GRADIENT = "<gradient:#fcba03:#03fc8c>";
    private static final List<ClickAction> MINIGAME_WITH_PORTAL = List.of(ClickAction.QUICK_JOIN, ClickAction.PICK_SERVER, ClickAction.PORTAL);
    private static final List<ClickAction> SERVER_WITHOUT_PORTAL = List.of(ClickAction.CONNECT);

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

    private static List<String> plain(List<Component> lines) {
        return lines.stream().map(line -> PlainTextComponentSerializer.plainText().serialize(line)).toList();
    }

    // The colour of the first visible character, which is the start of the gradient.
    private static TextColor firstColor(Component component, TextColor inherited) {
        TextColor color = component.color() != null ? component.color() : inherited;
        if (component instanceof TextComponent text && !text.content().isEmpty()) {
            return color;
        }
        for (Component child : component.children()) {
            TextColor found = firstColor(child, color);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @DisplayName("A minigame in German lists the separator, the players with the maximum, a blank line and the three click lines")
    @Test
    void germanMinigameWithPortal() {
        List<Component> lore = NavigatorMessages.modeLore(ELYTRA_GRADIENT, OptionalInt.of(13), OptionalInt.of(40), MINIGAME_WITH_PORTAL, Locale.GERMAN);
        Assertions.assertEquals(List.of("━━━━━━━━━━━━", "Spieler  13 / 40", "", "▸ Linksklick  Schnell beitreten", "▸ Rechtsklick  Server wählen", "▸ Shift-Klick  Zum Portal laufen"), plain(lore), "the German lore of ElytraRace with a portal");
    }

    @DisplayName("A minigame in English lists the same lines with English words")
    @Test
    void englishMinigameWithPortal() {
        List<Component> lore = NavigatorMessages.modeLore(ELYTRA_GRADIENT, OptionalInt.of(13), OptionalInt.of(40), MINIGAME_WITH_PORTAL, Locale.ENGLISH);
        Assertions.assertEquals(List.of("━━━━━━━━━━━━", "Players  13 / 40", "", "▸ Left click  Quick join", "▸ Right click  Choose a server", "▸ Shift click  Walk to portal"), plain(lore), "the English lore of ElytraRace with a portal");
    }

    @DisplayName("A server without a known maximum shows the count alone and one click line")
    @Test
    void germanServerWithoutMaximum() {
        List<Component> lore = NavigatorMessages.modeLore("<gradient:#7CFC00:#00C853>", OptionalInt.of(13), OptionalInt.empty(), SERVER_WITHOUT_PORTAL, Locale.GERMAN);
        Assertions.assertEquals(List.of("━━━━━━━━━━━━", "Spieler  13", "", "▸ Linksklick  Verbinden"), plain(lore), "the German lore of Survival without a limit");
    }

    @DisplayName("An unknown count drops the players line and keeps the blank line before the clicks")
    @Test
    void unknownCountHasNoPlayersLine() {
        List<Component> lore = NavigatorMessages.modeLore("<gradient:#7CFC00:#00C853>", OptionalInt.empty(), OptionalInt.empty(), SERVER_WITHOUT_PORTAL, Locale.ENGLISH);
        Assertions.assertEquals(List.of("━━━━━━━━━━━━", "", "▸ Left click  Connect"), plain(lore), "an unknown count must not read as an empty lobby");
    }

    @DisplayName("Zero players is a known count and still shows the maximum")
    @Test
    void zeroPlayersIsKnown() {
        List<Component> lore = NavigatorMessages.modeLore(ELYTRA_GRADIENT, OptionalInt.of(0), OptionalInt.of(40), SERVER_WITHOUT_PORTAL, Locale.ENGLISH);
        Assertions.assertEquals("Players  0 / 40", plain(lore).get(1), "zero is a count, not an unknown one");
    }

    @DisplayName("The separator carries the start colour of the mode's gradient")
    @Test
    void separatorUsesTheModeGradient() {
        List<Component> lore = NavigatorMessages.modeLore(ELYTRA_GRADIENT, OptionalInt.of(1), OptionalInt.of(2), SERVER_WITHOUT_PORTAL, Locale.GERMAN);
        Assertions.assertEquals(TextColor.fromHexString("#fcba03"), firstColor(lore.getFirst(), null), "the line starts in the ElytraRace gradient colour");
    }

    @DisplayName("Every lore line has text in both languages, except the one blank line before the clicks")
    @Test
    void everyLineHasText() {
        for (Locale locale : new Locale[]{Locale.GERMAN, Locale.ENGLISH}) {
            List<String> lines = plain(NavigatorMessages.modeLore(ELYTRA_GRADIENT, OptionalInt.of(3), OptionalInt.of(9), MINIGAME_WITH_PORTAL, locale));
            Assertions.assertEquals(1, lines.stream().filter(String::isEmpty).count(), "exactly one blank line in " + locale + ": " + lines);
            Assertions.assertTrue(lines.stream().filter(line -> !line.isEmpty()).noneMatch(String::isBlank), "every other line has text in " + locale);
        }
    }

    @DisplayName("The German and English lore differ, so the bundles are really used")
    @Test
    void languagesDiffer() {
        Assertions.assertNotEquals(plain(NavigatorMessages.modeLore(ELYTRA_GRADIENT, OptionalInt.of(3), OptionalInt.of(9), MINIGAME_WITH_PORTAL, Locale.GERMAN)), plain(NavigatorMessages.modeLore(ELYTRA_GRADIENT, OptionalInt.of(3), OptionalInt.of(9), MINIGAME_WITH_PORTAL, Locale.ENGLISH)), "the bundles must differ");
    }
}
