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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import java.time.Instant;
import java.time.ZoneOffset;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.testfixtures.AdjustableClock;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The started-dance counter, labelled with the dance's id. */
@ExtendWith(MicrotusExtension.class)
class DanceTelemetryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");
    private static final AttributeKey<String> DANCE = AttributeKey.stringKey("dance");

    @DisplayName("A started dance counts once under its id")
    @Test
    void startedDanceIsCountedUnderItsId(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), new AdjustableClock(NOW, ZoneOffset.UTC), telemetry.telemetry());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.GROOVE));

                Assertions.assertEquals(1, telemetry.counter(DanceTelemetry.STARTED, Attributes.of(DANCE, "groove")), "one started groove must be counted under dance=groove");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("A refused start is not counted")
    @Test
    void refusedStartIsNotCounted(Env env) {
        Instance instance = env.createFlatInstance();
        DanceFixtures.Dancer dancer = DanceFixtures.connect(env, instance);

        try (TestTelemetry telemetry = TestTelemetry.create(); TestTitanNode titan = TestTitanNode.attach(env)) {
            DanceModule module = DanceFixtures.module(env, titan, new FakePermissionService(), new AdjustableClock(NOW, ZoneOffset.UTC), telemetry.telemetry());
            module.start();
            try {
                env.process().eventHandler().call(DanceFixtures.danceClick(dancer.player(), Dance.SPIN));

                Assertions.assertEquals(0, telemetry.counter(DanceTelemetry.STARTED, Attributes.of(DANCE, "spin")), "a refused spin must not be counted");
            } finally {
                module.stop();
            }
        }
    }
}
