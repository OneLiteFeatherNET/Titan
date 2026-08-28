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

import java.time.ZonedDateTime;

/**
 * A single observed change of a feature's release stage — the record US-3.09 asks for, and the
 * material for a new line in {@code docs/rollout-log.md}.
 *
 * @param feature name of the Togglz feature that moved
 * @param from    the stage the feature was on before
 * @param to      the stage the feature is on now
 * @param at      the moment the change was observed
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public record StageTransition(String feature, ReleaseStage from, ReleaseStage to,
                              ZonedDateTime at) {
}
