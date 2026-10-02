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
package net.onelitefeather.titan.core.portal;

import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A text display shown in front of a {@link Portal}. Plain data read from the map file;
 * {@link PortalValidator} decides whether it is usable.
 *
 * @param position    where the display floats, or {@code null} if the map file has none
 * @param text        MiniMessage shown while the source runs
 * @param offlineText MiniMessage shown instead when it does not run, or {@code null} to show
 *                    {@code text} with zero counts
 * @param source      where the player count comes from, or {@code null} for the portal's task
 * @param billboard   orientation of the display, never {@code null}
 * @param yaw         rotation used with {@link Billboard#FIXED}
 */
public record PortalLabel(@Nullable Vec position, String text, @Nullable String offlineText,
                          @Nullable LabelSource source, Billboard billboard, float yaw) {

    public PortalLabel {
        Objects.requireNonNull(billboard, "billboard");
    }
}
