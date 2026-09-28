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
package net.onelitefeather.titan.runtime.permission;

import io.avaje.inject.BeanScope;
import io.avaje.inject.spi.AvajeModule;
import io.avaje.inject.spi.Builder;
import java.util.UUID;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Characterises D2's fallback selection (design.md): a concrete {@link PermissionService} must
 * outrank {@link DenyAllPermissionService}'s {@code @Secondary} priority, and the fallback must
 * still resolve alone.
 *
 * <p>Avaje's annotation processor never runs against this project's test sources (see
 * {@code net.onelitefeather.titan.runtime.variant.FooColumnModule}), so a test-only
 * {@code @Singleton} is never scanned. A supplied bean ({@code BeanScopeBuilder#bean}/{@code
 * mock}) would not prove anything either: it is registered at {@code SUPPLIED} priority, which
 * always wins regardless of {@code @Secondary}. Each test below instead wires a tiny,
 * self-contained {@link AvajeModule} through the exact {@code isBeanAbsent}/{@code
 * asSecondary}/{@code register} calls Avaje's generator itself emits for {@code @Secondary} (see
 * the generated {@code DenyAllPermissionService$DI}), scoped via {@code
 * BeanScope.builder().modules(...)} so the real runtime module - and every other test - is
 * untouched.
 */
class PermissionServiceFallbackSelectionTest {

    private record ConcretePermissionService(PermissionResult result) implements PermissionService {
        @Override
        public PermissionResult check(UUID playerId, String permission) {
            return this.result;
        }

        @Override
        public String name() {
            return "concrete";
        }
    }

    private static final class FallbackOnlyModule implements AvajeModule {
        @Override
        public Class<?>[] classes() {
            return new Class<?>[0];
        }

        @Override
        public void build(Builder builder) {
            if (builder.isBeanAbsent("DenyAll", DenyAllPermissionService.class, PermissionService.class)) {
                builder.asSecondary().register(new DenyAllPermissionService());
            }
        }
    }

    private static final class ConcreteAndFallbackModule implements AvajeModule {
        @Override
        public Class<?>[] classes() {
            return new Class<?>[0];
        }

        @Override
        public void build(Builder builder) {
            if (builder.isBeanAbsent("Concrete", ConcretePermissionService.class, PermissionService.class)) {
                builder.register(new ConcretePermissionService(PermissionResult.ALLOWED));
            }
            if (builder.isBeanAbsent("DenyAll", DenyAllPermissionService.class, PermissionService.class)) {
                builder.asSecondary().register(new DenyAllPermissionService());
            }
        }
    }

    @DisplayName("A concrete PermissionService outranks the @Secondary fallback")
    @Test
    void concretePermissionServiceOutranksTheFallback() {
        try (BeanScope scope = BeanScope.builder().modules(new ConcreteAndFallbackModule()).build()) {
            PermissionService resolved = scope.get(PermissionService.class);

            Assertions.assertEquals("concrete", resolved.name(), "the concrete service must win over the @Secondary fallback");
        }
    }

    @DisplayName("The @Secondary fallback resolves when no concrete PermissionService exists")
    @Test
    void fallbackResolvesWhenNoConcretePermissionServiceExists() {
        try (BeanScope scope = BeanScope.builder().modules(new FallbackOnlyModule()).build()) {
            PermissionService resolved = scope.get(PermissionService.class);

            Assertions.assertEquals("deny-all", resolved.name(), "the fallback must resolve when nothing else provides PermissionService");
        }
    }
}
