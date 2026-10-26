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
package net.onelitefeather.titan.feature.navigator;

/**
 * What a waiting or refused player is told: the action bar while a service starts, and the chat
 * refusals when the wait ends without a connection.
 *
 * @param <P> the player handle
 */
interface StagingNotice<P> {

    /** The action bar line that tells a player the service is starting and will connect them. */
    void waiting(P player, Destination destination);

    /** The chat refusal after the wait ran past its limit. */
    void timeout(P player, Destination destination);

    /** The chat refusal when the services cannot be read or a start was refused. */
    void unavailable(P player);
}
