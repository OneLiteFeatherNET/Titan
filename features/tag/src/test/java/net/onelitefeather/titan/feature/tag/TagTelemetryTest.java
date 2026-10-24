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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import net.onelitefeather.titan.core.testfixtures.TestTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TagTelemetryTest {

    private final TestTelemetry testTelemetry = TestTelemetry.create();
    private final TagTelemetry telemetry = new TagTelemetry(this.testTelemetry.telemetry());

    @AfterEach
    void closeTelemetry() {
        this.testTelemetry.close();
    }

    @DisplayName("A passed role counts as one tagged hit, with no player attribute")
    @Test
    void passedRoleCountsAsTagged() {
        this.telemetry.tagged();

        Assertions.assertEquals(1, this.testTelemetry.counter("titan.tag.tags", Attributes.of(AttributeKey.stringKey("result"), "tagged")), "one tagged hit must be counted under result=tagged only");
    }

    @DisplayName("A refused reverse hit counts as a cooldown hit")
    @Test
    void refusedHitCountsAsCooldown() {
        this.telemetry.cooldown();

        Assertions.assertEquals(1, this.testTelemetry.counter("titan.tag.tags", Attributes.of(AttributeKey.stringKey("result"), "cooldown")), "one refused hit must be counted under result=cooldown");
    }

    @DisplayName("Switching the setting on counts under state=on")
    @Test
    void switchingOnCountsOn() {
        this.telemetry.toggled(true);

        Assertions.assertEquals(1, this.testTelemetry.counter("titan.tag.toggles", Attributes.of(AttributeKey.stringKey("state"), "on")), "one switch on must be counted under state=on");
    }

    @DisplayName("Switching the setting off counts under state=off")
    @Test
    void switchingOffCountsOff() {
        this.telemetry.toggled(false);

        Assertions.assertEquals(1, this.testTelemetry.counter("titan.tag.toggles", Attributes.of(AttributeKey.stringKey("state"), "off")), "one switch off must be counted under state=off");
    }
}
