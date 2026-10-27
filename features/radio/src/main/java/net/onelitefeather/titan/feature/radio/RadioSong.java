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

import net.minestom.server.item.Material;

/**
 * A title the radio plays: a vanilla disc or a MIDI song. Both kinds share the same start, stop,
 * mode and telemetry; only the sound differs.
 */
sealed interface RadioSong permits VanillaDisc, MidiSong {

    String title();

    String artist();

    /** The music disc item that stands for the title in the menu. */
    Material disc();

    RadioKind kind();
}
