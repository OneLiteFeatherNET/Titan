package net.onelitefeather.deliver;


import net.minestom.server.entity.Player;

import java.util.UUID;

public sealed interface DeliverComponent permits DeliverComponent.TaskComponent, DeliverComponent.ServerDeliverComponent, TaskComponentImpl, ServerDeliverComponentImpl {
    /**
     * The type of deliver component
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

    sealed interface ServerDeliverComponent extends DeliverComponent permits ServerDeliverComponentImpl {
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
