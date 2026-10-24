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
package net.onelitefeather.titan.core.testfixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.UUID;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;
import org.junit.jupiter.api.Test;

/**
 * What every {@link PermissionService} must do. The fake the module tests lean on and any real
 * source run the same cases, so the fake cannot drift from the real one.
 */
public interface PermissionServiceContract {

    UUID ALEX = new UUID(0, 1);
    UUID STEVE = new UUID(0, 2);
    String BUILD = "titan.build";
    String FLY = "titan.fly";

    /** What a source holds for one player and permission. */
    record Entry(UUID playerId, String permission, PermissionResult result) {
    }

    /**
     * A fresh service that holds exactly {@code entries} and nothing else. Called once per test.
     */
    PermissionService serviceWith(Entry... entries);

    @Test
    default void check_ofAnAllowedPermission_isAllowed() {
        PermissionService service = serviceWith(new Entry(ALEX, BUILD, PermissionResult.ALLOWED));

        assertEquals(PermissionResult.ALLOWED, service.check(ALEX, BUILD), "the source allows the permission for the player");
    }

    @Test
    default void check_ofADeniedPermission_isDenied() {
        PermissionService service = serviceWith(new Entry(ALEX, BUILD, PermissionResult.DENIED));

        assertEquals(PermissionResult.DENIED, service.check(ALEX, BUILD), "an explicit denial is not the same as not set");
    }

    @Test
    default void check_ofAPermissionNobodySet_isNotSet() {
        PermissionService service = serviceWith(new Entry(ALEX, BUILD, PermissionResult.ALLOWED));

        assertEquals(PermissionResult.NOT_SET, service.check(ALEX, FLY), "another permission of the same player is not affected");
    }

    @Test
    default void check_ofAnotherPlayer_isNotSet() {
        PermissionService service = serviceWith(new Entry(ALEX, BUILD, PermissionResult.ALLOWED));

        assertEquals(PermissionResult.NOT_SET, service.check(STEVE, BUILD), "the permission of one player does not leak to another");
    }

    @Test
    default void check_keepsEveryEntryApart() {
        PermissionService service = serviceWith(new Entry(ALEX, BUILD, PermissionResult.ALLOWED), new Entry(STEVE, BUILD, PermissionResult.DENIED));

        assertEquals(PermissionResult.ALLOWED, service.check(ALEX, BUILD), "Alex keeps his own result");
        assertEquals(PermissionResult.DENIED, service.check(STEVE, BUILD), "Steve keeps his own result");
    }

    @Test
    default void name_isNotBlank() {
        assertFalse(serviceWith().name().isBlank(), "the start log names the service");
    }
}
