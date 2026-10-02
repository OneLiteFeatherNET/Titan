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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.event.EventNode;
import net.minestom.server.item.Material;
import net.minestom.testing.RegistriesTest;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;
import org.junit.jupiter.api.Test;

@RegistriesTest
class JumprunItemsTest {

    private final LobbyItem item = new JumprunItems().jumprun(new JumprunModule(EventNode.all("unused"), () -> null, List::of, new InMemoryRunRecords(), new RunMessages(), () -> 1L));

    @Test
    void theItemHasTheJumprunKeyAndBelongsToTheFeature() {
        assertEquals("titan:jumprun", item.key().asString());
        assertEquals("jumprun", item.featureId());
    }

    @Test
    void theItemSitsInTheFirstHotbarSlot() {
        assertEquals(ItemSlot.hotbar(0), item.placement());
    }

    @Test
    void theNameIsTheSameInEveryLanguage() {
        Component name = item.itemStack().get(DataComponents.CUSTOM_NAME);

        assertEquals("Jump & Run", PlainTextComponentSerializer.plainText().serialize(name));
    }

    @Test
    void theItemIsASlimeBlock() {
        assertEquals(Material.SLIME_BLOCK, item.itemStack().material());
    }

    @Test
    void theNameIsBoldAndNotItalic() {
        Component name = item.itemStack().get(DataComponents.CUSTOM_NAME);

        assertEquals(TextDecoration.State.FALSE, name.decoration(TextDecoration.ITALIC), "custom names are italic unless switched off");
        assertTrue(name.children().stream().anyMatch(child -> child.decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE), "the title is bold");
    }

    @Test
    void theItemCarriesNoDescription() {
        List<Component> lore = item.itemStack().get(DataComponents.LORE);

        assertTrue(lore == null || lore.isEmpty(), "a lobby item is shared by all players, so it has no translated lore");
    }
}
