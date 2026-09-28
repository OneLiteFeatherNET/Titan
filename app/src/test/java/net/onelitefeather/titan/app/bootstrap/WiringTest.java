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
import io.avaje.inject.spi.GenericType;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.feature.elytra.ElytraModule;
import net.onelitefeather.titan.app.feature.navigator.NavigatorModule;
import net.onelitefeather.titan.feature.protection.ProtectionModule;
import net.onelitefeather.titan.app.feature.respawn.RespawnModule;
import net.onelitefeather.titan.feature.sit.SitModule;
import net.onelitefeather.titan.app.feature.spawn.SpawnModule;
import net.onelitefeather.titan.feature.tickle.TickleModule;
import net.onelitefeather.titan.app.module.item.HotbarLobbyItems;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.common.map.MapProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * Builds the real Avaje Inject {@link BeanScope} - the same discovery {@code Titan} runs at
 * startup - and proves every feature bean, {@link HotbarLobbyItems}, node cleanup on close, and
 * failure
 * propagation work as they will in production.
 *
 * <p>{@link MapProvider} and {@link FeatureFlags} touch the filesystem or a process-wide static in
 * production, so both are mocked via Avaje's {@code forTesting().mock(Type)} escape hatch before
 * the scope is built, keeping this test Repeatable. Every other bean is built exactly as
 * {@code Titan} builds it in production.
 */
@ExtendWith(MicrotusExtension.class)
@Timeout(30)
class WiringTest {

    private static EventNode<Event> titanNode(BeanScope scope) {
        return scope.get(new GenericType<EventNode<Event>>() {
        }.type(), FeatureNode.TITAN_NODE);
    }

    @DisplayName("The scope builds all seven feature beans and a LobbyItems with exactly three items")
    @Test
    void scopeBuildsAllSevenFeaturesAndLobbyItemsWithThreeItems(Env env) {
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class).mock(FeatureFlags.class).build();

        try {
            Assertions.assertNotNull(scope.get(ProtectionModule.class), "the protection feature must be a bean");
            Assertions.assertNotNull(scope.get(SpawnModule.class), "the spawn feature must be a bean");
            Assertions.assertNotNull(scope.get(RespawnModule.class), "the respawn feature must be a bean");
            Assertions.assertNotNull(scope.get(NavigatorModule.class), "the navigator feature must be a bean");
            Assertions.assertNotNull(scope.get(SitModule.class), "the sit feature must be a bean");
            Assertions.assertNotNull(scope.get(TickleModule.class), "the tickle feature must be a bean");
            Assertions.assertNotNull(scope.get(ElytraModule.class), "the elytra feature must be a bean");

            HotbarLobbyItems lobbyItems = scope.get(HotbarLobbyItems.class);
            Assertions.assertEquals(3, lobbyItems.itemCount(), "exactly the navigator feather, the elytra chestplate and the elytra firework must be contributed");
        } finally {
            Assertions.assertDoesNotThrow(scope::close, "closing a fully built scope must not throw");
        }
    }

    @DisplayName("Closing the scope detaches every feature's own node from the titan node")
    @Test
    void closingTheScopeDetachesEveryFeatureNode(Env env) {
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class).mock(FeatureFlags.class).build();
        EventNode<Event> titan = titanNode(scope);
        Assertions.assertFalse(titan.getChildren().isEmpty(), "every feature must have attached its own node while the scope is open");

        scope.close();

        Assertions.assertTrue(titan.getChildren().isEmpty(), "no feature node may remain on the titan node once the scope is closed");
    }

    @DisplayName("A feature whose start fails aborts the whole build, and the exception's stack trace names the failing feature")
    @Test
    void aFailingFeatureAbortsTheBuildNamingIt(Env env) {
        // Avaje propagates a @PostConstruct failure unwrapped, so the failing feature's class only
        // appears in the original stack trace, never a wrapper naming it.
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> BeanScope.builder().forTesting().mock(MapProvider.class).mock(FeatureFlags.class, flags -> Mockito.when(flags.isActive(Mockito.anyString())).thenAnswer(invocation -> {
            throw new IllegalStateException("feature flag lookup failed (WiringTest)");
        })).build(), "a feature failing its start must abort building the scope instead of silently continuing");

        Assertions.assertTrue(namesFailingFeature(thrown, NavigatorModule.class), "the exception's stack trace (or one of its causes') must contain a frame in the failing feature's class; was: " + describeChain(thrown));
    }

    private static boolean namesFailingFeature(Throwable thrown, Class<?> featureType) {
        for (Throwable current = thrown; current != null; current = current.getCause()) {
            for (StackTraceElement frame : current.getStackTrace()) {
                if (featureType.getName().equals(frame.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String describeChain(Throwable thrown) {
        StringBuilder chain = new StringBuilder();
        for (Throwable current = thrown; current != null; current = current.getCause()) {
            chain.append(current).append(" <- ");
        }
        return chain.toString();
    }
}
