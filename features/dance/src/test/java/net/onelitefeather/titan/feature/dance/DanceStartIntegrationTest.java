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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Starting a dance: the camera, the pose, one dance at a time, the pause, and the permission gate.
 */
@ExtendWith(MicrotusExtension.class)
class DanceStartIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");

    @DisplayName("Starting a dance switches the dancer to a marker entity that stands in front of him")
    @Test
    void startingADanceSetsTheCamera(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));

                Assertions.assertEquals(1, DanceFixtures.markers(instance), "exactly one camera marker must exist while dancing");
                Assertions.assertNotEquals(dancer.player().getEntityId(), dancer.cameras().collect().getFirst().cameraId(), "the camera must be the marker, not the dancer");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Groove puts the dancer into the sneaking pose on the first tick")
    @Test
    void grooveSneaksOnTheFirstTick(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.GROOVE));
                env.tick();

                Assertions.assertEquals(EntityPose.SNEAKING, dancer.player().getPose(), "groove starts by sneaking");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A second dance while one runs is refused, and the camera stays on the first")
    @Test
    void secondDanceWhileDancingIsRefused(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.PARTY));

                Assertions.assertEquals(1, DanceFixtures.markers(instance), "a second dance must not add a second camera");
                Assertions.assertEquals(1, dancer.cameras().collect().size(), "a second dance must not switch the camera again");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A new start within the pause after a dance is refused")
    @Test
    void startWithinThePauseIsRefused(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);
        AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), clock, Telemetry.noop());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));
                env.process().eventHandler().call(DanceFixtures.stopClick(dancer.player()));
                clock.advance(Duration.ofSeconds(2));
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));

                Assertions.assertEquals(0, DanceFixtures.markers(instance), "a start inside the pause must not create a camera");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A start after the pause is accepted again")
    @Test
    void startAfterThePauseIsAccepted(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);
        AdjustableClock clock = new AdjustableClock(NOW, ZoneOffset.UTC);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), clock, Telemetry.noop());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));
                env.process().eventHandler().call(DanceFixtures.stopClick(dancer.player()));
                clock.advance(Duration.ofSeconds(3));
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.WAVE));

                Assertions.assertEquals(1, DanceFixtures.markers(instance), "a start after the pause must create the camera again");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Spin without the permission is refused and leaves the dancer untouched")
    @Test
    void gatedDanceIsRefusedWithoutPermission(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);
        Pos before = dancer.player().getPosition();

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.SPIN));
                env.tick();

                Assertions.assertEquals(0, DanceFixtures.markers(instance), "a refused dance must not create a camera");
                Assertions.assertEquals(EntityPose.STANDING, dancer.player().getPose(), "a refused dance must not change the pose");
                Assertions.assertEquals(before, dancer.player().getPosition(), "a refused dance must not move the player");
                Assertions.assertTrue(dancer.cameras().collect().isEmpty(), "a refused dance must send no camera packet");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Spin with the permission starts")
    @Test
    void spinWithPermissionStarts(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService().grant("titan.dance.spin"), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.SPIN));
                env.tick();

                Assertions.assertEquals(EntityPose.SPIN_ATTACK, dancer.player().getPose(), "spin with the permission turns into the spin pose");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A forged payload with an unknown id does nothing")
    @Test
    void unknownIdDoesNothing(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), new AdjustableClock(NOW, ZoneOffset.UTC), Telemetry.noop());
            module.start();
            try {
                env.process().eventHandler().call(new PlayerCustomClickEvent(dancer.player(), DanceKeys.DANCE, CompoundBinaryTag.builder().putString("id", "moonwalk").build()));

                Assertions.assertEquals(0, DanceFixtures.markers(instance), "an unknown dance id must not create a camera");
            } finally {
                module.stop();
            }
        }
    }
}
