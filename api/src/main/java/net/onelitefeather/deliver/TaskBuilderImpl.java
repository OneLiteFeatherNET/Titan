package net.onelitefeather.deliver;

import java.util.UUID;

final class TaskBuilderImpl implements DeliverComponent.TaskBuilder {

    private UUID playerId;
    private String taskName;

    @Override
    public TaskBuilderImpl taskName(String taskName) {
        this.taskName = taskName;
        return this;
    }

    @Override
    public TaskBuilderImpl playerId(UUID playerId) {
        this.playerId = playerId;
        return this;
    }

    @Override
    public DeliverComponent build() {
        if (playerId == null) {
            throw new IllegalStateException("Player ID not set");
        }
        if (taskName == null) {
            throw new IllegalStateException("Task name not set");
        }
        return new TaskComponentImpl(DeliverType.TASK, taskName, playerId);
    }
}
