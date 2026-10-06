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
package net.onelitefeather.titan.feature.jumprun.course;

import java.util.function.IntSupplier;
import net.onelitefeather.titan.core.module.LobbyHeightBounds;

/** Lobby height limits for tests; suppliers make them live, {@link #fixed} makes them constant. */
record TestBounds(IntSupplier min, IntSupplier max) implements LobbyHeightBounds {

    static TestBounds fixed(int min, int max) {
        return new TestBounds(() -> min, () -> max);
    }

    @Override
    public int minHeight() {
        return min.getAsInt();
    }

    @Override
    public int maxHeight() {
        return max.getAsInt();
    }
}
