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
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.onelitefeather.titan.core.portal.ServiceCount;

/** The clicks, reads and inspections the server list tests share; the picker is ElytraRace's. */
final class PickerTestSupport {

    static final int ELYTRA_RACE_SLOT = 12;

    private PickerTestSupport() {
    }

    /** {@code count} running ElytraRace services, named so that name order is creation order. */
    static ServiceCount[] services(int count) {
        return IntStream.range(0, count).mapToObj(i -> new ServiceCount("ElytraRace-%03d".formatted(i), 1, 50)).toArray(ServiceCount[]::new);
    }

    /**
     * Opens the navigator with the feather, then right clicks ElytraRace: the server list is open.
     */
    static AbstractInventory openPicker(Env env, NavigatorFixture fixture, Player player) {
        fixture.equip(player);
        fixture.useFeather(player);
        clickAndTick(env, player.getOpenInventory(), player, new Click.Right(ELYTRA_RACE_SLOT));
        return player.getOpenInventory();
    }

    /**
     * Passes one click to the listeners, then one tick, so a read that follows the click decides.
     */
    static void clickAndTick(Env env, AbstractInventory inventory, Player player, Click click) {
        env.process().eventHandler().call(new InventoryPreClickEvent(inventory, player, click));
        env.tick();
    }

    static String nameOf(AbstractInventory inventory, int slot) {
        ItemStack stack = inventory.getItemStack(slot);
        MiniMessage mini = MiniMessage.miniMessage();
        return mini.stripTags(mini.serialize(stack.get(DataComponents.CUSTOM_NAME)));
    }

    /** The lore of one slot as plain lines. */
    static List<String> loreOf(AbstractInventory inventory, int slot) {
        List<Component> lore = inventory.getItemStack(slot).get(DataComponents.LORE);
        return lore == null ? List.of() : lore.stream().map(line -> PlainTextComponentSerializer.plainText().serialize(line)).toList();
    }
}
