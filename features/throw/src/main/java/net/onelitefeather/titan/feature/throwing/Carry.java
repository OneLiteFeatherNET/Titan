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

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import net.minestom.server.item.ItemStack;

/**
 * One running trage: who carries whom, the thrower's hand as it was before the carry, and whether
 * a charge runs. The snapshot is what the hand gets back on every way the carry ends.
 *
 * @param slot     the hotbar slot the charge item lives in
 * @param snapshot the content of that slot before the carry; never stored as a charge item
 */
record Carry(UUID thrower, UUID target, int slot, ItemStack snapshot, Instant pickedAt,
             boolean charging) {

    Carry {
        Objects.requireNonNull(thrower, "thrower must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(pickedAt, "pickedAt must not be null");
    }

    Carry withCharging(boolean charging) {
        return new Carry(this.thrower, this.target, this.slot, this.snapshot, this.pickedAt, charging);
    }

    /** @return whether the carry has lasted {@code maxAge} or longer by {@code now} */
    boolean expired(Instant now, Duration maxAge) {
        return !now.isBefore(this.pickedAt.plus(maxAge));
    }
}
