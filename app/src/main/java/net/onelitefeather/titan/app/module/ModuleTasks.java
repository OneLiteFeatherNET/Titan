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

import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

/**
 * A module's own view of the scheduler, obtained through {@link ModuleContext#tasks()}.
 *
 * <p>Every task scheduled through this interface is tracked by the owning {@link ModuleContext}
 * and cancelled automatically when the module is disabled - a module never has to remember what it
 * scheduled or cancel it by hand.
 */
public interface ModuleTasks {

    /**
     * Schedules {@code task}, first running after {@code delay} and then repeating every
     * {@code repeat} until it is cancelled - either explicitly, through the returned {@link Task},
     * or automatically when the owning module is disabled.
     *
     * @param task   the action to run
     * @param delay  when the first run happens, relative to now
     * @param repeat when the next run happens after that, relative to the previous run; pass
     *               {@link TaskSchedule#stop()} to run {@code task} only once
     * @return the scheduled task
     */
    Task schedule(Runnable task, TaskSchedule delay, TaskSchedule repeat);
}
