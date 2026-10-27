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

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/**
 * A sink that keeps every sound it is given, in order, so a test can read what would have been
 * sent.
 */
final class RecordingRadioSink implements RadioSink {

    final List<Sound> notes = new ArrayList<>();
    final List<Sound> records = new ArrayList<>();
    final List<Key> stops = new ArrayList<>();

    @Override
    public void note(Sound sound) {
        this.notes.add(sound);
    }

    @Override
    public void playRecord(Sound sound) {
        this.records.add(sound);
    }

    @Override
    public void stopRecord(Key sound) {
        this.stops.add(sound);
    }
}
