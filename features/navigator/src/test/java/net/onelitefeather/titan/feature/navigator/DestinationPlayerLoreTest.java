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
import java.util.OptionalInt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Building an item needs Minestom's registries, which is why the item tests take an {@code Env}.
 */
@ExtendWith(MicrotusExtension.class)
class DestinationPlayerLoreTest {

    private static final List<Component> HINT = List.of(Component.text("Left click: connect"));

    private static List<String> lore(ItemStack item) {
        return item.get(DataComponents.LORE).stream().map(line -> PlainTextComponentSerializer.plainText().serialize(line)).toList();
    }

    private static boolean hasGreenPart(Component line) {
        return line.children().stream().anyMatch(child -> NamedTextColor.GREEN.equals(child.color()));
    }

    @DisplayName("A known count adds a lore line with the number, before the hint")
    @Test
    void knownCountAddsLine(Env env) {
        Assertions.assertEquals(List.of("3 online", "Left click: connect"), lore(Destination.SURVIVAL.item(OptionalInt.of(3), HINT)), "the players line comes first, then the click hint");
    }

    @DisplayName("A positive count is green")
    @Test
    void positiveCountIsGreen() {
        Assertions.assertTrue(hasGreenPart(Destination.playersLine(3)));
    }

    @DisplayName("Zero online still shows a line, without green")
    @Test
    void zeroCountAddsPlainLine(Env env) {
        Assertions.assertEquals(List.of("0 online", "Left click: connect"), lore(Destination.SURVIVAL.item(OptionalInt.of(0), HINT)), "zero players is a known count, so it still gets a line");
        Assertions.assertFalse(hasGreenPart(Destination.playersLine(0)), "zero players is not highlighted in green");
    }

    @DisplayName("An unknown count adds no players line, only the hint")
    @Test
    void unknownCountAddsNoLine(Env env) {
        Assertions.assertEquals(List.of("Left click: connect"), lore(Destination.SURVIVAL.item(OptionalInt.empty(), HINT)), "an unknown count must not read as an empty lobby");
    }
}
