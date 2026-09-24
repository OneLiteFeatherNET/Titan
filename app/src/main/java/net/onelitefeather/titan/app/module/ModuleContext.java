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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.item.ModuleItems;
import net.onelitefeather.titan.common.observability.TitanObservability;

/**
 * A single module's handle to the platform, handed to {@link LobbyModule#enable(ModuleContext)}.
 *
 * <p>Deliberately narrow: it never exposes the raw event node a module's listeners hang off of, so
 * a module cannot reach into another module's events, add a child node the registry does not know
 * about, or keep listening after it has been disabled. Everything registered through this
 * instance - listeners, tasks, commands - is torn down by {@link ModuleRegistry} without the module
 * having to remember what it registered. See {@code design.md}, decision 3.
 *
 * <p>{@link #listen} only works while {@link LobbyModule#enable} is running; {@link ModuleRegistry}
 * closes it immediately afterwards. Later platform additions (config, items, navigator entries)
 * follow the same shape: a small public view here, backed by the {@link #onDisable} cleanup hook,
 * so neither this class nor {@link ModuleRegistry} needs to change again for them.
 */
public final class ModuleContext {

    private final String moduleId;
    private final EventNode<Event> node;
    private final ModuleTasksImpl tasks;
    private final ModuleCommandsImpl commands;
    private final ModuleItems items;
    private final Deque<Runnable> cleanupHooks = new ArrayDeque<>();
    private volatile boolean listeningClosed;

    /**
     * Package-private test convenience: builds a context with its own, unshared
     * {@link ItemRegistry}. Production code always goes through
     * {@link #ModuleContext(String, Scheduler, CommandManager, ItemRegistry)} via
     * {@link ModuleRegistry}, so every module shares the one platform-wide registry.
     */
    ModuleContext(String moduleId, Scheduler scheduler, CommandManager commandManager) {
        this(moduleId, scheduler, commandManager, new ItemRegistry(EventNode.all("titan-item-registry-fallback/" + moduleId)));
    }

    ModuleContext(String moduleId, Scheduler scheduler, CommandManager commandManager, ItemRegistry itemRegistry) {
        this.moduleId = moduleId;
        this.node = EventNode.all("titan/" + moduleId);
        this.tasks = new ModuleTasksImpl(scheduler);
        this.commands = new ModuleCommandsImpl(commandManager, this);
        this.items = itemRegistry.contextView(moduleId, this::onDisable);
    }

    /**
     * @return the id this context was created for, i.e. {@link LobbyModule#id()}
     */
    public String moduleId() {
        return this.moduleId;
    }

    /**
     * Registers {@code listener} for {@code type}, wrapped in
     * {@link TitanObservability#guard(String, Consumer)} so a failure is attributed to this module
     * (and, if the event carries one, its player) without stopping the lobby. The listener hangs
     * off
     * this module's own event node and is removed as a whole - along with the node itself - when
     * the module is disabled.
     *
     * @param type     the event type to listen for
     * @param listener the listener
     * @param <E>      the event type
     * @throws IllegalStateException if called after {@link LobbyModule#enable} has returned - a
     *                               module registers everything it needs up front, never in
     *                               response to a player joining, opening a menu and so on
     */
    public <E extends Event> void listen(Class<E> type, Consumer<E> listener) {
        if (this.listeningClosed) {
            throw new IllegalStateException("Module '" + this.moduleId + "' tried to register a listener for " + type.getSimpleName() + " after enable() returned. Listeners must be registered while enable() runs.");
        }
        this.node.addListener(type, TitanObservability.guard(this.moduleId, listener));
    }

    /**
     * @return this module's own view of the scheduler
     */
    public ModuleTasks tasks() {
        return this.tasks;
    }

    /**
     * @return this module's own view of the command manager
     */
    public ModuleCommands commands() {
        return this.commands;
    }

    /**
     * @return this module's own view of the platform-wide item registry
     */
    public ModuleItems items() {
        return this.items;
    }

    /**
     * Queues {@code cleanup} to run when this module is disabled. Later registrars (commands today;
     * items and navigator entries in later changes) call this instead of {@link ModuleRegistry}
     * having to know about them individually. Hooks run in reverse of the order they were added,
     * mirroring how the module registered things in the first place.
     *
     * @param cleanup the action to run on disable
     */
    void onDisable(Runnable cleanup) {
        this.cleanupHooks.addLast(cleanup);
    }

    /**
     * @return this module's own event node - package-private so only {@link ModuleRegistry} can
     *         attach or detach it; a module itself never sees it
     */
    EventNode<Event> node() {
        return this.node;
    }

    /**
     * Stops accepting new listeners; called by {@link ModuleRegistry} once {@code enable} returns.
     */
    void closeForListening() {
        this.listeningClosed = true;
    }

    /** Cancels every task this module scheduled. */
    void cancelTasks() {
        this.tasks.cancelAll();
    }

    /** Runs every queued cleanup hook, most recently added first, then forgets them. */
    void runCleanupHooks() {
        Runnable hook;
        while ((hook = this.cleanupHooks.pollLast()) != null) {
            hook.run();
        }
    }
}
