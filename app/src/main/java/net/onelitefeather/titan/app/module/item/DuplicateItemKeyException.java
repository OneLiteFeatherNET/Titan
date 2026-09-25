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
package net.onelitefeather.titan.app.module.item;

/**
 * Thrown by {@link ItemRegistry#validate()} when two registrations - from any combination of
 * modules - claimed the same {@link LobbyItem#key()}. The message names the contested key and both
 * modules, so an operator does not have to read code to find the conflict.
 */
public final class DuplicateItemKeyException extends RuntimeException {

    DuplicateItemKeyException(DuplicateItemKeyDetector.Conflict conflict) {
        super("Modules '" + conflict.firstModuleId() + "' and '" + conflict.secondModuleId() + "' both registered item key '" + conflict.key() + "'");
    }
}
