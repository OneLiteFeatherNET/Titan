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
package net.onelitefeather.titan.feature.tag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * The rules of one lobby's tag game. Knows nothing of Minestom: the column reports joins, leaves,
 * hits and activity with the current time in millis, and reads back who holds the role.
 *
 * <p>With two or more active players there is always exactly one tagger; below two there is none.
 */
final class TagGame {

    /** After a hand-over the new tagger may not hit the player who tagged them back for this long. */
    static final long HIT_COOLDOWN_MILLIS = 5_000L;

    /** A tagger who neither moves, hits nor sends input for this long loses the role. */
    static final long INACTIVITY_MILLIS = 120_000L;

    private final RandomGenerator random;
    private final TagCooldown cooldown = new TagCooldown();
    /** Join order, so a seeded random source picks the same player on every run. */
    private final Set<UUID> active = new LinkedHashSet<>();
    private UUID tagger;
    private long lastActivity;

    TagGame(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    /** @return {@code true} if the player was not active before */
    boolean join(UUID player, long now) {
        if (!this.active.add(player)) {
            return false;
        }
        if (this.tagger == null && this.active.size() >= 2) {
            assignTagger(now, null);
        }
        return true;
    }

    /** @return {@code true} if the player was active before */
    boolean leave(UUID player, long now) {
        if (!this.active.remove(player)) {
            return false;
        }
        this.cooldown.forget(player);
        if (this.active.size() < 2) {
            this.tagger = null;
        } else if (player.equals(this.tagger)) {
            assignTagger(now, null);
        }
        return true;
    }

    /**
     * A hit between two active players. Only the tagger's hit on another active player can pass
     * the role; the reverse hit within the cooldown is refused.
     */
    TagOutcome hit(UUID attacker, UUID target, long now) {
        if (!this.active.contains(attacker) || !this.active.contains(target) || attacker.equals(target) || !attacker.equals(this.tagger)) {
            return TagOutcome.IGNORED;
        }
        this.lastActivity = now;
        if (this.cooldown.blocks(attacker, target, now)) {
            return TagOutcome.COOLDOWN;
        }
        this.cooldown.passed(attacker, target, now);
        this.tagger = target;
        this.lastActivity = now;
        return TagOutcome.PASSED;
    }

    /** Records that {@code player} moved or sent input; only the tagger's activity is kept. */
    void touch(UUID player, long now) {
        if (player.equals(this.tagger)) {
            this.lastActivity = now;
        }
    }

    /** Hands the role to another active player if the tagger has been idle for too long. */
    void tick(long now) {
        if (this.tagger == null || this.active.size() < 2) {
            return;
        }
        if (now - this.lastActivity >= INACTIVITY_MILLIS) {
            assignTagger(now, this.tagger);
        }
    }

    Optional<UUID> tagger() {
        return Optional.ofNullable(this.tagger);
    }

    boolean isActive(UUID player) {
        return this.active.contains(player);
    }

    /** @return a copy of the active players in join order */
    List<UUID> active() {
        return List.copyOf(this.active);
    }

    /** Picks a new tagger among the active players, never {@code excluded}. */
    private void assignTagger(long now, UUID excluded) {
        List<UUID> candidates = new ArrayList<>(this.active);
        candidates.remove(excluded);
        this.tagger = candidates.get(this.random.nextInt(candidates.size()));
        this.lastActivity = now;
    }
}
