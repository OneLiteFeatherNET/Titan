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

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.projectile.FireworkRocketMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.utils.time.TimeUnit;

/**
 * The entity half of the boost: the rocket a flying player sees when they use the firework.
 *
 * <p>Spawns a real firework entity with the player as shooter; the client ticks it locally and
 * applies the impulse to itself, so the server sends no velocity, and removes it after exactly
 * {@code burnDurationTicks} ticks instead of Vanilla's own randomized lifetime.
 */
final class FireworkRockets {

    // Matches Vanilla's own item id; this rocket carries no cooldown data component of its own.
    private static final String COOLDOWN_GROUP = "minecraft:firework_rocket";

    private FireworkRockets() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    static void fire(Player shooter, int burnDurationTicks, int cooldownTicks) {
        Instance instance = shooter.getInstance();
        if (instance == null) {
            return;
        }
        Entity rocket = new Entity(EntityType.FIREWORK_ROCKET);
        FireworkRocketMeta meta = (FireworkRocketMeta) rocket.getEntityMeta();
        meta.setFireworkInfo(ElytraItems.FIREWORK);
        meta.setShooter(shooter);
        // The client puts the rocket at whatever it is attached to on every one of its own ticks,
        // so the server neither moves it nor lets it fall.
        rocket.setNoGravity(true);
        rocket.setHasPhysics(false);
        rocket.setInstance(instance, shooter.getPosition());
        rocket.scheduleRemove(burnDurationTicks, TimeUnit.SERVER_TICK);

        shooter.sendPacket(new SetCooldownPacket(COOLDOWN_GROUP, cooldownTicks));
    }
}
