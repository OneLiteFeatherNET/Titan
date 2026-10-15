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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Building an item needs Minestom's registries, which is why these tests take an {@code Env}. */
@ExtendWith(MicrotusExtension.class)
class DestinationClickHintLoreTest {

    private static final List<Component> HINT = List.of(Component.text("Left click: connect"));

    private static List<String> lore(ItemStack item) {
        return item.get(DataComponents.LORE).stream().map(line -> PlainTextComponentSerializer.plainText().serialize(line)).toList();
    }

    @DisplayName("A click hint comes after the players line")
    @Test
    void hintFollowsThePlayersLine(Env env) {
        Assertions.assertEquals(List.of("3 online", "Left click: connect"), lore(Destination.SURVIVAL.item(OptionalInt.of(3), HINT)));
    }

    @DisplayName("The left and the right click hint each take their own line, after the players line")
    @Test
    void leftAndRightHintsOnSeparateLines(Env env) {
        List<Component> hints = List.of(Component.text("Left click: connect"), Component.text("Right click: walk to portal"));
        Assertions.assertEquals(List.of("3 online", "Left click: connect", "Right click: walk to portal"), lore(Destination.SURVIVAL.item(OptionalInt.of(3), hints)));
    }

    @DisplayName("Without a known count the click hint is the only lore line")
    @Test
    void hintAloneWhenCountIsUnknown(Env env) {
        Assertions.assertEquals(List.of("Left click: connect"), lore(Destination.SURVIVAL.item(OptionalInt.empty(), HINT)));
    }
}
