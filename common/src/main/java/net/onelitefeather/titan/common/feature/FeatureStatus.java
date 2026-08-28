/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.common.feature;

import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Snapshot of everything an operator needs to know about one feature: whether the kill switch is
 * engaged, which audience the feature is released to, and which time window it is bound to. Read
 * by the {@code /season status} command.
 *
 * @param feature           name of the Togglz feature
 * @param killSwitchEngaged whether the feature is switched off outright
 * @param stage             the release stage the feature is currently on
 * @param from              inclusive start of the window, {@code null} when the window is open
 * @param to                exclusive end of the window, {@code null} when the window never closes
 * @param zone              the zone {@code from} and {@code to} are read in
 * @param withinWindow      whether the window is open right now
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public record FeatureStatus(String feature, boolean killSwitchEngaged, ReleaseStage stage,
                            @Nullable LocalDateTime from, @Nullable LocalDateTime to, ZoneId zone,
                            boolean withinWindow) {

    /**
     * Returns whether this feature has any time window configured at all.
     *
     * @return whether at least one of the two bounds is set
     */
    public boolean hasWindow() {
        return this.from != null || this.to != null;
    }
}
