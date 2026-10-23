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

/**
 * One vanilla-visible thing a dancer does at a given tick; {@link DanceEffects} performs it. The
 * dancer is the NPC: the real player is hidden from the others and is never moved by a step.
 */
enum DanceAction {
    SWING_MAIN_HAND, SWING_OFF_HAND, HEAD_DOWN, HEAD_UP, SNEAK, STAND, SPIN, SPIN_LAND, JUMP, NOTE, HEART
}
