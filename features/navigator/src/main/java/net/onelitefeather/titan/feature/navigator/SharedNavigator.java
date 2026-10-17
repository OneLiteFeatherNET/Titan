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

import io.opentelemetry.api.common.Attributes;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.theevilreaper.aves.inventory.GlobalInventoryBuilder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;

/**
 * One inventory shared by every player who gets this menu, plus the layout state that belongs to
 * it. Each instance lays out independently, so no two menus see each other's layout.
 */
final class SharedNavigator {

    private final GlobalInventoryBuilder builder;
    private final Set<Destination> granted;
    private final Telemetry telemetry;
    private List<Destination> appliedVisible;

    SharedNavigator(Set<Destination> granted, Telemetry telemetry) {
        this.builder = new GlobalInventoryBuilder(MiniMessage.miniMessage().deserialize("<yellow>Navigator"), InventoryType.CHEST_1_ROW);
        this.granted = granted;
        this.telemetry = telemetry;
    }

    void register() {
        this.builder.register();
    }

    void unregister() {
        this.builder.unregister();
    }

    Inventory inventory() {
        return this.builder.getInventory();
    }

    // The visible destinations depend only on the grants this menu was built for, so the layout is
    // applied once. Synchronized so a refresh never reads the layout half-built.
    synchronized void applyLayout(Function<List<Destination>, InventoryLayout> layoutFor) {
        List<Destination> visible = Destination.visible(this.granted);
        this.telemetry.inSpan(NavigatorTelemetry.LAYOUT_SPAN, Attributes.empty(), () -> {
            this.builder.setLayout(layoutFor.apply(visible));
            this.builder.invalidateLayout();
            this.appliedVisible = visible;
        });
    }

    // Updates the stacks in place, so a refresh never rebuilds the layout or touches the click handlers.
    synchronized void showItems(Function<Destination, ItemStack> itemFor) {
        if (this.appliedVisible == null) {
            return;
        }
        for (Destination destination : this.appliedVisible) {
            this.builder.getInventory().setItemStack(destination.slot(), itemFor.apply(destination));
        }
    }
}
