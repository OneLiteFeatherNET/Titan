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
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;

/**
 * Performs dance actions on the NPC, the only body the others see dance. The real player is never
 * moved, posed or animated: his client is not applying input while his camera is detached, and the
 * server would overwrite a pose of his on every tick anyway. Every step uses vanilla packets and
 * entity metadata only.
 */
final class MinestomDanceEffects implements DanceEffects {

    /** Head pitch while nodding down and up, in degrees; the yaw stays where the dance started. */
    private static final float HEAD_DOWN_PITCH = 30f;
    private static final float HEAD_UP_PITCH = -10f;

    /** A small hop: a few tenths of a block high, not a jump across the lobby. */
    private static final double HOP_VELOCITY = 6.0;

    /**
     * How high the riptide spin lifts the NPC above the floor, in blocks: the height a vanilla
     * riptide jump reaches. The NPC hangs there until the spin ends, then falls back with gravity.
     */
    static final double SPIN_LIFT = 1.25;

    private static final String NOTE_SOUND = "block.note_block.harp";
    private static final float[] NOTE_PITCHES = {0.5f, 0.56f, 0.63f, 0.67f, 0.75f, 0.84f, 0.94f, 1.0f, 1.12f, 1.26f, 1.33f, 1.5f, 1.68f, 1.78f, 2.0f};

    /** Height of the particles above the feet, in blocks. */
    private static final double PARTICLE_HEIGHT = 2.0;

    private final LivingEntity npc;
    private int notesPlayed;

    /** @param npc the mannequin that dances in the dancer's place */
    MinestomDanceEffects(LivingEntity npc) {
        this.npc = npc;
    }

    @Override
    public void apply(DanceAction action, Pos anchor) {
        switch (action) {
            case SWING_MAIN_HAND -> this.npc.swingMainHand();
            case SWING_OFF_HAND -> this.npc.swingOffHand();
            case HEAD_DOWN -> this.npc.setView(anchor.yaw(), HEAD_DOWN_PITCH);
            case HEAD_UP -> this.npc.setView(anchor.yaw(), HEAD_UP_PITCH);
            case SNEAK -> this.npc.setSneaking(true);
            case STAND -> stand(this.npc);
            case SPIN -> spin(this.npc, anchor);
            case SPIN_LAND -> land(this.npc);
            case JUMP -> this.npc.setVelocity(new Vec(0, HOP_VELOCITY, 0));
            case NOTE -> note();
            case HEART -> particle(Particle.HEART);
        }
    }

    @Override
    public void reset(Pos anchor) {
        this.npc.setNoGravity(false);
        stand(this.npc);
        // Set directly as well: the NPC has no tick that would derive the pose from the flags.
        this.npc.setPose(EntityPose.STANDING);
        this.npc.setView(anchor.yaw(), anchor.pitch());
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

    /**
     * The riptide flag makes the client spin the model; the pose is set as well, because the NPC
     * has
     * no tick that would derive it. The lift holds the NPC in the air, so it does not sink into the
     * floor while it spins.
     */
    private static void spin(LivingEntity body, Pos anchor) {
        setRiptide(body, true);
        body.setPose(EntityPose.SPIN_ATTACK);
        body.setNoGravity(true);
        body.teleport(anchor.add(0, SPIN_LIFT, 0));
    }

    /** The spin is over: the NPC stops spinning and gravity brings it back to the floor. */
    private static void land(LivingEntity body) {
        body.setNoGravity(false);
        stand(body);
    }

    private static void setRiptide(LivingEntity body, boolean spinning) {
        if (body.getEntityMeta() instanceof LivingEntityMeta living) {
            living.setInRiptideSpinAttack(spinning);
        }
    }

    private void note() {
        particle(Particle.NOTE);
        Instance instance = this.npc.getInstance();
        if (instance == null) {
            return;
        }
        float pitch = NOTE_PITCHES[this.notesPlayed % NOTE_PITCHES.length];
        this.notesPlayed++;
        Sound sound = Sound.sound(Key.key(NOTE_SOUND), Sound.Source.RECORD, 1f, pitch);
        instance.playSoundExcept(null, sound, this.npc.getPosition());
    }

    private void particle(Particle particle) {
        Instance instance = this.npc.getInstance();
        if (instance == null) {
            return;
        }
        Pos at = this.npc.getPosition();
        instance.sendGroupedPacket(new ParticlePacket(particle, at.x(), at.y() + PARTICLE_HEIGHT, at.z(), 0.3f, 0.3f, 0.3f, 1f, 2));
    }
}
