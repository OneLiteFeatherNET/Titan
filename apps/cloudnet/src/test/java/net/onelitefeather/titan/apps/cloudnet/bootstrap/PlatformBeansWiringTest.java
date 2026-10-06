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
package net.onelitefeather.titan.apps.cloudnet.bootstrap;

import io.avaje.inject.BeanScope;
import io.opentelemetry.api.OpenTelemetry;
import java.util.List;
import net.minestom.server.timer.Scheduler;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.item.LobbyItems;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.platform.luckperms.LuckPermsPermissionService;
import net.onelitefeather.titan.apps.cloudnet.ActiveLobby;
import net.onelitefeather.titan.runtime.bootstrap.BeanProfiles;
import net.onelitefeather.titan.runtime.bootstrap.PlatformBeans;
import net.onelitefeather.titan.common.deliver.HolderPlayerCounts;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.core.portal.PlayerCount;
import net.onelitefeather.titan.core.portal.PlayerCounts;
import net.onelitefeather.titan.core.portal.SourceType;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Builds the real Avaje Inject {@link BeanScope}, exactly like {@code WiringTest}, and proves the
 * two platform beans {@link PlatformBeans} adds actually wire: {@link Scheduler} resolves to the
 * real scheduler manager, and {@link LobbyItems} builds successfully from the item beans the
 * features contribute. {@code WiringTest} additionally covers the exact item count and every
 * feature bean.
 */
@ExtendWith(MicrotusExtension.class)
@Timeout(30)
class PlatformBeansWiringTest {

    @DisplayName("The scope resolves Scheduler and builds LobbyItems")
    @Test
    void scopeResolvesSchedulerAndBuildsLobbyItems(Env env) {
        // Named mock, not the plain mock(Type) overload - see docs/lobby-modules.md,
        // "Permission-Plattform".
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.empty()).mock(FeatureFlags.class).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).build();

        try {
            Scheduler scheduler = scope.get(Scheduler.class);
            LobbyItems lobbyItems = scope.get(LobbyItems.class);

            Assertions.assertNotNull(scheduler, "Scheduler must resolve to the real scheduler manager");
            Assertions.assertNotNull(lobbyItems, "LobbyItems must build from the features' item beans");
        } finally {
            Assertions.assertDoesNotThrow(scope::close, "closing a fully built scope must not throw");
        }
    }

    @DisplayName("Without an agent the scope provides OpenTelemetry and Telemetry as no-ops")
    @Test
    void scopeProvidesNoopTelemetryWithoutAnAgent(Env env) {
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.empty()).mock(FeatureFlags.class).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).build();

        try {
            Assertions.assertNotNull(scope.get(OpenTelemetry.class), "OpenTelemetry must resolve");
            Telemetry telemetry = scope.get(Telemetry.class);
            Assertions.assertFalse(telemetry.tracer().spanBuilder("test.op").startSpan().getSpanContext().isValid(), "without an agent a span has no valid context");
        } finally {
            scope.close();
        }
    }

    @DisplayName("Outside CloudNet the portal column's fallback provides the player counts")
    @Test
    void fallbackProvidesPlayerCountsOutsideCloudNet(Env env) {
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.empty()).mock(FeatureFlags.class).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).profiles(BeanProfiles.active(List.of(), false)).build();

        try {
            Assertions.assertFalse(scope.get(PlayerCounts.class) instanceof HolderPlayerCounts, "the CloudNet bridge lookup must not be registered outside CloudNet");
            Assertions.assertEquals(PlayerCount.NOT_RUNNING, scope.get(PlayerCounts.class).count(SourceType.TASK, "Survival"), "the fallback reads every source as not running");
        } finally {
            scope.close();
        }
    }

    @DisplayName("As a CloudNet service the bridge lookup provides the player counts")
    @Test
    void holderProvidesPlayerCountsInCloudNet(Env env) {
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.empty()).mock(FeatureFlags.class).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).profiles(BeanProfiles.active(List.of(), true)).build();

        try {
            Assertions.assertInstanceOf(HolderPlayerCounts.class, scope.get(PlayerCounts.class), "the CloudNet profile must register the bridge lookup");
        } finally {
            scope.close();
        }
    }
}
