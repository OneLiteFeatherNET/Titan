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
package net.onelitefeather.titan.app.feature.protection;

import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.common.utils.Cancelable;

/**
 * Protects the lobby from being modified by a player: nobody may pick up, drop, or swap items,
 * click inside an inventory, or break or place a block.
 *
 * <p>This module has no state and no configuration - it simply cancels every
 * {@link PickupItemEvent}, {@link InventoryPreClickEvent}, {@link PlayerBlockBreakEvent},
 * {@link PlayerBlockPlaceEvent}, {@link PlayerSwapItemEvent} and {@link ItemDropEvent}
 * unconditionally, mirroring what {@code Titan#initListeners()} wired directly onto the shared
 * event node before this module existed.
 *
 * <p>Cancelling an event does not stop it from reaching listeners registered elsewhere: Minestom
 * keeps walking the rest of the listener chain regardless of {@link
 * net.minestom.server.event.trait.CancellableEvent#isCancelled()}. What a cancellation does affect
 * is any single {@code Consumer}-based listener (the kind {@link ModuleContext#listen} registers)
 * for a cancellable event type - such a listener checks {@code isCancelled()} right before running
 * and skips its own body if the event is already cancelled by the time it is invoked. In practice
 * that means another module wanting to react to {@link InventoryPreClickEvent} regardless of this
 * module's cancellation (the navigator module's own menu, for instance) must be registered so its
 * listener runs before this module's - e.g. by being enabled earlier - not after.
 */
public final class ProtectionModule implements LobbyModule {

    @Override
    public String id() {
        return "protection";
    }

    @Override
    public void enable(ModuleContext context) {
        context.listen(PickupItemEvent.class, Cancelable::cancel);
        context.listen(InventoryPreClickEvent.class, Cancelable::cancel);
        context.listen(PlayerBlockBreakEvent.class, Cancelable::cancel);
        context.listen(PlayerBlockPlaceEvent.class, Cancelable::cancel);
        context.listen(PlayerSwapItemEvent.class, Cancelable::cancel);
        context.listen(ItemDropEvent.class, Cancelable::cancel);
    }
}
