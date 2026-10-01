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
package net.onelitefeather.titan.feature.portal;

import io.avaje.inject.BeanScope;
import io.avaje.inject.BeanScopeBuilder;
import io.avaje.inject.spi.GenericType;
import java.time.Clock;
import java.util.List;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.SourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCountsWiringTest {

    /** What the platform supplies to the column; the portal module itself is built for real. */
    private static BeanScopeBuilder platform() {
        return BeanScope.builder().bean(FeatureNode.TITAN_NODE, new GenericType<EventNode<Event>>() {
        }.type(), EventNode.all("test-portal-wiring")).bean(Deliver.class, new RecordingDeliver()).bean(PermissionService.class, new FakePermissionService()).bean(LobbyPortals.class, (LobbyPortals) List::of).bean(Clock.class, Clock.systemUTC());
    }

    @DisplayName("Without a provider the fallback answers that nothing runs")
    @Test
    void fallbackAnswersNotRunning() {
        try (BeanScope scope = platform().build()) {
            PlayerCounts counts = scope.get(PlayerCounts.class);

            assertInstanceOf(NoPlayerCounts.class, counts, "the @Secondary fallback must fill the gap");
            assertEquals(PlayerCount.NOT_RUNNING, counts.count(SourceType.TASK, "Survival"), "fallback reports not running");
            assertTrue(counts.supports(SourceType.SERVICE), "fallback supports every type");
        }
    }

    @DisplayName("A provider bean wins over the fallback")
    @Test
    void providerWinsOverFallback() {
        PlayerCounts provider = (type, name) -> new PlayerCount(3, 20, true);
        try (BeanScope scope = platform().bean(PlayerCounts.class, provider).build()) {
            assertEquals(new PlayerCount(3, 20, true), scope.get(PlayerCounts.class).count(SourceType.TASK, "Survival"), "the provider must be chosen without configuration");
        }
    }
}
