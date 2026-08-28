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

import net.minestom.server.entity.Player;

import java.util.UUID;

public sealed interface DeliverComponent permits DeliverComponent.TaskComponent,
        DeliverComponent.ServerDeliverComponent, TaskComponentImpl, ServerDeliverComponentImpl {
    /**
     * The type of deliver component
     * 
     * @return the type
     */
    DeliverType type();

    UUID playerId();

    static TaskBuilder taskBuilder() {
        return new TaskBuilderImpl();
    }

    static ServerBuilder serverBuilder() {
        return new ServerBuilderImpl();
    }

    sealed interface TaskComponent extends DeliverComponent permits TaskComponentImpl {
        String taskName();
    }

    sealed interface ServerDeliverComponent extends DeliverComponent permits
            ServerDeliverComponentImpl {
        String gameServer();
    }

    sealed interface Builder<T extends Builder<T>> {

        T playerId(UUID playerId);

        default T player(Player player) {
            return playerId(player.getUuid());
        }

        DeliverComponent build();
    }

    sealed interface TaskBuilder extends Builder<TaskBuilder> permits TaskBuilderImpl {
        TaskBuilder taskName(String taskName);
    }

    sealed interface ServerBuilder extends Builder<ServerBuilder> permits ServerBuilderImpl {
        ServerBuilder serverName(String serverName);
    }
}
