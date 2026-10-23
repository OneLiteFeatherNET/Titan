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

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;

/**
 * Performs dance actions on a real player with vanilla packets only: poses, arm swings, head
 * rotation, a small hop, particles and note block sounds.
 */
final class MinestomDanceEffects implements DanceEffects {

    /** Head pitch while nodding down and up, in degrees; the yaw stays where the dance started. */
    private static final float HEAD_DOWN_PITCH = 30f;
    private static final float HEAD_UP_PITCH = -10f;

    /** A small hop: a few tenths of a block high, not a jump across the lobby. */
    private static final double HOP_VELOCITY = 6.0;

    private static final String NOTE_SOUND = "block.note_block.harp";
    private static final float[] NOTE_PITCHES = {0.5f, 0.56f, 0.63f, 0.67f, 0.75f, 0.84f, 0.94f, 1.0f, 1.12f, 1.26f, 1.33f, 1.5f, 1.68f, 1.78f, 2.0f};

    /** Height of the particles above the feet, in blocks. */
    private static final double PARTICLE_HEIGHT = 2.0;

    private final Player dancer;
    private int notesPlayed;

    MinestomDanceEffects(Player dancer) {
        this.dancer = dancer;
    }

    @Override
    public void apply(DanceAction action, Pos anchor) {
        switch (action) {
            case SWING_MAIN_HAND -> this.dancer.swingMainHand();
            case SWING_OFF_HAND -> this.dancer.swingOffHand();
            case HEAD_DOWN -> this.dancer.setView(anchor.yaw(), HEAD_DOWN_PITCH);
            case HEAD_UP -> this.dancer.setView(anchor.yaw(), HEAD_UP_PITCH);
            case SNEAK -> this.dancer.setSneaking(true);
            case STAND -> stand();
            case SPIN -> riptideSpin(true);
            case JUMP -> this.dancer.setVelocity(new Vec(0, HOP_VELOCITY, 0));
            case NOTE -> note();
            case HEART -> particle(Particle.HEART);
        }
    }

    @Override
    public void reset(Pos anchor) {
        stand();
        // The flags take effect on the next tick; the pose is set now, so the dancer is standing at once.
        this.dancer.setPose(EntityPose.STANDING);
        this.dancer.setView(anchor.yaw(), anchor.pitch());
    }

    /**
     * Minestom derives the pose from the sneak and riptide flags on every tick, so a pose set
     * directly is overwritten at once; the flags are what keep the pose.
     */
    private void stand() {
        this.dancer.setSneaking(false);
        riptideSpin(false);
    }

    private void riptideSpin(boolean spinning) {
        if (this.dancer.getEntityMeta() instanceof LivingEntityMeta living) {
            living.setInRiptideSpinAttack(spinning);
        }
    }

    private void note() {
        particle(Particle.NOTE);
        Instance instance = this.dancer.getInstance();
        if (instance == null) {
            return;
        }
        float pitch = NOTE_PITCHES[this.notesPlayed % NOTE_PITCHES.length];
        this.notesPlayed++;
        Sound sound = Sound.sound(Key.key(NOTE_SOUND), Sound.Source.RECORD, 1f, pitch);
        instance.playSoundExcept(null, sound, this.dancer.getPosition());
    }

    private void particle(Particle particle) {
        Instance instance = this.dancer.getInstance();
        if (instance == null) {
            return;
        }
        Pos at = this.dancer.getPosition();
        instance.sendGroupedPacket(new ParticlePacket(particle, at.x(), at.y() + PARTICLE_HEIGHT, at.z(), 0.3f, 0.3f, 0.3f, 1f, 2));
    }
}
