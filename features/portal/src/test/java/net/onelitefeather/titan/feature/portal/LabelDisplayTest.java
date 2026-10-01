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

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LabelDisplayTest {

    private final List<Component> sent = new ArrayList<>();
    private int removed;
    private final LabelDisplay display = new LabelDisplay(this.sent::add, () -> this.removed++);

    @DisplayName("The first text is always sent")
    @Test
    void firstTextIsSent() {
        Assertions.assertTrue(this.display.update(Component.text("12/40")), "nothing was sent before");

        Assertions.assertEquals(List.of(Component.text("12/40")), this.sent);
    }

    @DisplayName("The same rendered text sends nothing again")
    @Test
    void sameTextSendsNothing() {
        this.display.update(Component.text("12/40", NamedTextColor.GRAY));

        boolean sent = this.display.update(Component.text("12/40", NamedTextColor.GRAY));

        Assertions.assertFalse(sent, "an equal component must not be sent");
        Assertions.assertEquals(1, this.sent.size(), "exactly the first update reached the client");
    }

    @DisplayName("A changed text is sent exactly once")
    @Test
    void changedTextIsSentOnce() {
        this.display.update(Component.text("12/40"));

        this.display.update(Component.text("13/40"));
        this.display.update(Component.text("13/40"));

        Assertions.assertEquals(List.of(Component.text("12/40"), Component.text("13/40")), this.sent, "one send per distinct text");
    }

    @DisplayName("Equal words in another colour count as a change")
    @Test
    void formattingCountsAsChange() {
        this.display.update(Component.text("a", NamedTextColor.RED));

        Assertions.assertTrue(this.display.update(Component.text("a", NamedTextColor.GREEN)), "component equality includes the style");
    }

    @DisplayName("Remove despawns the entity")
    @Test
    void removeDespawns() {
        this.display.remove();

        Assertions.assertEquals(1, this.removed);
    }
}
