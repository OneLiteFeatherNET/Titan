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
import java.util.Optional;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;

/**
 * The 22 vanilla discs in menu order. The lengths are wiki values in seconds, not measured against
 * the client files; {@code tears}, {@code lava_chicken} and {@code bounce} are placeholders.
 */
final class VanillaDiscs {

    static final List<VanillaDisc> ALL = List.of(
            disc(Material.MUSIC_DISC_13, SoundEvent.MUSIC_DISC_13, "13", "C418", 178), disc(Material.MUSIC_DISC_CAT, SoundEvent.MUSIC_DISC_CAT, "cat", "C418", 185), disc(Material.MUSIC_DISC_BLOCKS, SoundEvent.MUSIC_DISC_BLOCKS, "blocks", "C418", 345), disc(Material.MUSIC_DISC_CHIRP, SoundEvent.MUSIC_DISC_CHIRP, "chirp", "C418", 185), disc(Material.MUSIC_DISC_FAR, SoundEvent.MUSIC_DISC_FAR, "far", "C418", 174), disc(Material.MUSIC_DISC_MALL, SoundEvent.MUSIC_DISC_MALL, "mall", "C418", 197), disc(Material.MUSIC_DISC_MELLOHI, SoundEvent.MUSIC_DISC_MELLOHI, "mellohi", "C418", 96), disc(Material.MUSIC_DISC_STAL, SoundEvent.MUSIC_DISC_STAL, "stal", "C418", 150), disc(Material.MUSIC_DISC_STRAD, SoundEvent.MUSIC_DISC_STRAD, "strad", "C418", 188), disc(Material.MUSIC_DISC_WARD, SoundEvent.MUSIC_DISC_WARD, "ward", "C418", 251), disc(Material.MUSIC_DISC_11, SoundEvent.MUSIC_DISC_11, "11", "C418", 71), disc(Material.MUSIC_DISC_WAIT, SoundEvent.MUSIC_DISC_WAIT, "wait", "C418", 238), disc(Material.MUSIC_DISC_OTHERSIDE, SoundEvent.MUSIC_DISC_OTHERSIDE, "otherside", "Lena Raine", 195), disc(Material.MUSIC_DISC_5, SoundEvent.MUSIC_DISC_5, "5", "Samuel Åberg", 178), disc(Material.MUSIC_DISC_PIGSTEP, SoundEvent.MUSIC_DISC_PIGSTEP, "pigstep", "Lena Raine", 148), disc(Material.MUSIC_DISC_RELIC, SoundEvent.MUSIC_DISC_RELIC, "relic", "Lena Raine", 197), disc(Material.MUSIC_DISC_CREATOR, SoundEvent.MUSIC_DISC_CREATOR, "creator", "Lena Raine", 180), disc(Material.MUSIC_DISC_CREATOR_MUSIC_BOX, SoundEvent.MUSIC_DISC_CREATOR_MUSIC_BOX, "creator_music_box", "Lena Raine", 73), disc(Material.MUSIC_DISC_PRECIPICE, SoundEvent.MUSIC_DISC_PRECIPICE, "precipice", "Aaron Cherof", 359), disc(Material.MUSIC_DISC_TEARS, SoundEvent.MUSIC_DISC_TEARS, "tears", "Mojang", 180), disc(Material.MUSIC_DISC_LAVA_CHICKEN, SoundEvent.MUSIC_DISC_LAVA_CHICKEN, "lava_chicken", "Mojang", 180), disc(Material.MUSIC_DISC_BOUNCE, SoundEvent.MUSIC_DISC_BOUNCE, "bounce", "Mojang", 180));

    private VanillaDiscs() {
    }

    /** The vanilla entry of {@code material}, if it is a music disc of the table. */
    static Optional<VanillaDisc> ofMaterial(Material material) {
        return ALL.stream().filter(disc -> disc.disc() == material).findFirst();
    }

    private static VanillaDisc disc(Material disc, SoundEvent sound, String title, String artist, int seconds) {
        return new VanillaDisc(disc, sound, title, artist, seconds * RadioLimits.TICKS_PER_SECOND);
    }
}
