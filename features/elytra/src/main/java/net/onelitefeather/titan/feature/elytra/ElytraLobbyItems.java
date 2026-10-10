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
package net.onelitefeather.titan.feature.elytra;

import io.avaje.config.Config;
import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import jakarta.inject.Named;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.EquipmentSlot;
import net.onelitefeather.titan.core.module.item.ItemSlot;
import net.onelitefeather.titan.core.module.item.LobbyItem;

/**
 * Contributes the {@code elytra} feature's two {@link LobbyItem}s to the platform-wide {@link
 * net.onelitefeather.titan.core.module.item.LobbyItems} as {@code @Bean}s. {@link
 * #firework(FireworkBoostTracker)} shares the same {@link FireworkBoostTracker} instance as
 * {@link ElytraModule}, so both advance and read the same per-player boost count.
 */
@Factory
final class ElytraLobbyItems {

    static final Key ELYTRA_KEY = Key.key("titan:elytra");

    static final Key FIREWORK_KEY = Key.key("titan:firework");

    private static final String FEATURE_ID = "elytra";

    @Bean
    @Named("elytra-chestplate")
    LobbyItem elytraChestplate() {
        return new LobbyItem(FEATURE_ID, ELYTRA_KEY, ElytraItems.ELYTRA, ItemSlot.equipment(EquipmentSlot.CHESTPLATE), (player, event) -> {
        });
    }

    @Bean
    @Named("elytra-firework")
    LobbyItem firework(FireworkBoostTracker boosts, ElytraTelemetry telemetry) {
        return new LobbyItem(FEATURE_ID, FIREWORK_KEY, ElytraItems.FIREWORK, ItemSlot.unplaced(), (player, event) -> {
            // Live, unvalidated read on every boost (see ElytraSettings' Javadoc): the strict,
            // cross-field check only ever runs once, in ElytraModule#start().
            int burnDurationTicks = Config.getInt(ElytraSettings.BURN_DURATION_TICKS_KEY);
            int cooldownTicks = Config.getInt(ElytraSettings.COOLDOWN_TICKS_KEY);
            if (boosts.requestBoost(player.getUuid(), burnDurationTicks, cooldownTicks, player.isFlyingWithElytra())) {
                telemetry.boosted();
                FireworkRockets.fire(player, burnDurationTicks, cooldownTicks);
            }
        });
    }
}
