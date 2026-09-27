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

import io.avaje.config.Config;
import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import jakarta.inject.Named;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.EquipmentSlot;
import net.onelitefeather.titan.app.module.item.ItemSlot;
import net.onelitefeather.titan.app.module.item.LobbyItem;

/**
 * Contributes the {@code elytra} feature's two {@link LobbyItem}s to the platform-wide {@link
 * net.onelitefeather.titan.app.module.item.LobbyItems} as {@code @Bean}s, replacing what {@link
 * ElytraModule} used to register directly through {@code context.items()} (see {@code
 * openspec/changes/dissolve-module-platform/design.md}, decision 2).
 *
 * <p>{@link #firework(FireworkBoostTracker)} shares the same {@link FireworkBoostTracker} instance
 * as {@link ElytraModule} - both ask for it through their constructor/method parameter, so the
 * count a boost is requested against here is the very same one {@link ElytraModule} advances once
 * per tick and clears on stop-flying or disconnect. Reads
 * {@link ElytraSettings#BURN_DURATION_TICKS_KEY}
 * and {@link ElytraSettings#COOLDOWN_TICKS_KEY} live and unvalidated on every boost, via {@code
 * io.avaje.config.Config.getInt} - see {@link ElytraSettings}'s own Javadoc for why: the
 * cross-field check only ever runs once, at {@link ElytraModule#start()}.
 *
 * <p>Package-private, like the rest of this feature's internals: Avaje Inject's generated wiring
 * lives in the same package as the class it annotates, so it reaches these {@code @Bean} methods
 * without either needing to be public.
 */
@Factory
final class ElytraLobbyItems {

    /**
     * The identity of the {@code titan:elytra} chestplate item; also read by {@link ElytraModule}.
     */
    static final Key ELYTRA_KEY = Key.key("titan:elytra");

    /** The identity of the {@code titan:firework} item; also read by {@link ElytraModule}. */
    static final Key FIREWORK_KEY = Key.key("titan:firework");

    private static final String FEATURE_ID = "elytra";

    /**
     * @return the unbreakable elytra, fixed to {@link EquipmentSlot#CHESTPLATE}; using it does
     *         nothing on its own - the spawn and respawn features put it on through
     *         {@code LobbyItems#equip(Player)}
     */
    @Bean
    @Named("elytra-chestplate")
    LobbyItem elytraChestplate() {
        return new LobbyItem(FEATURE_ID, ELYTRA_KEY, ElytraItems.ELYTRA, ItemSlot.equipment(EquipmentSlot.CHESTPLATE), (player, event) -> {
        });
    }

    /**
     * @param boosts the tracker shared with {@link ElytraModule}, which advances it once per tick
     *               and clears a player's entry on stop-flying or disconnect
     * @return the boost rocket, with no fixed place; {@link ElytraModule} hands the
     *         {@code LobbyItems}-stamped stack into a gliding player's offhand itself
     */
    @Bean
    @Named("elytra-firework")
    LobbyItem firework(FireworkBoostTracker boosts) {
        return new LobbyItem(FEATURE_ID, FIREWORK_KEY, ElytraItems.FIREWORK, ItemSlot.unplaced(), (player, event) -> {
            // Live, unvalidated read on every boost (see ElytraSettings' Javadoc): the strict,
            // cross-field check only ever runs once, in ElytraModule#start().
            int burnDurationTicks = Config.getInt(ElytraSettings.BURN_DURATION_TICKS_KEY);
            int cooldownTicks = Config.getInt(ElytraSettings.COOLDOWN_TICKS_KEY);
            if (boosts.requestBoost(player.getUuid(), burnDurationTicks, cooldownTicks, player.isFlyingWithElytra())) {
                FireworkRockets.fire(player, burnDurationTicks, cooldownTicks);
            }
        });
    }
}
