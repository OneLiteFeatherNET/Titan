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
package net.onelitefeather.titan.app.module.navigator;

/**
 * Thrown by {@link NavigatorEntries#validate()} when two entries, contributed by any combination of
 * the configuration and modules, occupy the same slot. The message names the slot and both
 * conflicting entries together with the module that contributed each, so an operator does not have
 * to read a stack trace to find the conflict. See {@code design.md}, decision 8, and the
 * {@code lobby-navigator} spec's "Doppelt belegte Navigator-Plätze" requirement.
 */
public final class NavigatorConflictException extends RuntimeException {

    NavigatorConflictException(int slot, String firstModuleId, NavigatorEntry first, String secondModuleId, NavigatorEntry second) {
        super("Navigator slot " + slot + " is used by both '" + first.destination() + "' (contributed by module '" + firstModuleId + "') and '" + second.destination() + "' (contributed by module '" + secondModuleId + "')");
    }
}
