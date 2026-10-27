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
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.minestom.server.entity.Player;

/**
 * Sends the radio's sounds to one player. The emitter is the player, so the sound is not placed in
 * the world and no other player receives a packet.
 */
final class PlayerRadioSink implements RadioSink {

    private final Player player;

    PlayerRadioSink(Player player) {
        this.player = Objects.requireNonNull(player, "player must not be null");
    }

    @Override
    public void note(Sound sound) {
        this.player.playSound(sound, Sound.Emitter.self());
    }

    @Override
    public void playRecord(Sound sound) {
        this.player.playSound(sound, Sound.Emitter.self());
    }

    @Override
    public void stopRecord(Key sound) {
        this.player.stopSound(SoundStop.named(sound));
    }
}
