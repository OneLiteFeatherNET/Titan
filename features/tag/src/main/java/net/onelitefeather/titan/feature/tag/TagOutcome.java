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
package net.onelitefeather.titan.feature.tag;

/** What a hit did to the tag game. */
enum TagOutcome {
    /** The role moved to the target. */
    PASSED,
    /** The reverse hit within the cooldown; the role stays where it is. */
    COOLDOWN,
    /** Nothing to count: no tagger hit, or a player outside the game. */
    IGNORED
}
