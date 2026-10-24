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

import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;
import org.junit.jupiter.api.Test;

class FakePermissionServiceTest implements PermissionServiceContract {

    @Override
    public PermissionService serviceWith(Entry... entries) {
        FakePermissionService service = new FakePermissionService();
        for (Entry entry : entries) {
            service.set(entry.playerId(), entry.permission(), entry.result());
        }
        return service;
    }

    @Test
    void grant_allowsThePermissionForThatPlayerOnly() {
        FakePermissionService service = new FakePermissionService().grant(ALEX, BUILD);

        assertEquals(PermissionResult.ALLOWED, service.check(ALEX, BUILD), "the granted player");
        assertEquals(PermissionResult.NOT_SET, service.check(STEVE, BUILD), "everybody else");
    }

    @Test
    void allowEveryone_allowsThePermissionForAnyPlayer() {
        FakePermissionService service = new FakePermissionService().allowEveryone(BUILD);

        assertEquals(PermissionResult.ALLOWED, service.check(STEVE, BUILD), "a player without an entry");
        assertEquals(PermissionResult.NOT_SET, service.check(STEVE, FLY), "another permission stays unset");
    }

    @Test
    void anExplicitResult_winsOverAllowEveryone() {
        FakePermissionService service = new FakePermissionService().allowEveryone(BUILD).set(STEVE, BUILD, PermissionResult.DENIED);

        assertEquals(PermissionResult.DENIED, service.check(STEVE, BUILD), "a denial for one player beats the general grant");
        assertEquals(PermissionResult.ALLOWED, service.check(ALEX, BUILD), "the general grant still holds for the others");
    }
}
