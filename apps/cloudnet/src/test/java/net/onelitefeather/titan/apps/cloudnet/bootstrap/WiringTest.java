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
import java.util.List;
import java.util.Optional;
import io.avaje.inject.spi.GenericType;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.feature.elytra.ElytraModule;
import net.onelitefeather.titan.feature.navigator.NavigatorModule;
import net.onelitefeather.titan.feature.protection.ProtectionModule;
import net.onelitefeather.titan.feature.respawn.RespawnModule;
import net.onelitefeather.titan.feature.sit.SitModule;
import net.onelitefeather.titan.feature.spawn.SpawnModule;
import net.onelitefeather.titan.feature.tickle.TickleModule;
import net.onelitefeather.titan.feature.hotbar.HotbarLobbyItems;
import net.onelitefeather.titan.core.lobby.LobbyIdentities;
import net.onelitefeather.titan.core.lobby.LobbyIdentity;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.feature.FeatureFlags;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.platform.luckperms.LuckPermsPermissionService;
import net.onelitefeather.titan.apps.cloudnet.ActiveLobby;
import net.onelitefeather.titan.common.map.MapProvider;
import net.onelitefeather.titan.runtime.bootstrap.BeanProfiles;
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
 * production, and {@code PermissionService} would start real LuckPerms, so all three are mocked
 * via Avaje's {@code forTesting().mock(Type)} escape hatch before the scope is built, keeping this
 * test Fast and Repeatable. Every other bean is built exactly as {@code Titan} builds it in
 * production.
 */
@ExtendWith(MicrotusExtension.class)
@Timeout(30)
class WiringTest {

    private static EventNode<Event> titanNode(BeanScope scope) {
        return scope.get(new GenericType<EventNode<Event>>() {
        }.type(), FeatureNode.TITAN_NODE);
    }

    @DisplayName("The scope builds the seven module feature beans and a LobbyItems with exactly four items")
    @Test
    void scopeBuildsTheFeatureBeansAndLobbyItemsWithFourItems(Env env) {
        // Named mock, not the plain mock(Type) overload - see docs/lobby-modules.md,
        // "Permission-Plattform".
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.empty()).mock(FeatureFlags.class).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).build();

        try {
            Assertions.assertNotNull(scope.get(ProtectionModule.class), "the protection feature must be a bean");
            Assertions.assertNotNull(scope.get(SpawnModule.class), "the spawn feature must be a bean");
            Assertions.assertNotNull(scope.get(RespawnModule.class), "the respawn feature must be a bean");
            Assertions.assertNotNull(scope.get(NavigatorModule.class), "the navigator feature must be a bean");
            Assertions.assertNotNull(scope.get(SitModule.class), "the sit feature must be a bean");
            Assertions.assertNotNull(scope.get(TickleModule.class), "the tickle feature must be a bean");
            Assertions.assertNotNull(scope.get(ElytraModule.class), "the elytra feature must be a bean");

            HotbarLobbyItems lobbyItems = scope.get(HotbarLobbyItems.class);
            Assertions.assertEquals(4, lobbyItems.itemCount(), "exactly the jump and run item, the navigator feather, the elytra chestplate and the elytra firework must be contributed");
        } finally {
            Assertions.assertDoesNotThrow(scope::close, "closing a fully built scope must not throw");
        }
    }

    @DisplayName("As a CloudNet service with the flag on and an identity, the lobby switcher module and its clock exist")
    @Test
    void cloudnetProfileWithFlagOnProvidesTheLobbySwitcher(Env env) throws ClassNotFoundException {
        LobbyIdentities identities = () -> Optional.of(new LobbyIdentity("Lobby", "Lobby-1"));
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.empty()).mock(FeatureFlags.class, flags -> Mockito.when(flags.isActive("LOBBYSWITCHER")).thenReturn(true)).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).bean(LobbyIdentities.class, identities).profiles(BeanProfiles.active(List.of(), true)).build();

        try {
            Assertions.assertTrue(hasLobbySwitcherModule(scope), "the lobby switcher module must be a bean as a CloudNet service");
            Assertions.assertEquals(5, scope.get(HotbarLobbyItems.class).itemCount(), "the four lobby items plus the switcher clock must be contributed");
        } finally {
            Assertions.assertDoesNotThrow(scope::close, "closing a fully built scope must not throw");
        }
    }

    @DisplayName("As a CloudNet service with the flag off, the clock does not exist")
    @Test
    void cloudnetProfileWithFlagOffProvidesNoClock(Env env) {
        LobbyIdentities identities = () -> Optional.of(new LobbyIdentity("Lobby", "Lobby-1"));
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.empty()).mock(FeatureFlags.class).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).bean(LobbyIdentities.class, identities).profiles(BeanProfiles.active(List.of(), true)).build();

        try {
            Assertions.assertEquals(4, scope.get(HotbarLobbyItems.class).itemCount(), "without the flag no switcher clock may be contributed");
        } finally {
            scope.close();
        }
    }

    @DisplayName("As a CloudNet service the real identity bean resolves, empty until the bridge reports one")
    @Test
    void cloudnetProfileResolvesTheRealIdentityBean(Env env) {
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.empty()).mock(FeatureFlags.class).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).profiles(BeanProfiles.active(List.of(), true)).build();

        try {
            Assertions.assertNotNull(scope.get(LobbyIdentities.class), "the CloudNet profile must provide the lobby identity bean");
        } finally {
            scope.close();
        }
    }

    @DisplayName("Without the CloudNet profile no lobby switcher bean exists")
    @Test
    void withoutTheCloudnetProfileThereIsNoLobbySwitcher(Env env) throws ClassNotFoundException {
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.empty()).mock(FeatureFlags.class, flags -> Mockito.when(flags.isActive("LOBBYSWITCHER")).thenReturn(true)).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).profiles(BeanProfiles.active(List.of(), false)).build();

        try {
            Assertions.assertFalse(hasLobbySwitcherModule(scope), "the switcher module must not exist outside CloudNet");
            Assertions.assertEquals(4, scope.get(HotbarLobbyItems.class).itemCount(), "no switcher clock may be contributed outside CloudNet");
        } finally {
            scope.close();
        }
    }

    // The switcher module is package-private, so the bean is looked up by class name.
    private static boolean hasLobbySwitcherModule(BeanScope scope) throws ClassNotFoundException {
        return scope.contains(Class.forName("net.onelitefeather.titan.feature.lobbyswitcher.LobbySwitcherModule"));
    }

    @DisplayName("Closing the scope detaches every feature's own node from the titan node")
    @Test
    void closingTheScopeDetachesEveryFeatureNode(Env env) {
        // Named mock, not the plain mock(Type) overload - see docs/lobby-modules.md,
        // "Permission-Plattform".
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.empty()).mock(FeatureFlags.class).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).build();
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
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> BeanScope.builder().forTesting().mock(MapProvider.class, ActiveLobby.empty()).mock(FeatureFlags.class, flags -> Mockito.when(flags.isActive(Mockito.anyString())).thenAnswer(invocation -> {
            throw new IllegalStateException("feature flag lookup failed (WiringTest)");
        })).mock(PermissionService.class, LuckPermsPermissionService.QUALIFIER).build(), "a feature failing its start must abort building the scope instead of silently continuing");

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
