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
package net.onelitefeather.titan.feature.throwing;

import java.util.Optional;

/**
 * Decides whether a pickup may happen. Pure: the module reads the facts (permission, settings,
 * vehicle, flight) and passes them in, so each rule is tested on its own.
 */
final class ThrowRules {

    private ThrowRules() {
    }

    /** Why a pickup or a throw was refused; {@link #reason()} is the telemetry attribute. */
    enum Refusal {
        PERMISSION("permission"), OPT_OUT("opt_out"), IMMUNE("immune"), COOLDOWN("cooldown"), STATE("state");

        private final String reason;

        Refusal(String reason) {
            this.reason = reason;
        }

        String reason() {
            return this.reason;
        }
    }

    /**
     * What the rules need to know about one of the two players.
     *
     * @param busy     sitting, dancing, in a jump-and-run or flying with an elytra
     * @param carried  riding on someone else's head
     * @param carrying carrying someone right now
     * @param inFlight thrown and not yet landed
     * @param immune   holds {@code titan.throw.immune}; only meaningful for the target
     * @param optedOut switched the target option off; only meaningful for the target
     */
    record Party(boolean busy, boolean carried, boolean carrying, boolean inFlight, boolean immune,
                 boolean optedOut) {

        boolean occupied() {
            return this.busy || this.carried || this.carrying || this.inFlight;
        }
    }

    /**
     * @param throwPermitted whether the thrower holds {@code titan.throw}
     * @return the first refusal that applies, or empty if the pickup may happen
     */
    static Optional<Refusal> pickup(boolean throwPermitted, Party thrower, Party target) {
        if (!throwPermitted) {
            return Optional.of(Refusal.PERMISSION);
        }
        if (target.immune()) {
            return Optional.of(Refusal.IMMUNE);
        }
        if (target.optedOut()) {
            return Optional.of(Refusal.OPT_OUT);
        }
        if (thrower.occupied() || target.occupied()) {
            return Optional.of(Refusal.STATE);
        }
        return Optional.empty();
    }
}
