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
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.portal.PlayerCount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Building an item needs Minestom's registries, which is why the item tests take an {@code Env}.
 */
@ExtendWith(MicrotusExtension.class)
class DestinationPlayerLoreTest {

    private static List<String> lore(ItemStack item) {
        return item.get(DataComponents.LORE).stream().map(line -> PlainTextComponentSerializer.plainText().serialize(line)).toList();
    }

    private static boolean isSlash(Component child) {
        return "/".equals(PlainTextComponentSerializer.plainText().serialize(child));
    }

    @DisplayName("A known count with a known max reads current/max")
    @Test
    void knownCountAddsCurrentOfMax(Env env) {
        Assertions.assertEquals(List.of("12/100"), lore(Destination.SURVIVAL.item(Optional.of(new PlayerCount(12, 100, true)))));
    }

    @DisplayName("A count without a known max shows only the current number")
    @Test
    void unknownMaxShowsCurrentOnly(Env env) {
        Assertions.assertEquals(List.of("12"), lore(Destination.SURVIVAL.item(Optional.of(new PlayerCount(12, 0, true)))));
    }

    @DisplayName("Zero players still shows a line, as 0/100")
    @Test
    void zeroCountAddsLine(Env env) {
        Assertions.assertEquals(List.of("0/100"), lore(Destination.SURVIVAL.item(Optional.of(new PlayerCount(0, 100, true)))));
    }

    @DisplayName("The slash between the two numbers is gray")
    @Test
    void slashIsGray() {
        Component line = Destination.playersLine(new PlayerCount(12, 100, true));

        Assertions.assertTrue(line.children().stream().anyMatch(child -> isSlash(child) && NamedTextColor.GRAY.equals(child.color())));
    }

    @DisplayName("An unknown count adds no lore line")
    @Test
    void unknownCountAddsNoLine(Env env) {
        Assertions.assertTrue(lore(Destination.SURVIVAL.item(Optional.empty())).isEmpty());
        Assertions.assertTrue(lore(Destination.SURVIVAL.item()).isEmpty());
    }
}
