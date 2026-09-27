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
import io.avaje.inject.BeanScopeBuilder;
import io.avaje.inject.spi.GenericType;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.feature.elytra.ElytraModule;
import net.onelitefeather.titan.app.feature.navigator.NavigatorModule;
import net.onelitefeather.titan.app.feature.protection.ProtectionModule;
import net.onelitefeather.titan.app.feature.respawn.RespawnModule;
import net.onelitefeather.titan.app.feature.sit.SitModule;
import net.onelitefeather.titan.app.feature.spawn.SpawnModule;
import net.onelitefeather.titan.app.feature.tickle.TickleModule;
import net.onelitefeather.titan.app.module.FeatureNode;
import net.onelitefeather.titan.app.module.item.LobbyItems;
import net.onelitefeather.titan.common.feature.FeatureFlags;
import net.onelitefeather.titan.common.map.MapProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Builds the real Avaje Inject {@link BeanScope} - the same discovery {@code Titan} runs at
 * startup - and proves the wiring {@code openspec/changes/dissolve-module-platform/design.md}
 * decisions 1 and 4 describe: every one of the seven lobby features is a bean the scope builds
 * (each starting itself through its own {@code @PostConstruct}), {@link LobbyItems} collects
 * exactly the three item beans the features contribute, closing the scope detaches every feature's
 * event node again, and a feature whose start fails aborts the whole build with a message naming
 * it.
 *
 * <p>Replaces the previous wiring test, which asserted the module-list-based discovery this change
 * removes (see task 3.2).
 *
 * <p><strong>Hermetic seam:</strong> two of {@code app.bootstrap.PlatformBeans}' beans touch the
 * filesystem or a process-wide static in production - {@link MapProvider} reads {@code worlds/},
 * and {@link FeatureFlags} (the real {@code ConfigFeatureFlags}) reads {@code features.*} through
 * the static, process-wide {@code io.avaje.config.Config} facade (a global neither this test
 * nor {@code PlatformBeans} controls, and {@code common} - which owns it - is out of scope for
 * this change). Building the scope with those two built for real would make this test read and
 * depend on repository-relative files - not Repeatable, and exactly the untracked {@code worlds/}
 * the task warns against. Avaje Inject ships a test-only escape hatch for precisely this:
 * {@code BeanScope.builder().forTesting().mock(Type)} registers a Mockito mock for that type
 * <em>before</em> the scope is built, and every generated factory method checks whether its bean
 * type is already supplied before constructing one - so {@code PlatformBeans#mapProvider} and
 * {@code #featureFlags} never run at all, and every other bean (all seven features, the shared
 * event node, {@code LobbyItems} and its three item beans, {@code Deliver}, {@code Clock}, and -
 * since nothing overrides it - the real {@code InstanceContainer}) is built exactly as
 * {@code Titan}
 * builds it in production.
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

            LobbyItems lobbyItems = scope.get(LobbyItems.class);
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

    /**
     * A feature's own {@code @PostConstruct} runs as part of building the {@link BeanScope}, on
     * the same construction path {@link BeanScopeBuilder#addPostConstruct(Runnable)} hooks into -
     * so a callback registered there that throws exercises the same "does a failure abort the
     * whole build" behaviour a real feature's failing {@code start()} would, without needing a
     * feature that only ever fails for this one test (a production test hook this change's rules
     * forbid). The callback names the class it stands in for itself, exactly like the report a real
     * failing feature's exception carries.
     */
    @DisplayName("A feature whose start fails aborts the whole build, naming the failing feature")
    @Test
    void aFailingFeatureAbortsTheBuildNamingIt(Env env) {
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> BeanScope.builder().forTesting().mock(MapProvider.class).mock(FeatureFlags.class).addPostConstruct(() -> {
            throw new IllegalStateException("SimulatedFailingFeature deliberately failed to start (WiringTest)");
        }).build(), "a feature failing its start must abort building the scope instead of silently continuing");

        Assertions.assertTrue(namesFailingFeature(thrown), "the exception (or one of its causes) must name the failing feature; was: " + describeChain(thrown));
    }

    private static boolean namesFailingFeature(Throwable thrown) {
        for (Throwable current = thrown; current != null; current = current.getCause()) {
            if (String.valueOf(current.getMessage()).contains("SimulatedFailingFeature")) {
                return true;
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
