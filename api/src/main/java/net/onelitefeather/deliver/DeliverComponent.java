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
