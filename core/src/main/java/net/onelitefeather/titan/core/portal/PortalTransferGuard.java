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
package net.onelitefeather.titan.core.portal;

import java.util.UUID;

/**
 * Lets another feature hold a player back from a portal hand-over, e.g. while the player is in
 * the air after being thrown. A player may pass only if every guard allows it.
 */
@FunctionalInterface
public interface PortalTransferGuard {

    /** @return {@code false} to keep {@code playerId} from being handed over at this moment */
    boolean mayTransfer(UUID playerId);
}
