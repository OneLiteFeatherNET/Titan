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

import java.util.List;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RadioMaterialsTest {

    @DisplayName("Every vanilla item the radio menu uses exists in this Minestom version")
    @Test
    void menuMaterialsExist() {
        List<Material> used = List.of(Material.OAK_DOOR, Material.SPECTRAL_ARROW, Material.BOOK, Material.BARRIER, Material.REPEATER, Material.NOTE_BLOCK, Material.ARROW, Material.PAPER, Material.MUSIC_DISC_13, Material.GRAY_STAINED_GLASS_PANE, Material.JUKEBOX, Material.EMERALD, Material.FIREWORK_ROCKET, Material.CLOCK, Material.JACK_O_LANTERN);

        Assertions.assertEquals(15, used.size(), "the list must name all fifteen materials of the change");
        used.forEach(material -> Assertions.assertNotNull(Material.fromKey(material.key()), material.key() + " must be a Minestom material"));
    }
}
