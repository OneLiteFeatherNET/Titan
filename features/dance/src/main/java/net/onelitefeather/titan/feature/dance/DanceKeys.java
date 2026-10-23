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
package net.onelitefeather.titan.feature.dance;

import net.kyori.adventure.key.Key;

/** The keys the dance lists and their buttons are known by. */
final class DanceKeys {

    /** The stop button of the dance list. */
    static final Key STOP = Key.key("titan", "dances/stop");

    /** A dance button of the dance list; its payload names the dance. */
    static final Key DANCE = Key.key("titan", "dance");

    private DanceKeys() {
    }
}
