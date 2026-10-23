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
package net.onelitefeather.titan.feature.portal;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.titan.core.portal.Disc;
import net.onelitefeather.titan.core.portal.Portal;

/**
 * The sounds of walking or flying through a portal. Each one is heard by the player alone, since
 * it answers that player's own action; the other players nearby hear nothing.
 *
 * <p>A box hands over with a short teleport whoosh, a disc (ring) with a bright chime, so one pass
 * gives exactly one sound. A refused entry gets a low bass note. Every sound is vanilla.
 */
final class PortalSounds {

    static final SoundEvent BOX_PASS = SoundEvent.ENTITY_ENDERMAN_TELEPORT;
    static final float BOX_PASS_VOLUME = 0.5f;
    static final float BOX_PASS_PITCH = 1.0f;

    static final SoundEvent RING_PASS = SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP;
    static final float RING_PASS_VOLUME = 0.6f;
    static final float RING_PASS_PITCH = 1.3f;

    static final SoundEvent REFUSED = SoundEvent.BLOCK_NOTE_BLOCK_BASS;
    static final float REFUSED_VOLUME = 0.8f;
    static final float REFUSED_PITCH = 0.7f;

    private PortalSounds() {
    }

    /** The sound of a hand-over: the ring chime for a disc, the teleport whoosh for a box. */
    static void passed(Player player, Portal portal) {
        if (portal.shape() instanceof Disc) {
            hear(player, RING_PASS, RING_PASS_VOLUME, RING_PASS_PITCH);
        } else {
            hear(player, BOX_PASS, BOX_PASS_VOLUME, BOX_PASS_PITCH);
        }
    }

    /** The sound of an entry the player has no permission for. */
    static void refused(Player player) {
        hear(player, REFUSED, REFUSED_VOLUME, REFUSED_PITCH);
    }

    private static void hear(Player player, SoundEvent event, float volume, float pitch) {
        // Bound to the player, not a position: the player moves on at once, and the sound follows.
        player.playSound(Sound.sound(event, Sound.Source.PLAYER, volume, pitch), Sound.Emitter.self());
    }
}
