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
package net.onelitefeather.titan.app.feature.elytra;

import java.util.Random;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.app.module.ModuleContext;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;

/**
 * Moves today's elytra flight and firework boost - {@code ElytraStartFlyingListener}, {@code
 * ElytraStopFlyingListener} and {@code ElytraBoostListener} on {@code main} - into a
 * {@link LobbyModule} (see {@code openspec/changes/lobby-feature-modules}, task 6.7).
 *
 * <p>Registers two {@link LobbyItem}s with the platform's item registry:
 * <ul>
 * <li>{@code titan:elytra} - the elytra itself, at the fixed
 * {@link ItemSlot#equipment(EquipmentSlot)} placement
 * {@link EquipmentSlot#CHESTPLATE}. Its use handler is a no-op; the spawn and respawn
 * modules put it on through {@code items().equip(player)}, nothing needs to happen when a
 * player merely uses it.</li>
 * <li>{@code titan:firework} - the boost rocket, registered with no fixed place
 * ({@link ItemSlot#unplaced()}) because it only ever lives in the offhand while a player is
 * gliding. This module hands the registry-stamped stack into the offhand itself on
 * {@link PlayerStartFlyingWithElytraEvent} and takes it back on
 * {@link PlayerStopFlyingWithElytraEvent}, exactly as the {@code lobby-hotbar} spec
 * requires for "Items mit wechselndem Platz". Using it only boosts while the player is
 * actually flying with the elytra - see {@link FireworkBoostTracker#useFirework}.</li>
 * </ul>
 *
 * <p>Per-player boost state lives in a {@link FireworkBoostTracker}, cleared on stop-flying and
 * on {@link PlayerDisconnectEvent} so it never leaks a player who can no longer be boosted.
 */
public final class ElytraModule implements LobbyModule {

    @Override
    public String id() {
        return "elytra";
    }

    @Override
    public void enable(ModuleContext context) {
        ElytraConfig config = context.config(ElytraConfig.class, ElytraConfig.DEFAULTS);
        FireworkBoostTracker boosts = new FireworkBoostTracker(new Random(), config.boostMultiplier());

        context.items().register(new LobbyItem(Key.key("titan:elytra"), ElytraItems.ELYTRA, ItemSlot.equipment(EquipmentSlot.CHESTPLATE), (player, event) -> {
        }));
        ItemStack stampedFirework = context.items().register(new LobbyItem(Key.key("titan:firework"), ElytraItems.FIREWORK, ItemSlot.unplaced(), (player, event) -> boosts.useFirework(player, event.getItemStack())));

        context.listen(PlayerStartFlyingWithElytraEvent.class, event -> event.getPlayer().setItemInOffHand(stampedFirework));
        context.listen(PlayerStopFlyingWithElytraEvent.class, event -> {
            event.getPlayer().setItemInOffHand(ItemStack.AIR);
            boosts.clear(event.getPlayer().getUuid());
        });
        context.listen(PlayerDisconnectEvent.class, event -> boosts.clear(event.getPlayer().getUuid()));
    }
}
