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
package net.onelitefeather.titan.app.module;

/**
 * Thrown by {@link ModuleRegistry#enableAll()} when a module's {@link LobbyModule#enable} throws.
 * Startup aborts rather than continuing with a partially initialised lobby; the message names the
 * failing module so an operator does not have to read a stack trace to find it.
 */
public final class ModuleLifecycleException extends RuntimeException {

    ModuleLifecycleException(String moduleId, Throwable cause) {
        super("Module '" + moduleId + "' failed to enable", cause);
    }
}
