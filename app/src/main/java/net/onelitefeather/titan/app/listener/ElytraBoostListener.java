/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.app.listener;

import net.minestom.server.event.player.PlayerUseItemEvent;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.utils.Items;

import java.util.function.Consumer;

public final class ElytraBoostListener implements Consumer<PlayerUseItemEvent> {

    private final AppConfig appConfig;

    public ElytraBoostListener(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void accept(PlayerUseItemEvent event) {
        var itemStack = event.getItemStack();
        if (itemStack.isSimilar(Items.PLAYER_FIREWORK) && event.getPlayer().isFlyingWithElytra()) {
            event.getPlayer().setVelocity(event.getPlayer().getVelocity().normalize().add(event.getPlayer().getPosition().direction().mul(this.appConfig.elytraBoostMultiplier())));
        }
    }
}
