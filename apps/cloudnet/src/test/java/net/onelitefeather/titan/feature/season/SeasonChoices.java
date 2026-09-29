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
package net.onelitefeather.titan.feature.season;

import java.nio.file.Path;
import java.time.Clock;
import net.onelitefeather.titan.core.module.LobbyWorldChoice;

/** Reaches the column's package-private test constructor from another package. */
public final class SeasonChoices {

    private SeasonChoices() {
    }

    public static LobbyWorldChoice at(Clock clock, Path worldsDirectory) {
        return new SeasonWorldChoice(clock, worldsDirectory);
    }
}
