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
 * Who may connect to a staging entry, and the connection itself. {@link StagingStarts} asks before
 * every connect, so a permission taken away while a player waits is never acted on.
 *
 * @param <P> the player handle, which the start logic only compares and passes on
 */
interface StagingConnect<P> {

    /** Whether the player still holds the permission of the entry. */
    boolean allowed(P player, Destination destination);

    /** Sends the player to the entry's task. */
    void connect(P player, Destination destination);
}
