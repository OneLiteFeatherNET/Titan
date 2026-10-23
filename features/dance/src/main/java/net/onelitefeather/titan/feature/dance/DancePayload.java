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
package net.onelitefeather.titan.feature.dance;

import java.util.Optional;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.jetbrains.annotations.Nullable;

/**
 * Reads the dance a client asks for from the payload of a {@code titan:dance} click. The client is
 * never trusted: anything that is not a compound with a known string id yields nothing.
 */
final class DancePayload {

    static final String ID_FIELD = "id";

    private DancePayload() {
    }

    /** @return the id as a payload for a dance button */
    static CompoundBinaryTag of(Dance dance) {
        return CompoundBinaryTag.builder().putString(ID_FIELD, dance.id()).build();
    }

    /**
     * @return the dance named by {@code payload}, or empty if the payload is missing, malformed or
     *         unknown
     */
    static Optional<Dance> dance(@Nullable BinaryTag payload) {
        if (!(payload instanceof CompoundBinaryTag compound) || !(compound.get(ID_FIELD) instanceof StringBinaryTag id)) {
            return Optional.empty();
        }
        return Dance.byId(id.value());
    }
}
