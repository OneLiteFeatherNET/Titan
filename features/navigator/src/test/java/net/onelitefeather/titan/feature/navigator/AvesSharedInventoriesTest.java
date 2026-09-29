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

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.theevilreaper.aves.inventory.GlobalInventoryBuilder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Pins the assumption behind the navigator's two shared inventories: two registered Aves
 * {@link GlobalInventoryBuilder}s each route a click only to their own inventory's handler.
 */
@ExtendWith(MicrotusExtension.class)
class AvesSharedInventoriesTest {

    private static GlobalInventoryBuilder builderRecordingInto(String name, List<String> clicks) {
        GlobalInventoryBuilder builder = new GlobalInventoryBuilder(Component.text(name), InventoryType.CHEST_1_ROW);
        InventoryLayout layout = InventoryLayout.fromType(InventoryType.CHEST_1_ROW);
        layout.setItem(0, ItemStack.of(Material.STONE), (player, slot, click, stack, result) -> clicks.add(name));
        builder.setLayout(layout);
        builder.invalidateLayout();
        builder.register();
        return builder;
    }

    @DisplayName("Two registered global inventories route a click only to their own handler")
    @Test
    void twoRegisteredBuildersRouteClicksToTheirOwnInventory(Env env) {
        List<String> clicks = new ArrayList<>();
        GlobalInventoryBuilder first = builderRecordingInto("first", clicks);
        GlobalInventoryBuilder second = builderRecordingInto("second", clicks);
        try {
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);

            player.openInventory(first.getInventory());
            env.process().eventHandler().call(new InventoryPreClickEvent(player.getOpenInventory(), player, new Click.Left(0)));
            Assertions.assertEquals(List.of("first"), clicks, "a click in the first inventory must reach only the first handler");

            player.openInventory(second.getInventory());
            env.process().eventHandler().call(new InventoryPreClickEvent(player.getOpenInventory(), player, new Click.Left(0)));
            Assertions.assertEquals(List.of("first", "second"), clicks, "a click in the second inventory must reach only the second handler");
        } finally {
            first.unregister();
            second.unregister();
        }
    }
}
