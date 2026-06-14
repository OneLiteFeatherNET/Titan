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
package net.onelitefeather.titan.app.listener;

import net.minestom.server.ServerFlag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.FireworkList;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.titan.common.config.AppConfig;
import net.onelitefeather.titan.common.utils.Items;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Recreates the vanilla elytra firework boost.
 *
 * <p>In vanilla, a used firework rocket attaches to the gliding player and, every tick for its
 * lifetime, nudges the player's velocity towards {@code look * 1.5}
 * (see {@code net.minecraft.world.entity.projectile.FireworkRocketEntity#tick}). Minestom does not
 * implement firework items, so this listener reproduces that behaviour: it runs a per-tick task on
 * the player's scheduler that applies the same formula for the same vanilla lifetime.
 *
 * <p>Velocity is computed in vanilla per-tick units and converted to Minestom's per-second velocity
 * on apply. {@link AppConfig#elytraBoostMultiplier()} scales the output ({@code 1.0} = vanilla).
 */
public final class ElytraBoostListener implements Consumer<PlayerUseItemEvent> {

    // Constants from FireworkRocketEntity#tick (per-tick deltaMovement units).
    private static final double LOOK_ACCELERATION = 0.1;
    private static final double LOOK_TARGET = 1.5;
    private static final double PULL_TOWARDS_TARGET = 0.5;

    private final AppConfig appConfig;
    private final Random random = new Random();
    private final Map<UUID, Boost> boosts = new ConcurrentHashMap<>();

    public ElytraBoostListener(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void accept(PlayerUseItemEvent event) {
        Player player = event.getPlayer();
        if (!player.isFlyingWithElytra() || !event.getItemStack().isSimilar(Items.PLAYER_FIREWORK)) {
            return;
        }

        // Vanilla: lifetime = 10 * flightCount + random(6) + random(7), flightCount = 1 + flight duration.
        int lifetime = 10 * flightCount(event.getItemStack()) + this.random.nextInt(6) + this.random.nextInt(7);

        Boost active = this.boosts.get(player.getUuid());
        if (active != null) {
            // Using a rocket during an active boost extends it, as stacking rockets does in vanilla.
            active.remainingTicks = Math.max(active.remainingTicks, lifetime);
            return;
        }

        Boost boost = new Boost(player.getVelocity().div(ServerFlag.SERVER_TICKS_PER_SECOND), lifetime);
        this.boosts.put(player.getUuid(), boost);
        player.scheduler().submitTask(() -> tick(player, boost));
    }

    private TaskSchedule tick(Player player, Boost boost) {
        if (!player.isOnline() || !player.isFlyingWithElytra() || boost.remainingTicks-- <= 0) {
            this.boosts.remove(player.getUuid());
            return TaskSchedule.stop();
        }

        Vec look = player.getPosition().direction();
        Vec velocity = boost.velocity;
        Vec next = velocity.add(
                look.x() * LOOK_ACCELERATION + (look.x() * LOOK_TARGET - velocity.x()) * PULL_TOWARDS_TARGET, look.y() * LOOK_ACCELERATION + (look.y() * LOOK_TARGET - velocity.y()) * PULL_TOWARDS_TARGET, look.z() * LOOK_ACCELERATION + (look.z() * LOOK_TARGET - velocity.z()) * PULL_TOWARDS_TARGET);
        boost.velocity = next;
        player.setVelocity(next.mul(ServerFlag.SERVER_TICKS_PER_SECOND * this.appConfig.elytraBoostMultiplier()));
        return TaskSchedule.tick(1);
    }

    private int flightCount(ItemStack itemStack) {
        FireworkList fireworks = itemStack.get(DataComponents.FIREWORKS);
        return fireworks != null ? 1 + fireworks.flightDuration() : 1;
    }

    private static final class Boost {

        private Vec velocity;
        private int remainingTicks;

        private Boost(Vec velocity, int remainingTicks) {
            this.velocity = velocity;
            this.remainingTicks = remainingTicks;
        }
    }
}
