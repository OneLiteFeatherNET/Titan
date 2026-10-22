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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The server picker in the navigator's style, checked as plain text so the wording is in the test.
 */
@ExtendWith(MicrotusExtension.class)
class ServerPickerStyleTest {

    private static final ServiceCount JOINABLE = new ServiceCount("ElytraRace-1", 13, 40);
    private static final ServiceCount FULL = new ServiceCount("ElytraRace-2", 40, 40);
    private static final ServiceCount NOT_READY = new ServiceCount("ElytraRace-3", 3, 0);

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

    private static String nameOf(ItemStack stack) {
        return PlainTextComponentSerializer.plainText().serialize(stack.get(DataComponents.CUSTOM_NAME));
    }

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

    @DisplayName("The picker title is translated, in German and in English")
    @Test
    void titleIsTranslated() {
        Assertions.assertEquals("Server wählen", PlainTextComponentSerializer.plainText().serialize(ServerPicker.title(Locale.GERMAN)), "the German title");
        Assertions.assertEquals("Choose a server", PlainTextComponentSerializer.plainText().serialize(ServerPicker.title(Locale.ENGLISH)), "the English title");
    }

    @DisplayName("A joinable server lists its players against the limit and the join state")
    @Test
    void joinableLore() {
        Assertions.assertEquals(List.of("Spieler  13 / 40", "", "▸ Klicken zum Beitreten"), plain(ServerPicker.lore(JOINABLE, Locale.GERMAN)), "the German entry of a joinable server");
        Assertions.assertEquals(List.of("Players  13 / 40", "", "▸ Click to join"), plain(ServerPicker.lore(JOINABLE, Locale.ENGLISH)), "the English entry of a joinable server");
    }

    @DisplayName("A full server says so in its state line")
    @Test
    void fullLore() {
        Assertions.assertEquals(List.of("Spieler  40 / 40", "", "▸ Voll"), plain(ServerPicker.lore(FULL, Locale.GERMAN)), "the German state of a full server");
        Assertions.assertEquals(List.of("Players  40 / 40", "", "▸ Full"), plain(ServerPicker.lore(FULL, Locale.ENGLISH)), "the English state of a full server");
    }

    @DisplayName("A server without a limit shows only the count and that it is not ready")
    @Test
    void notReadyLore() {
        Assertions.assertEquals(List.of("Spieler  3", "", "▸ Noch nicht bereit"), plain(ServerPicker.lore(NOT_READY, Locale.GERMAN)), "a server without a limit has no maximum to show");
    }

    @DisplayName("The entry name carries the gradient of its destination")
    @Test
    void nameUsesTheDestinationGradient() {
        Component name = ServerPicker.name(Destination.ELYTRA_RACE, JOINABLE);
        Assertions.assertEquals("ElytraRace-1", PlainTextComponentSerializer.plainText().serialize(name), "the name is the service name");
        Assertions.assertEquals(TextColor.fromHexString("#fcba03"), firstColor(name, null), "the name starts in the ElytraRace gradient");
    }

    @DisplayName("The state is the colour of the entry's icon: red when full, yellow when not ready, lime when joinable")
    @Test
    void iconMaterialFollowsTheState() {
        Assertions.assertEquals(Material.RED_CONCRETE, ServerPicker.material(FULL), "a full server is red");
        Assertions.assertEquals(Material.YELLOW_CONCRETE, ServerPicker.material(NOT_READY), "a server without a limit is yellow");
        Assertions.assertEquals(Material.LIME_CONCRETE, ServerPicker.material(JOINABLE), "a joinable server is lime");
    }

    @DisplayName("A refused join says why, in the player's language")
    @Test
    void refusalIsTranslated() {
        Assertions.assertEquals("Servers » Dieser Server ist voll.", PlainTextComponentSerializer.plainText().serialize(ServerPicker.refusal(FULL, Locale.GERMAN)), "the German refusal of a full server");
        Assertions.assertEquals("Servers » That server is not ready yet.", PlainTextComponentSerializer.plainText().serialize(ServerPicker.refusal(NOT_READY, Locale.ENGLISH)), "the English refusal of a server without a limit");
    }

    @DisplayName("The sort item is named after the order that is active, in German and in English")
    @Test
    void sortItemNamesTheActiveOrder(Env env) {
        Assertions.assertEquals("Sortierung: Freie Plätze", nameOf(ServerPicker.sortItem(PickerSort.MOST_FREE, Locale.GERMAN)), "the German name of the sort item");
        Assertions.assertEquals("Sort: Free slots", nameOf(ServerPicker.sortItem(PickerSort.MOST_FREE, Locale.ENGLISH)), "the English name of the sort item");
    }

    @DisplayName("The sort item's lore lists all four orders and marks only the active one")
    @Test
    void sortLoreListsEveryOrder() {
        Assertions.assertEquals(List.of("  Name (A bis Z)", "  Meiste Spieler", "  Freie Plätze", "▸ Status", "", "▸ Klicken, um die Ordnung zu wechseln"), plain(ServerPicker.sortLore(PickerSort.STATUS, Locale.GERMAN)), "German: status is the active order");
        Assertions.assertEquals(List.of("▸ Name (A–Z)", "  Most players", "  Free slots", "  Status", "", "▸ Click to change the order"), plain(ServerPicker.sortLore(PickerSort.NAME, Locale.ENGLISH)), "English: name is the active order");
    }

    @DisplayName("The active order of the sort item is cyan, the others are grey")
    @Test
    void sortLoreColoursTheActiveOrder() {
        List<Component> lore = ServerPicker.sortLore(PickerSort.MOST_PLAYERS, Locale.ENGLISH);

        Assertions.assertEquals(TextColor.fromHexString("#00ddff"), firstColor(lore.get(1), null), "the active order is cyan");
        Assertions.assertEquals(NamedTextColor.GRAY, firstColor(lore.get(0), null), "an inactive order is grey");
        Assertions.assertEquals(NamedTextColor.GRAY, firstColor(lore.get(3), null), "every inactive order is grey");
    }
}
