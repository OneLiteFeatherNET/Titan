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
package net.onelitefeather.titan.feature.tickle;

import java.util.Objects;
import java.util.random.RandomGenerator;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.sound.SoundEvent;

/**
 * The sound of a tickle: a short, bright allay chirp at the tickled player, heard by everyone
 * within {@link #REACH_BLOCKS}. The pitch varies per tickle so repeated tickles do not sound
 * identical; the randomness is injected so tests can fix it.
 */
final class TickleSounds {

    static final SoundEvent SOUND = SoundEvent.ENTITY_ALLAY_ITEM_GIVEN;
    static final float VOLUME = 0.6f;
    static final float MIN_PITCH = 0.9f;
    static final float MAX_PITCH = 1.2f;

    /** The audible range of a volume 1.0 sound in vanilla; players further away get no packet. */
    static final double REACH_BLOCKS = 16;

    private final TicklePairLimiter limiter;
    private final RandomGenerator random;

    TickleSounds(TicklePairLimiter limiter, RandomGenerator random) {
        this.limiter = Objects.requireNonNull(limiter, "limiter");
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * Plays the tickle sound for {@code tickler} and {@code target}, unless the pair is rate
     * limited.
     */
    void tickle(Player tickler, Player target) {
        if (!this.limiter.tryAcquire(tickler.getUuid(), target.getUuid())) {
            return;
        }
        Instance instance = target.getInstance();
        if (instance == null) {
            return;
        }
        Point at = target.getPosition();
        Sound sound = Sound.sound(SOUND, Sound.Source.PLAYER, VOLUME, pitch());
        for (Entity entity : instance.getNearbyEntities(at, REACH_BLOCKS)) {
            if (entity instanceof Player listener) {
                listener.playSound(sound, at);
            }
        }
    }

    float pitch() {
        return MIN_PITCH + this.random.nextFloat() * (MAX_PITCH - MIN_PITCH);
    }
}
