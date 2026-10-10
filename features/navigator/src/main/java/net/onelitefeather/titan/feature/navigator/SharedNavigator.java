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
import java.util.function.Function;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.theevilreaper.aves.inventory.GlobalInventoryBuilder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;

/**
 * One inventory shared by every player who gets this menu, plus the layout state that belongs to
 * it. Each instance rebuilds independently, so the public and the team menu never see each
 * other's layout.
 */
final class SharedNavigator {

    private final GlobalInventoryBuilder builder;
    private final boolean withPermissioned;
    private final Telemetry telemetry;
    private List<Destination> appliedVisible;

    SharedNavigator(boolean withPermissioned, Telemetry telemetry) {
        this.builder = new GlobalInventoryBuilder(MiniMessage.miniMessage().deserialize("<yellow>Navigator"), InventoryType.CHEST_1_ROW);
        this.withPermissioned = withPermissioned;
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

    // Synchronized so two threads opening the navigator at once can't observe, or trigger, half of
    // a rebuild.
    synchronized void applyLayoutIfChanged(FeatureFlags featureFlags, Function<List<Destination>, InventoryLayout> layoutFor) {
        List<Destination> visible = Destination.visible(featureFlags, this.withPermissioned);
        if (visible.equals(this.appliedVisible)) {
            return;
        }
        this.telemetry.inSpan(NavigatorTelemetry.LAYOUT_SPAN, Attributes.empty(), () -> {
            this.builder.setLayout(layoutFor.apply(visible));
            this.builder.invalidateLayout();
            this.appliedVisible = visible;
        });
    }
}
