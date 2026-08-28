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
/**
 * Delivery of stacked resource packs: one long-lived base pack plus a swappable
 * season pack, each with its own stable identifier.
 *
 * <p>The whole package is inert until a {@code resource-packs.json} is present -
 * without it the lobby sends no resource pack packet at all.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
@NotNullByDefault
package net.onelitefeather.titan.common.resourcepack;

import org.jetbrains.annotations.NotNullByDefault;
