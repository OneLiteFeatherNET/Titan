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

import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.utils.Items;

import java.util.Random;
import java.util.function.Consumer;

public final class ElytraBoostListener implements Consumer<PlayerUseItemEvent> {

    private final AppConfig appConfig;
    private final Random random = new Random();

    public ElytraBoostListener(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void accept(PlayerUseItemEvent event) {
        var itemStack = event.getItemStack();
        if (itemStack.isSimilar(Items.PLAYER_FIREWORK) && event.getPlayer().isFlyingWithElytra()) {
            // In vanilla Minecraft, firework power affects boost strength
            // Since we don't have power levels in our firework, we'll use the config multiplier
            // as a base and implement the vanilla-like boost logic

            // Get player's current direction and velocity
            Vec playerDirection = event.getPlayer().getPosition().direction();
            Vec playerVelocity = event.getPlayer().getVelocity();

            // Calculate the boost vector based on vanilla Minecraft mechanics
            // 1. Add a boost in the player's looking direction
            double baseBoost = this.appConfig.elytraBoostMultiplier();
            Vec boostVector = playerDirection.mul(baseBoost);

            // 2. Add a small random component for natural movement (vanilla-like)
            double randomX = (random.nextDouble() - 0.5) * 0.05;
            double randomY = (random.nextDouble() - 0.5) * 0.05;
            double randomZ = (random.nextDouble() - 0.5) * 0.05;
            Vec randomComponent = new Vec(randomX, randomY, randomZ);

            // 3. Apply the boost
            // In vanilla, if player is looking down, boost has more upward component
            if (playerDirection.y() < 0) {
                // Add a slight upward boost when looking down to prevent crashing
                double upwardCorrection = -playerDirection.y() * 0.3;
                boostVector = boostVector.add(0, upwardCorrection, 0);
            }

            // Combine all components and apply to player
            Vec finalVelocity = playerVelocity.add(boostVector).add(randomComponent);

            // Apply the velocity
            event.getPlayer().setVelocity(finalVelocity);
        }
    }
}
