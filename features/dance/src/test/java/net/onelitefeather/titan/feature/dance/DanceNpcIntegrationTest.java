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
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.PlayerSkin;
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
 * The NPC that dances in the dancer's place while the camera shows him the dance: who sees it, what
 * it looks like, which steps reach it, and where the camera stands.
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

    @DisplayName("The NPC is shown to the dancer only, not to another player in the instance")
    @Test
    void npcIsViewableOnlyByTheDancer(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);
        // Another player in the same instance, who must not see the NPC.
        env.createPlayer(instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));

                Assertions.assertEquals(Set.of(dancer.player()), DanceFixtures.npc(instance).getViewers(), "the NPC must be viewable by the dancer alone");
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

                Assertions.assertEquals(EntityPose.SNEAKING, DanceFixtures.npc(instance).getPose(), "the groove must make the NPC sneak too");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("An arm swing of the dance reaches the dancer's view of the NPC")
    @Test
    void armSwingReachesTheNpc(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = module(env, titan, new FakePermissionService());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));
                env.tick();

                int npcId = DanceFixtures.npc(instance).getEntityId();
                boolean swung = dancer.animations().collect().stream().anyMatch(packet -> packet.entityId() == npcId && packet.animation() == EntityAnimationPacket.Animation.SWING_MAIN_ARM);
                Assertions.assertTrue(swung, "the dancer must see the NPC swing its arm");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The spin reaches the NPC")
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

                Assertions.assertEquals(EntityPose.SPIN_ATTACK, DanceFixtures.npc(instance).getPose(), "the spin must turn the NPC too");
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
