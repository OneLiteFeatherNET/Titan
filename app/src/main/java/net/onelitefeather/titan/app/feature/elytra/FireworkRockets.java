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
 * Ported from Voyager ({@code net.elytrarace.voyager.server.game.Rockets}): the entity half of the
 * boost - the rocket a flying player sees when they use the firework.
 *
 * <h2>Why a real entity, and not a server-applied velocity</h2>
 *
 * <p>The lobby's previous elytra boost computed the impulse itself and pushed it with
 * {@code Player#setVelocity}. This does what Vanilla does instead: it spawns a real firework
 * rocket entity with the player as its shooter ({@link FireworkRocketMeta#setShooter}, which
 * writes the shooter entity id into the metadata), and the client - which ticks that entity
 * locally and, seeing itself as the attachment, applies the impulse to its own player - boosts
 * itself. The server applies no velocity at all.
 *
 * <h2>Why the server removes the rocket</h2>
 *
 * <p>Vanilla's rocket picks its own lifetime with two dice rolls. Two identical boosts should be
 * worth the same in the lobby, so the entity is removed after exactly
 * {@link ElytraConfig#burnDurationTicks()} ticks and nothing random is left in it. The removal is
 * scheduled in {@link TimeUnit#SERVER_TICK}, a tick count rather than a wall-clock delay a slow
 * tick would stretch.
 *
 * <p>Called only after {@link FireworkBoostTracker#requestBoost} has agreed to the boost - this
 * does not itself check the cooldown or the gliding flag, so there is only one place that decides
 * whether a boost happens.
 */
final class FireworkRockets {

    /**
     * The cooldown group the client greys the firework stack out under, matching Vanilla's own
     * {@code minecraft:firework_rocket} item id - the rocket handed out here carries no
     * {@code use_cooldown} data component of its own.
     */
    private static final String COOLDOWN_GROUP = "minecraft:firework_rocket";

    private FireworkRockets() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Spawns one rocket attached to {@code shooter} and tells their client how long the next one
     * is, via {@link SetCooldownPacket}.
     *
     * @param shooter the flying player boosting; must be in an instance
     * @param config  the tuning this burn runs under
     */
    static void fire(Player shooter, ElytraConfig config) {
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
        rocket.scheduleRemove(config.burnDurationTicks(), TimeUnit.SERVER_TICK);

        shooter.sendPacket(new SetCooldownPacket(COOLDOWN_GROUP, config.cooldownTicks()));
    }
}
