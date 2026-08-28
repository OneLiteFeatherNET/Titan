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
package net.onelitefeather.deliver;

import java.util.UUID;

final class ServerBuilderImpl implements DeliverComponent.ServerBuilder {

    private String serverName;
    private UUID playerId;

    @Override
    public DeliverComponent.ServerBuilder serverName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    @Override
    public DeliverComponent.ServerBuilder playerId(UUID playerId) {
        this.playerId = playerId;
        return this;
    }

    @Override
    public DeliverComponent build() {
        if (serverName == null) {
            throw new IllegalArgumentException("Server cannot be null");
        }
        if (playerId == null) {
            throw new IllegalArgumentException("playerId cannot be null");
        }
        return new ServerDeliverComponentImpl(DeliverType.SERVER, serverName, playerId);
    }
}
