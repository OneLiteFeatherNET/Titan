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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Snapshot of everything an operator needs to know about one feature: whether the kill switch is
 * engaged, which audience the feature is released to, which time window it is bound to, and
 * whether any of that configuration could not be read. Rendered by the {@code /season status}
 * command.
 *
 * <p>The two "unreadable" fields exist because failing closed is safe but silent. A window whose
 * {@code from} is a typo makes the gate deny everyone, while the bounds themselves come back
 * empty — so without {@link #windowProblem()} the status would report "no window configured" for
 * a feature nobody can see. The one command whose purpose is to spare the operator a trip to the
 * log would then be telling them the opposite of the truth.
 *
 * @param feature           name of the Togglz feature
 * @param killSwitchEngaged whether the feature is switched off outright
 * @param stage             the release stage in effect, after the fallback for an unusable id
 * @param unknownStage      the configured stage id when it is not one of the three known ones,
 *                          otherwise {@code null}
 * @param from              inclusive start of the window, {@code null} when unset or unreadable
 * @param to                exclusive end of the window, {@code null} when unset or unreadable
 * @param zone              the zone {@code from} and {@code to} are read in
 * @param withinWindow      whether the window is open right now
 * @param windowProblem     description of the window parameter that cannot be read, otherwise
 *                          {@code null}
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 1.15.0
 */
public record FeatureStatus(String feature, boolean killSwitchEngaged, ReleaseStage stage,
                            @Nullable String unknownStage, @Nullable LocalDateTime from,
                            @Nullable LocalDateTime to, ZoneId zone, boolean withinWindow,
                            @Nullable String windowProblem) {

    /**
     * Guards the contradiction this record was extended to remove: an unreadable window is never
     * reported as an open one.
     */
    public FeatureStatus {
        if (windowProblem != null && withinWindow) {
            throw new IllegalArgumentException("a feature whose window cannot be read is never within it: " + feature);
        }
    }

    /**
     * Returns whether this feature has a readable time window configured.
     *
     * <p>Only ever {@code true} when the configuration parsed: ask {@link #windowReadable()}
     * before concluding from a {@code false} here that the feature runs unbounded.
     *
     * @return whether at least one of the two bounds is set and readable
     */
    @Contract(pure = true)
    public boolean hasWindow() {
        return this.from != null || this.to != null;
    }

    /**
     * Returns whether every configured window parameter could be read.
     *
     * @return whether the window configuration is usable
     */
    @Contract(pure = true)
    public boolean windowReadable() {
        return this.windowProblem == null;
    }

    /**
     * Returns whether the configured release stage was one of the three known ids.
     *
     * @return whether the stage configuration is usable
     */
    @Contract(pure = true)
    public boolean stageReadable() {
        return this.unknownStage == null;
    }
}
