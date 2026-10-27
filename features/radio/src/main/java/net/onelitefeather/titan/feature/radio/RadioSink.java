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
package net.onelitefeather.titan.feature.radio;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/**
 * Where a playback sends its sounds. The production sink sends them to one player only; the tests
 * record them. A playback never builds packets itself.
 */
interface RadioSink {

    /** One note block note. */
    void note(Sound sound);

    /** The start of a vanilla disc: the client plays its own record sound. */
    void playRecord(Sound sound);

    /** A named stop for one sound, so only that sound ends. */
    void stopRecord(Key sound);
}
