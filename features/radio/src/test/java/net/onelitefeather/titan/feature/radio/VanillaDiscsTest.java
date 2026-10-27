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

import java.util.HashSet;
import java.util.Set;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VanillaDiscsTest {

    private static final String DISC_PREFIX = "music_disc_";
    private static final int MAX_LENGTH_TICKS = 15 * 60 * 20;

    @DisplayName("The table holds exactly the music disc items of this Minestom version, one per material")
    @Test
    void tableCoversEveryMusicDiscMaterial() {
        Set<Material> expected = new HashSet<>();
        for (Material material : Material.values()) {
            if (material.key().value().startsWith(DISC_PREFIX)) {
                expected.add(material);
            }
        }
        Set<Material> listed = new HashSet<>();
        VanillaDiscs.ALL.forEach(disc -> listed.add(disc.disc()));

        Assertions.assertEquals(expected, listed, "every Material.MUSIC_DISC_* must have exactly one vanilla disc entry");
        Assertions.assertEquals(22, VanillaDiscs.ALL.size(), "this Minestom version knows 22 music discs");
    }

    @DisplayName("Each disc names no other sound than its own client record sound")
    @Test
    void eachDiscPlaysItsOwnRecordSound() {
        for (VanillaDisc disc : VanillaDiscs.ALL) {
            String expectedSound = "music_disc." + disc.disc().key().value().substring(DISC_PREFIX.length());

            Assertions.assertEquals(expectedSound, disc.sound().key().value(), "the sound of " + disc.disc() + " must be its client record sound");
        }
    }

    @DisplayName("No disc appears twice in the table")
    @Test
    void noDiscIsListedTwice() {
        Set<Material> distinct = new HashSet<>();
        VanillaDiscs.ALL.forEach(disc -> distinct.add(disc.disc()));

        Assertions.assertEquals(VanillaDiscs.ALL.size(), distinct.size(), "every disc must be listed once");
    }

    @DisplayName("A vanilla disc longer than fifteen minutes is accepted, as there is no length limit")
    @Test
    void discLongerThanFifteenMinutesIsAccepted() {
        int twentyMinutes = 20 * 60 * RadioLimits.TICKS_PER_SECOND;

        Assertions.assertDoesNotThrow(() -> new VanillaDisc(Material.MUSIC_DISC_13, SoundEvent.MUSIC_DISC_13, "13", "C418", twentyMinutes), "a length above fifteen minutes must not be rejected");
    }

    @DisplayName("A vanilla disc with no length is rejected")
    @Test
    void discWithoutLengthIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new VanillaDisc(Material.MUSIC_DISC_13, SoundEvent.MUSIC_DISC_13, "13", "C418", 0), "a length of zero ticks must be rejected");
    }

    @DisplayName("Every length lies between zero and fifteen minutes")
    @Test
    void everyLengthIsPositiveAndShort() {
        for (VanillaDisc disc : VanillaDiscs.ALL) {
            Assertions.assertTrue(disc.lengthTicks() > 0 && disc.lengthTicks() < MAX_LENGTH_TICKS, disc.disc() + " must last more than zero and less than fifteen minutes");
        }
    }
}
