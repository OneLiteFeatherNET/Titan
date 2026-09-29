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

import java.util.Optional;

/**
 * The world this lobby was started with. A seam so the choice made when the world was loaded can
 * be supplied as is, instead of being computed a second time.
 */
@FunctionalInterface
interface StartedWorld {

    /** @return the world directory name; empty for the default world */
    Optional<String> worldName();
}
