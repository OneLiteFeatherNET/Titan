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
package net.onelitefeather.titan.api.deliver;

import net.minestom.server.entity.Player;

import java.util.UUID;

/**
 * One delivery of one player to a task or a game server, built with {@link #taskBuilder()} or
 * {@link #serverBuilder()}.
 */
public sealed interface DeliverComponent permits DeliverComponent.TaskComponent,
        DeliverComponent.ServerDeliverComponent, TaskComponentImpl, ServerDeliverComponentImpl {
    /**
     * The type of deliver component
     * 
     * @return the type
     */
    DeliverType type();

    /** The UUID of the player to deliver. */
    UUID playerId();

    /** Starts a delivery to a task; the task name is set on the returned builder. */
    static TaskBuilder taskBuilder() {
        return new TaskBuilderImpl();
    }

    /** Starts a delivery to a game server; the server name is set on the returned builder. */
    static ServerBuilder serverBuilder() {
        return new ServerBuilderImpl();
    }

    /** A delivery addressed to a task, the group of servers that the proxy picks from. */
    sealed interface TaskComponent extends DeliverComponent permits TaskComponentImpl {
        /** The name of the task the player is delivered to. */
        String taskName();
    }

    /** A delivery addressed to one named game server. */
    sealed interface ServerDeliverComponent extends DeliverComponent permits
            ServerDeliverComponentImpl {
        /** The name of the game server the player is delivered to. */
        String gameServer();
    }

    /** The steps every delivery builder shares: the player, then the build. */
    sealed interface Builder<T extends Builder<T>> {

        /** Sets the player to deliver. */
        T playerId(UUID playerId);

        /** Sets the player to deliver, taken from the player's UUID. */
        default T player(Player player) {
            return playerId(player.getUuid());
        }

        /** Builds the component from the values set so far. */
        DeliverComponent build();
    }

    /** Builder for a delivery to a task. */
    sealed interface TaskBuilder extends Builder<TaskBuilder> permits TaskBuilderImpl {
        /** Sets the task to deliver to. */
        TaskBuilder taskName(String taskName);
    }

    /** Builder for a delivery to a game server. */
    sealed interface ServerBuilder extends Builder<ServerBuilder> permits ServerBuilderImpl {
        /** Sets the game server to deliver to. */
        ServerBuilder serverName(String serverName);
    }
}
