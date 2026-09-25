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
package net.onelitefeather.titan.app.module;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

/**
 * Default {@link ModuleTasks}, backed by a single {@link Scheduler} shared by every module.
 *
 * <p>Scheduled tasks are tracked in a thread-safe list so {@link #cancelAll()} - called by
 * {@link ModuleRegistry} while disabling the owning module - stops every task the module ever
 * scheduled, whether or not it had already stopped itself.
 */
final class ModuleTasksImpl implements ModuleTasks {

    private final Scheduler scheduler;
    private final List<Task> tasks = new CopyOnWriteArrayList<>();

    ModuleTasksImpl(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Task schedule(Runnable task, TaskSchedule delay, TaskSchedule repeat) {
        Task scheduled = this.scheduler.scheduleTask(task, delay, repeat);
        this.tasks.add(scheduled);
        return scheduled;
    }

    /** Cancels every task this instance ever scheduled, alive or not, and forgets them. */
    void cancelAll() {
        for (Task task : this.tasks) {
            task.cancel();
        }
        this.tasks.clear();
    }
}
