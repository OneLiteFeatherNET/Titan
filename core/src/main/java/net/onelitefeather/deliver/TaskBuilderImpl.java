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
