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

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntityAnimationPacket;
import net.minestom.server.network.player.ResolvableProfile;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The NPC that dances in the dancer's place: who sees it and who does not, what it looks like,
 * which steps reach it, and that the real player is left alone.
 */
@ExtendWith(MicrotusExtension.class)
class DanceNpcIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");
    private static final PlayerSkin SKIN = new PlayerSkin("skin-textures", "skin-signature");

    private static DanceModule module(Env env, TestTitanNode titan, PermissionService permissions) {
        return DanceFixtures.module(env, titan, permissions, new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
    }

    @DisplayName("Starting a dance spawns exactly one NPC")
    @Test
    void startingADanceSpawnsOneNpc(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));

                Assertions.assertEquals(1, DanceFixtures.npcs(instance), "exactly one NPC must exist while dancing");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The NPC is shown to every player in the instance, the dancer included")
    @Test
    void npcIsViewableByEveryone(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);
        Player other = env.createPlayer(instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));

                Assertions.assertEquals(Set.of(dancer.player(), other), DanceFixtures.npc(instance).getViewers(), "the NPC must be viewable by the dancer and everyone else");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The real dancer is hidden from the other players while the NPC dances for them")
    @Test
    void realDancerIsHiddenFromOthers(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);
        Player other = env.createPlayer(instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));

                Assertions.assertFalse(dancer.player().getViewers().contains(other), "the other player must not see the real dancer");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A player who joins during the dance does not see the real dancer, but sees the NPC")
    @Test
    void joinerDoesNotSeeTheRealDancer(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));
                Player joiner = env.createPlayer(instance);

                Assertions.assertFalse(dancer.player().getViewers().contains(joiner), "a joiner must not see the real dancer");
                Assertions.assertTrue(DanceFixtures.npc(instance).getViewers().contains(joiner), "a joiner must see the NPC");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The NPC stands where the dancer stands and faces the same way")
    @Test
    void npcStandsWhereTheDancerStands(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                Pos anchor = dancer.player().getPosition();
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));

                // The first step may already nod the head, so only the place and the body's yaw are compared.
                Pos npcAt = DanceFixtures.npc(instance).getPosition();
                Assertions.assertTrue(npcAt.samePoint(anchor), "the NPC must start at the dancer's position");
                Assertions.assertEquals(anchor.yaw(), npcAt.yaw(), "the NPC must face the way the dancer faces");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The NPC wears the dancer's skin")
    @Test
    void npcWearsTheDancersSkin(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);
        dancer.player().setSkin(SKIN);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));

                ResolvableProfile profile = ((MannequinMeta) DanceFixtures.npc(instance).getEntityMeta()).getProfile();
                Assertions.assertEquals(new ResolvableProfile(SKIN), profile, "the NPC must carry the dancer's skin");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Without a skin the NPC carries the dancer's name and id instead")
    @Test
    void npcWithoutSkinCarriesNameAndId(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));

                ResolvableProfile expected = new ResolvableProfile(new ResolvableProfile.Partial(dancer.player().getUsername(), dancer.player().getUuid(), List.of()));
                ResolvableProfile profile = ((MannequinMeta) DanceFixtures.npc(instance).getEntityMeta()).getProfile();
                Assertions.assertEquals(expected, profile, "without a skin the NPC must carry the dancer's name and id");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The NPC shows the dancer's name above it, so the others know who dances")
    @Test
    void npcShowsTheDancersNameAsNameTag(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));

                LivingEntity npc = DanceFixtures.npc(instance);
                Assertions.assertEquals(Component.text(dancer.player().getUsername()), npc.get(DataComponents.CUSTOM_NAME), "the NPC must carry the dancer's name");
                Assertions.assertTrue(npc.isCustomNameVisible(), "the name must be shown above the NPC");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The NPC cannot be pushed away from its spot")
    @Test
    void npcIsImmovable(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));

                Assertions.assertTrue(((MannequinMeta) DanceFixtures.npc(instance).getEntityMeta()).isImmovable(), "the NPC must be immovable");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The groove's sneak reaches the NPC")
    @Test
    void sneakReachesTheNpc(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.GROOVE));
                env.tick();

                Assertions.assertEquals(EntityPose.SNEAKING, DanceFixtures.npc(instance).getPose(), "the groove must make the NPC sneak");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("An arm swing of the dance is seen by the dancer and by the others")
    @Test
    void armSwingReachesTheDancerAndOthers(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);
        DanceFixtures.Dancer other = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));
                env.tick();

                int npcId = DanceFixtures.npc(instance).getEntityId();
                Assertions.assertTrue(swung(dancer, npcId), "the dancer must see the NPC swing its arm");
                Assertions.assertTrue(swung(other, npcId), "the other player must see the NPC swing its arm");
            } finally {
                module.stop();
            }
        }
    }

    private static boolean swung(DanceFixtures.Dancer watcher, int npcId) {
        return watcher.animations().collect().stream().anyMatch(packet -> packet.entityId() == npcId && packet.animation() == EntityAnimationPacket.Animation.SWING_MAIN_ARM);
    }

    @DisplayName("The spin reaches the NPC: its pose and the riptide flag the client renders")
    @Test
    void spinReachesTheNpc(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService().grant("titan.dance.spin"));
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.SPIN));
                env.tick();

                LivingEntity npc = DanceFixtures.npc(instance);
                Assertions.assertEquals(EntityPose.SPIN_ATTACK, npc.getPose(), "the spin must turn the NPC too");
                Assertions.assertTrue(((LivingEntityMeta) npc.getEntityMeta()).isInRiptideSpinAttack(), "the riptide flag must be set on the NPC");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The spin lifts the NPC off the floor for the spin and lets it land again afterwards")
    @Test
    void spinLiftsTheNpcAndLandsIt(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance, DanceFixtures.FLOOR);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService().grant("titan.dance.spin"));
            module.start();
            try {
                double floor = dancer.player().getPosition().y();
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.SPIN));
                LivingEntity npc = DanceFixtures.npc(instance);

                env.tick();   // tick 0: the spin starts and lifts the NPC
                Assertions.assertEquals(floor + MinestomDanceEffects.SPIN_LIFT, npc.getPosition().y(), 0.01, "the spin must lift the NPC by the spin lift");

                for (int tick = 0; tick < 30; tick++) {
                    env.tick();   // ticks 1 to 30: still hanging in the air
                }
                Assertions.assertTrue(npc.getPosition().y() > floor + 1.0, "the NPC must still be above the floor during the spin");

                for (int tick = 0; tick < 20; tick++) {
                    env.tick();   // ticks 31 to 50: the NPC lands at tick 40 and the dance still runs
                }
                Assertions.assertEquals(floor, npc.getPosition().y(), 0.05, "after the spin the NPC must be back on the floor");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A jump of the party lifts the NPC and lets it land again")
    @Test
    void jumpLiftsTheNpcAndItLands(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance, DanceFixtures.FLOOR);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                double floor = dancer.player().getPosition().y();
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.PARTY));
                LivingEntity npc = DanceFixtures.npc(instance);

                env.tick();   // tick 0: the first hop starts
                for (int tick = 0; tick < 3; tick++) {
                    env.tick();
                }
                Assertions.assertTrue(npc.getPosition().y() > floor + 0.01, "the hop must lift the NPC");

                for (int tick = 0; tick < 15; tick++) {
                    env.tick();   // ticks 4 to 18: the hop has come down before the next one at tick 20
                }
                Assertions.assertEquals(floor, npc.getPosition().y(), 0.05, "the NPC must land back on the floor");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The dance moves neither the pose nor the velocity of the real player")
    @Test
    void danceLeavesTheRealPlayerAlone(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance, DanceFixtures.FLOOR);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                Pos before = dancer.player().getPosition();
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.PARTY));
                // Gravity pulls the test player down on its own; the dance must add no hop and no sideways push.
                for (int tick = 0; tick < 25; tick++) {
                    env.tick();
                    Vec velocity = dancer.player().getVelocity();
                    Assertions.assertTrue(velocity.y() <= 0, "no upward velocity may be applied to the real player on tick " + tick);
                    Assertions.assertEquals(0, velocity.x(), 1e-9, "no sideways velocity may be applied to the real player");
                    Assertions.assertEquals(0, velocity.z(), 1e-9, "no sideways velocity may be applied to the real player");
                }

                Assertions.assertEquals(EntityPose.STANDING, dancer.player().getPose(), "the real player must keep his own pose");
                Assertions.assertEquals(before.x(), dancer.player().getPosition().x(), 1e-9, "the real player must not be moved sideways by the dance");
                Assertions.assertEquals(before.z(), dancer.player().getPosition().z(), 1e-9, "the real player must not be moved sideways by the dance");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The camera stands in front of the NPC and looks at it")
    @Test
    void cameraLooksAtTheNpcFromInFront(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));

                LivingEntity npc = DanceFixtures.npc(instance);
                Entity camera = DanceFixtures.cameraOf(instance, dancer);
                Assertions.assertNotNull(camera, "the dancer's view must be a camera entity in the instance");

                Pos cam = camera.getPosition();
                Pos target = npc.getPosition().add(0, dancer.player().getEyeHeight(), 0);
                Vec toNpc = new Vec(target.x() - cam.x(), target.y() - cam.y(), target.z() - cam.z()).normalize();
                double yaw = Math.toRadians(cam.yaw());
                double pitch = Math.toRadians(cam.pitch());
                Vec look = new Vec(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
                Assertions.assertTrue(look.dot(toNpc) > 0.999, "the camera must look at the NPC's eyes");

                double npcYaw = Math.toRadians(npc.getPosition().yaw());
                Vec facing = new Vec(-Math.sin(npcYaw), 0, Math.cos(npcYaw));
                Vec fromNpc = new Vec(cam.x() - npc.getPosition().x(), 0, cam.z() - npc.getPosition().z());
                Assertions.assertTrue(facing.dot(fromNpc) > 0, "the camera must stand on the side the NPC faces");
            } finally {
                module.stop();
            }
        }
    }
}
