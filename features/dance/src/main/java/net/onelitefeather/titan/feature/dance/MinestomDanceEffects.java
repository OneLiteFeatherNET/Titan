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

import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;

/**
 * Performs dance actions on the dancer and on his NPC with vanilla packets only: poses, arm swings,
 * head rotation, a small hop, particles and note block sounds. Other players see the real player
 * dance; the dancer sees the NPC dance from the camera.
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
    private final List<LivingEntity> bodies;
    private int notesPlayed;

    /**
     * @param dancer the real player
     * @param npc    the mannequin that stands in for him in the dancer's view
     */
    MinestomDanceEffects(Player dancer, LivingEntity npc) {
        this.dancer = dancer;
        this.bodies = List.of(dancer, npc);
    }

    @Override
    public void apply(DanceAction action, Pos anchor) {
        switch (action) {
            case SWING_MAIN_HAND -> each(body -> body.swingMainHand());
            case SWING_OFF_HAND -> each(body -> body.swingOffHand());
            case HEAD_DOWN -> each(body -> body.setView(anchor.yaw(), HEAD_DOWN_PITCH));
            case HEAD_UP -> each(body -> body.setView(anchor.yaw(), HEAD_UP_PITCH));
            case SNEAK -> each(body -> body.setSneaking(true));
            case STAND -> each(MinestomDanceEffects::stand);
            case SPIN -> each(MinestomDanceEffects::spin);
            // The NPC keeps gravity, so the hop comes back down to the floor.
            case JUMP -> each(body -> body.setVelocity(new Vec(0, HOP_VELOCITY, 0)));
            case NOTE -> note();
            case HEART -> particle(Particle.HEART);
        }
    }

    @Override
    public void reset(Pos anchor) {
        each(body -> {
            stand(body);
            // Set directly as well: the NPC has no tick that would derive the pose from the flags.
            body.setPose(EntityPose.STANDING);
            body.setView(anchor.yaw(), anchor.pitch());
        });
    }

    /**
     * Sneaking re-derives the pose from the flags, so the riptide flag goes first and the pose
     * drops
     * to standing with the sneak flag.
     */
    private static void stand(LivingEntity body) {
        setRiptide(body, false);
        body.setSneaking(false);
    }

    private static void spin(LivingEntity body) {
        setRiptide(body, true);
        // A player's tick derives the pose from the flag; the NPC has no such tick, so set it here.
        body.setPose(EntityPose.SPIN_ATTACK);
    }

    private static void setRiptide(LivingEntity body, boolean spinning) {
        if (body.getEntityMeta() instanceof LivingEntityMeta living) {
            living.setInRiptideSpinAttack(spinning);
        }
    }

    private void each(Consumer<LivingEntity> step) {
        for (LivingEntity body : this.bodies) {
            step.accept(body);
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
