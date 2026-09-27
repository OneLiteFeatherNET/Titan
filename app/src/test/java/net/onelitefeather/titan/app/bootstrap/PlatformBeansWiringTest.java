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
package net.onelitefeather.titan.app.bootstrap;

import io.avaje.inject.BeanScope;
import net.minestom.server.timer.Scheduler;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.item.LobbyItems;
import net.onelitefeather.titan.common.feature.FeatureFlags;
import net.onelitefeather.titan.common.map.MapProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Builds the real Avaje Inject {@link BeanScope}, exactly like {@code ModuleWiringTest}, and
 * proves the two beans {@link PlatformBeans} adds in this wave actually wire: {@link Scheduler}
 * resolves to the real scheduler manager, and {@link LobbyItems} builds successfully even though no
 * feature contributes a {@code @Bean LobbyItem} yet. Avaje Inject's list injection point for
 * {@code List<LobbyItem>} must tolerate being empty - if it did not, building the scope below would
 * throw. See {@code openspec/changes/dissolve-module-platform/tasks.md}, task 1.4.
 */
@ExtendWith(MicrotusExtension.class)
class PlatformBeansWiringTest {

    @DisplayName("The scope resolves Scheduler and builds LobbyItems with an empty item list")
    @Test
    void scopeResolvesSchedulerAndBuildsLobbyItemsWithNoItems(Env env) {
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class).mock(FeatureFlags.class).build();

        try {
            Scheduler scheduler = scope.get(Scheduler.class);
            LobbyItems lobbyItems = scope.get(LobbyItems.class);

            Assertions.assertNotNull(scheduler, "Scheduler must resolve to the real scheduler manager");
            Assertions.assertNotNull(lobbyItems, "LobbyItems must build even with no LobbyItem beans contributed yet");
        } finally {
            Assertions.assertDoesNotThrow(scope::close, "closing a fully built scope must not throw");
        }
    }
}
