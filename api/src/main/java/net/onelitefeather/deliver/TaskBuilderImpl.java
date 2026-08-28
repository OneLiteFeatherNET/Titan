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
