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

/**
 * The pure decision behind {@link TickleAttackHandler}: given whether the attacking player already
 * carries the tickle cooldown tag, when that tag expires and the current time, decides what should
 * happen next.
 *
 * <p>Deliberately takes no {@link net.minestom.server.entity.Player} or {@link java.time.Clock} -
 * only plain {@code long} values - so it can be unit tested independent of Minestom and wall-clock
 * time.
 */
final class TickleCooldownRule {

    /** What {@link TickleAttackHandler} should do in reaction to an attack. */
    enum Decision {
        /** No cooldown tag is present: tickle and set a fresh cooldown. */
        TICKLE,
        /**
         * The cooldown tag is present but expired: today's code only clears it, it does not tickle.
         */
        CLEAR_EXPIRED_TAG, ON_COOLDOWN
    }

    private TickleCooldownRule() {
    }

    static Decision decide(boolean hasCooldownTag, long cooldownExpiryMillis, long nowMillis) {
        if (!hasCooldownTag) {
            return Decision.TICKLE;
        }
        if (nowMillis > cooldownExpiryMillis) {
            return Decision.CLEAR_EXPIRED_TAG;
        }
        return Decision.ON_COOLDOWN;
    }

    static long expiryAfter(long nowMillis, long cooldownMillis) {
        return nowMillis + cooldownMillis;
    }
}
