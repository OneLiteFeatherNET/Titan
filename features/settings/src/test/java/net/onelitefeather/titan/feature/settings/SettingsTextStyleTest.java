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
package net.onelitefeather.titan.feature.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SettingsTextStyleTest {

    private SettingsMessages messages;

    @BeforeEach
    void registerTexts() {
        this.messages = new SettingsMessages();
        this.messages.register();
    }

    @AfterEach
    void unregisterTexts() {
        this.messages.close();
    }

    @DisplayName("The hotbar item is named Settings in the brand gradient and is not italic")
    @Test
    void itemNameIsGradientAndNotItalic() {
        Component name = this.messages.itemName(Locale.ENGLISH);
        Assertions.assertEquals("Settings", plain(name), "the item must be named Settings");
        Assertions.assertEquals(TextDecoration.State.FALSE, name.decoration(TextDecoration.ITALIC), "the item name must not be italic");
        Assertions.assertEquals(TextColor.fromHexString("#00ddff"), firstColour(name), "the item name must start with the brand gradient");
    }

    @DisplayName("The hotbar item has one non-italic hint line in each language")
    @Test
    void itemHintIsNonItalicInBothLanguages() {
        Assertions.assertEquals("Right-click to open", plain(this.messages.itemHint(Locale.ENGLISH)), "the English hint must say how to open the menu");
        Assertions.assertEquals("Rechtsklick zum Öffnen", plain(this.messages.itemHint(Locale.GERMAN)), "the German hint must say how to open the menu");
        Assertions.assertEquals(TextDecoration.State.FALSE, this.messages.itemHint(Locale.GERMAN).decoration(TextDecoration.ITALIC), "the hint must not be italic");
    }

    @DisplayName("The menu title ends with the brand gradient's last colour")
    @Test
    void titleIsGradient() {
        Assertions.assertEquals(TextColor.fromHexString("#ffffff"), lastColour(this.messages.title(Locale.ENGLISH)), "the title must end with the gradient's last colour");
    }

    @DisplayName("The switch shows its state in green when on and in red when off")
    @Test
    void switchStateIsColoured() {
        Assertions.assertEquals("✔ On", plain(SettingsMessages.toggleName(true, Locale.ENGLISH)), "an option that is on must read ✔ On");
        Assertions.assertEquals(NamedTextColor.GREEN, firstColour(SettingsMessages.toggleName(true, Locale.ENGLISH)), "the on state must be green");
        Assertions.assertEquals("✘ Aus", plain(SettingsMessages.toggleName(false, Locale.GERMAN)), "an option that is off must read ✘ Aus");
        Assertions.assertEquals(NamedTextColor.RED, firstColour(SettingsMessages.toggleName(false, Locale.GERMAN)), "the off state must be red");
    }

    @DisplayName("An option description is not italic")
    @Test
    void descriptionIsNotItalic() {
        Component description = SettingsMessages.description(SettingsFixtures.TAG, Locale.ENGLISH);
        Assertions.assertEquals(TextDecoration.State.FALSE, description.decoration(TextDecoration.ITALIC), "the description must not be italic");
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static TextColor firstColour(Component component) {
        return colours(component).getFirst();
    }

    private static TextColor lastColour(Component component) {
        return colours(component).getLast();
    }

    private static List<TextColor> colours(Component component) {
        List<TextColor> colours = new ArrayList<>();
        collect(component, null, colours);
        Assertions.assertFalse(colours.isEmpty(), "the text must carry a colour");
        return colours;
    }

    private static void collect(Component component, TextColor inherited, List<TextColor> colours) {
        TextColor colour = component.color() != null ? component.color() : inherited;
        if (component instanceof TextComponent text && !text.content().isEmpty() && colour != null) {
            colours.add(colour);
        }
        for (Component child : component.children()) {
            collect(child, colour, colours);
        }
    }
}
