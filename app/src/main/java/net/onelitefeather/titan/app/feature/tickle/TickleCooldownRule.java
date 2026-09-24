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
package net.onelitefeather.titan.app.feature.tickle;

/**
 * The pure decision behind {@link TickleAttackHandler}: given whether the attacking player already
 * carries the tickle cooldown tag, when that tag expires and the current time, decides what should
 * happen next.
 *
 * <p>Deliberately takes no {@link net.minestom.server.entity.Player} or {@link java.time.Clock} -
 * only plain {@code long} values - so it can be unit tested on its own, independent of Minestom and
 * wall-clock time.
 *
 * <p>{@link Decision#CLEAR_EXPIRED_TAG} documents today's known cooldown bug (see the class Javadoc
 * on {@link TickleAttackHandler}): the first hit after the cooldown has expired only clears the tag
 * instead of tickling again. Fixing this is out of scope for this change; see the follow-up change
 * {@code tickle-cooldown}.
 */
final class TickleCooldownRule {

    /** What {@link TickleAttackHandler} should do in reaction to an attack. */
    enum Decision {
        /** No cooldown tag is present: tickle and set a fresh cooldown. */
        TICKLE,
        /**
         * The cooldown tag is present but expired: today's code only clears it, it does not tickle.
         */
        CLEAR_EXPIRED_TAG,
        /** The cooldown tag is present and still valid: do nothing. */
        ON_COOLDOWN
    }

    private TickleCooldownRule() {
    }

    /**
     * Decides what should happen for an attack, given the attacker's current cooldown state.
     *
     * @param hasCooldownTag       whether the attacking player currently carries the cooldown tag
     * @param cooldownExpiryMillis the tag's value - the epoch millis timestamp the cooldown expires
     *                             at; ignored when {@code hasCooldownTag} is {@code false}
     * @param nowMillis            the current time, in epoch millis
     * @return what the handler should do
     */
    static Decision decide(boolean hasCooldownTag, long cooldownExpiryMillis, long nowMillis) {
        if (!hasCooldownTag) {
            return Decision.TICKLE;
        }
        if (nowMillis > cooldownExpiryMillis) {
            return Decision.CLEAR_EXPIRED_TAG;
        }
        return Decision.ON_COOLDOWN;
    }

    /**
     * Computes the epoch millis timestamp a freshly applied cooldown expires at.
     *
     * @param nowMillis      the current time, in epoch millis
     * @param cooldownMillis the configured cooldown duration, in milliseconds
     * @return {@code nowMillis + cooldownMillis}
     */
    static long expiryAfter(long nowMillis, long cooldownMillis) {
        return nowMillis + cooldownMillis;
    }
}
