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
package net.onelitefeather.titan.feature.radio;

import java.util.Objects;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;

/**
 * A vanilla music disc. The client plays its own record sound; the server only knows the length
 * from the table, which is an approximation until it is measured against the client files.
 *
 * @param disc        the disc item of the menu
 * @param sound       the client's record sound of that disc
 * @param title       the disc's title, as shown in the menu
 * @param artist      the disc's composer, as shown in the lore
 * @param lengthTicks the approximate length in server ticks
 */
record VanillaDisc(Material disc, SoundEvent sound, String title, String artist,
                   int lengthTicks) implements RadioSong {

    VanillaDisc {
        Objects.requireNonNull(disc, "disc must not be null");
        Objects.requireNonNull(sound, "sound must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(artist, "artist must not be null");
        if (lengthTicks <= 0) {
            throw new IllegalArgumentException("lengthTicks must be positive, was " + lengthTicks);
        }
    }

    @Override
    public RadioKind kind() {
        return RadioKind.VANILLA;
    }

    @Override
    public RadioCategory category() {
        return RadioCategories.MINECRAFT;
    }
}
