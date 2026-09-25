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
import java.util.List;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.module.LobbyModule;
import net.onelitefeather.titan.common.feature.FeatureFlags;
import net.onelitefeather.titan.common.map.MapProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Builds the real Avaje Inject {@link BeanScope} - the same discovery {@code Titan} runs at
 * startup - and proves the wiring {@code openspec/changes/avaje-dependency-injection/design.md}
 * decisions 2, 3 and 6 describe: every one of the seven lobby feature modules is found exactly
 * once, {@link BeanScope#listByPriority(Class)} returns them in the fixed priority order the design
 * table declares, the scope builds with no missing constructor dependency, and it closes cleanly.
 *
 * <p><strong>Hermetic seam:</strong> two of {@code app.bootstrap.PlatformBeans}' beans touch the
 * filesystem or a process-wide static in production - {@link MapProvider} reads {@code worlds/},
 * and {@link FeatureFlags} (the real {@code TogglzFeatureFlags}) reads {@code flags.properties}
 * through Togglz's own static, JVM-wide cached {@code FeatureContext} (a global neither this test
 * nor {@code PlatformBeans} controls, and {@code common} - which owns it - is out of scope for
 * this change). Building the scope with those two built for real would make this test read and
 * depend on repository-relative files - not Repeatable, and exactly the untracked {@code worlds/}
 * the task warns against. Avaje Inject ships a test-only escape hatch for precisely this:
 * {@code BeanScope.builder().forTesting().mock(Type)} registers a Mockito mock for that type
 * <em>before</em> the scope is built, and every generated factory method checks whether its bean
 * type is already supplied before constructing one (see {@code *$DI.build_*} in the
 * annotation-processor output - each starts with {@code if (builder.isBeanAbsent(...))}) - so
 * {@code PlatformBeans#mapProvider} and {@code #featureFlags} never run at all, and every other
 * bean (all seven modules, the shared event node, item registry, navigator entries,
 * {@code Deliver}, {@code Clock}, and - since nothing overrides it - the real
 * {@code InstanceContainer}) is built exactly as {@code Titan} builds it in production. Beans that
 * depend on the mocked ones (e.g. {@code LobbySpawn}, wired from {@link MapProvider}) still get
 * built for real, against the mock - safe here because this test never calls a module's
 * {@code enable()}, so the mocked instances' methods are never actually invoked.
 */
@ExtendWith(MicrotusExtension.class)
class ModuleWiringTest {

    /** The table from design.md, decision 2: ascending @Priority, low first. */
    private static final List<String> EXPECTED_ORDER = List.of("protection", "spawn", "respawn", "navigator", "sit", "tickle", "elytra");

    @DisplayName("The scope wires all seven lobby modules exactly once, in priority order, with no missing dependency, and closes cleanly")
    @Test
    void scopeWiresEverySevenModulesInPriorityOrderAndClosesCleanly(Env env) {
        BeanScope scope = BeanScope.builder().forTesting().mock(MapProvider.class).mock(FeatureFlags.class).build();

        List<LobbyModule> modules = scope.listByPriority(LobbyModule.class);
        List<String> actualOrder = modules.stream().map(LobbyModule::id).toList();

        Assertions.assertEquals(EXPECTED_ORDER, actualOrder, "listByPriority(LobbyModule.class) must return every one of the seven modules exactly once, in this fixed priority order");
        Assertions.assertDoesNotThrow(scope::close, "closing a fully built scope must not throw");
    }
}
