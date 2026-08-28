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

/**
 * Outcome of a {@link FeatureGate} evaluation. The three denials name the step that stopped the
 * evaluation, in the fixed order the gate walks them: kill switch, release stage, time window.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public enum FeatureDecision {

    /** The player sees the feature. */
    ALLOWED,

    /**
     * The feature is switched off. A feature that has never been enabled counts as switched off,
     * which is what makes an unknown or unconfigured feature invisible rather than public.
     */
    DENIED_KILL_SWITCH,

    /** The feature is on, but the player is not part of the audience of its release stage. */
    DENIED_STAGE,

    /** The player is in the audience, but the current time is outside the configured window. */
    DENIED_WINDOW;

    /**
     * Returns whether this decision lets the player see the feature.
     *
     * @return {@code true} for {@link #ALLOWED}
     */
    @Contract(pure = true)
    public boolean isAllowed() {
        return this == ALLOWED;
    }
}
