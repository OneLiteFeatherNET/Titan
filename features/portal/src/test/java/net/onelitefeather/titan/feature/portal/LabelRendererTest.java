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
package net.onelitefeather.titan.feature.portal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.titan.core.portal.Billboard;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.PortalLabel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LabelRendererTest {

    private static Portal portal(String task, PortalLabel label) {
        return new Portal("p", new Box(new Vec(0, 0, 0), new Vec(1, 1, 1)), task, null, label);
    }

    private static PortalLabel label(String text, String offlineText) {
        return new PortalLabel(new Vec(0, 64, 0), text, offlineText, null, Billboard.CENTER, 0f);
    }

    private static Component render(String task, PortalLabel label, LabelReading reading) {
        return LabelRenderer.render(portal(task, label), label, reading);
    }

    private static LabelReading remote(int online, int max, boolean running) {
        return new LabelReading.Remote(new PlayerCount(online, max, running));
    }

    /**
     * The colour a leaf with this content ends up with, inherited from its parents like a client
     * does.
     */
    private static TextColor colorOf(Component component, String content) {
        return colorOf(component, content, null);
    }

    private static TextColor colorOf(Component component, String content, TextColor inherited) {
        TextColor own = component.color() != null ? component.color() : inherited;
        if (component instanceof TextComponent text && text.content().equals(content)) {
            return own;
        }
        for (Component child : component.children()) {
            TextColor found = colorOf(child, content, own);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @DisplayName("12 of 40 players show as '12/40' with the colours of the text")
    @Test
    void numbersAreInserted() {
        PortalLabel label = label("<gold>Survival<newline><gray><online>/<max> Spieler", null);

        Component rendered = render("Survival", label, remote(12, 40, true));

        Assertions.assertEquals("Survival\n12/40 Spieler", plain(rendered), "the text with both numbers inserted");
        Assertions.assertEquals(NamedTextColor.GOLD, colorOf(rendered, "Survival\n"), "the first line keeps its gold");
        Assertions.assertEquals(NamedTextColor.GRAY, colorOf(rendered, "12/40 Spieler"), "the second line is gray, the numbers included");
    }

    @DisplayName("<task> shows the task of the portal")
    @Test
    void taskNameIsInserted() {
        Assertions.assertEquals("Survival", plain(render("Survival", label("<task>", null), remote(0, 0, true))));
    }

    @DisplayName("<prefix> is left to the MiniMessage parser, so the global provider resolves it")
    @Test
    void prefixComesFromTheParser() {
        MiniMessage parser = MiniMessage.builder().tags(TagResolver.resolver(TagResolver.standard(), Placeholder.component("prefix", Component.text("Titan")))).build();
        PortalLabel label = label("<prefix> <online>", null);

        Component rendered = LabelRenderer.render(parser, portal("Survival", label), label, remote(3, 20, true));

        Assertions.assertEquals("Titan 3", plain(rendered), "<prefix> must be resolved by the parser, <online> by the renderer");
    }

    @DisplayName("A source that is not running shows the offline text")
    @Test
    void offlineTextIsShown() {
        PortalLabel label = label("<online>/<max>", "<red>Survival startet gleich");

        Component rendered = render("Survival", label, remote(0, 0, false));

        Assertions.assertEquals("Survival startet gleich", plain(rendered), "the offline text replaces the text");
        Assertions.assertEquals(NamedTextColor.RED, colorOf(rendered, "Survival startet gleich"), "the offline text keeps its red");
    }

    @DisplayName("Without an offline text a source that is not running shows the text with 0/0")
    @Test
    void textWithZerosWithoutOfflineText() {
        Assertions.assertEquals("0/0", plain(render("Survival", label("<online>/<max>", null), remote(7, 9, false))));
    }

    @DisplayName("The offline text may use the placeholders too, with zeros")
    @Test
    void offlineTextUsesPlaceholders() {
        Assertions.assertEquals("Survival 0/0", plain(render("Survival", label("x", "<task> <online>/<max>"), remote(0, 0, false))));
    }

    @DisplayName("A running source shows the text, not the offline text")
    @Test
    void runningSourceShowsText() {
        Assertions.assertEquals("1/2", plain(render("Survival", label("<online>/<max>", "offline"), remote(1, 2, true))));
    }

    @DisplayName("A server that comes up switches the rendering from the offline text to the text")
    @Test
    void comingUpSwitchesTheText() {
        PortalLabel label = label("<online>/<max>", "offline");

        Component before = render("Survival", label, remote(0, 0, false));
        Component after = render("Survival", label, remote(1, 20, true));

        Assertions.assertEquals("offline", plain(before));
        Assertions.assertEquals("1/20", plain(after));
    }

    @DisplayName("The local source shows the local number and '?' as the maximum")
    @Test
    void localHasUnknownMaximum() {
        Assertions.assertEquals("9/?", plain(render("Survival", label("<online>/<max>", "offline"), new LabelReading.Local(9))), "local is never offline");
    }

    @DisplayName("Tags in a task name stay literal text and never act as markup")
    @Test
    void tagsInTaskNameStayLiteral() {
        Component rendered = render("<red>Evil</red><click:run_command:'/op me'>", label("<task>", null), remote(1, 2, true));

        Assertions.assertEquals("<red>Evil</red><click:run_command:'/op me'>", plain(rendered), "the task name is text");
        Assertions.assertTrue(rendered.clickEvent() == null && rendered.children().stream().allMatch(child -> child.clickEvent() == null && child.color() == null), "no style or click event may come from the value");
    }
}
