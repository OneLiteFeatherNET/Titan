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

import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import jakarta.inject.Singleton;
import java.util.Objects;
import net.onelitefeather.titan.common.permission.TitanPermissionBridge;
import net.onelitefeather.titan.core.permission.PermissionService;

/**
 * Feeds the CloudNet bridge's {@link TitanPermissionBridge} from the active
 * {@link PermissionService}, so a CloudNet permission query agrees with a lobby permission check,
 * with or without a platform module in the scope.
 */
@Singleton
public final class PermissionBridgeConnector {

    private final PermissionService permissionService;

    public PermissionBridgeConnector(PermissionService permissionService) {
        this.permissionService = Objects.requireNonNull(permissionService, "permissionService must not be null");
    }

    @PostConstruct
    void start() {
        TitanPermissionBridge.setResolver(PermissionBridgeResolver.of(this.permissionService));
    }

    @PreDestroy
    void stop() {
        TitanPermissionBridge.setResolver(null);
    }
}
