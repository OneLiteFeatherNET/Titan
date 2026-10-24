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
package net.onelitefeather.titan.feature.tag;

import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntityAnimationPacket;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;

/**
 * What a player sees and hears of the tag game. Only vanilla means: the glow, red particles, the
 * action bar, a title, a sound and the hurt animation. No resource pack and no scoreboard team.
 */
final class TagEffects {

    static final SoundEvent SOUND = SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP;
    private static final float SOUND_VOLUME = 0.8f;
    private static final float SOUND_PITCH = 1.0f;
    private static final Key DUST_KEY = Key.key("minecraft", "dust");
    private static final TextColor RED = TextColor.color(0xFF2020);
    private static final float PARTICLE_SCALE = 1.2f;
    private static final int PARTICLE_COUNT = 6;
    private static final Vec SPREAD = new Vec(0.3, 0.5, 0.3);

    /**
     * Shows the new state after the tagger changed from {@code previous}: the glow moves, and the
     * tagger's action bar is refreshed while there is one, which keeps it from fading out. When
     * the tagger leaves and one player is left alone, that player sees the waiting hint once.
     */
    void taggerChanged(Instance instance, TagGame game, UUID previous) {
        UUID current = game.tagger().orElse(null);
        if (previous != null && !previous.equals(current)) {
            playerIn(instance, previous).ifPresent(player -> player.setGlowing(false));
        }
        if (current != null) {
            playerIn(instance, current).ifPresent(player -> player.setGlowing(true));
            actionBars(instance, game);
        } else if (previous != null) {
            for (Player player : instance.getPlayers()) {
                if (game.isActive(player.getUuid())) {
                    waiting(player);
                }
            }
        }
    }

    /**
     * The hint that a player is alone and waits for a second one. Sent once, when the player
     * becomes alone, and not refreshed: the action bar fades out by itself after a few seconds.
     */
    void waiting(Player player) {
        player.sendActionBar(TagMessages.text(TagMessages.WAITING, player.getLocale()));
    }

    /** The player left the game: no glow stays behind. */
    void leftGame(Player player) {
        player.setGlowing(false);
    }

    /**
     * A hit passed the role: a sound for both, the title for the new tagger, and the hurt flash.
     */
    void passed(Player attacker, Player target) {
        attacker.playSound(Sound.sound(SOUND, Sound.Source.PLAYER, SOUND_VOLUME, SOUND_PITCH), attacker.getPosition());
        target.playSound(Sound.sound(SOUND, Sound.Source.PLAYER, SOUND_VOLUME, SOUND_PITCH), target.getPosition());
        target.showTitle(Title.title(TagMessages.text(TagMessages.TITLE, target.getLocale()), TagMessages.text(TagMessages.SUBTITLE, target.getLocale())));
        target.sendPacketToViewersAndSelf(new EntityAnimationPacket(target.getEntityId(), EntityAnimationPacket.Animation.TAKE_DAMAGE));
    }

    /** A hit within the cooldown: a hint in the action bar only, no sound. */
    void cooldown(Player attacker) {
        attacker.sendActionBar(TagMessages.text(TagMessages.COOLDOWN, attacker.getLocale()));
    }

    /** Red particles above the tagger, for the players of the tagger's instance. */
    void particles(Instance instance, TagGame game) {
        Optional<UUID> tagger = game.tagger();
        if (tagger.isEmpty()) {
            return;
        }
        Particle.Dust dust = ((Particle.Dust) Particle.fromKey(DUST_KEY)).withProperties(RED, PARTICLE_SCALE);
        playerIn(instance, tagger.get()).ifPresent(player -> instance.sendGroupedPacket(new ParticlePacket(dust, player.getPosition().add(0, 1, 0), SPREAD, 0f, PARTICLE_COUNT)));
    }

    /**
     * Every active player of the instance sees who the tagger is. Does nothing without a tagger.
     */
    void actionBars(Instance instance, TagGame game) {
        Optional<UUID> tagger = game.tagger();
        if (tagger.isEmpty()) {
            return;
        }
        String name = playerIn(instance, tagger.get()).map(Player::getUsername).orElse("");
        for (Player player : instance.getPlayers()) {
            if (game.isActive(player.getUuid())) {
                player.sendActionBar(TagMessages.text(TagMessages.TAGGER, player.getLocale()).append(Component.text(" " + name)));
            }
        }
    }

    /** The player with {@code id} if they are in {@code instance}; empty otherwise. */
    static Optional<Player> playerIn(Instance instance, UUID id) {
        return instance.getPlayers().stream().filter(player -> player.getUuid().equals(id)).findFirst();
    }
}
