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

import io.avaje.inject.Secondary;
import jakarta.inject.Singleton;
import java.util.UUID;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.permission.PermissionService;

/**
 * The fallback used when no platform module provides a {@link PermissionService}: every player
 * permission resolves to {@link PermissionResult#NOT_SET}, so callers treat it as not granted. The
 * console is unaffected - commands check the console separately from this service.
 */
@Singleton
@Secondary
public final class DenyAllPermissionService implements PermissionService {

    @Override
    public PermissionResult check(UUID playerId, String permission) {
        return PermissionResult.NOT_SET;
    }

    @Override
    public String name() {
        return "deny-all";
    }
}
