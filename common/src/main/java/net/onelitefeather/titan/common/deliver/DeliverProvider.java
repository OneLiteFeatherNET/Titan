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
package net.onelitefeather.titan.common.deliver;

import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.common.utils.CloudNetEnvironment;

/**
 * Picks the {@link Deliver} implementation based on whether the server runs as a
 * CloudNet service. CloudNet is no longer bundled into the fat jar (it is provided
 * by the CloudNet wrapper at runtime), so standalone runs fall back to a no-op.
 */
public final class DeliverProvider {

    private DeliverProvider() {
    }

    public static Deliver create() {
        if (CloudNetEnvironment.isPresent()) {
            return new MessageChannelDeliver();
        }
        return new NoopDeliver();
    }
}
