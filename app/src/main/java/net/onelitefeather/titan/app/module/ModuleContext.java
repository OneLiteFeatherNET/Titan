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
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.common.config.ConfigException;
import net.onelitefeather.titan.common.config.ConfigStore;
import net.onelitefeather.titan.common.observability.TitanObservability;
import org.jetbrains.annotations.Nullable;

/**
 * A single module's handle to the platform, handed to {@link LobbyModule#enable(ModuleContext)}.
 *
 * <p>Deliberately narrow: it never exposes the raw event node a module's listeners hang off of, so
 * a module cannot reach into another module's events, add a child node the registry does not know
 * about, or keep listening after it has been disabled. Everything registered through this
 * instance - listeners, tasks, commands - is torn down by {@link ModuleRegistry} without the module
 * having to remember what it registered. See {@code design.md}, decision 3.
 *
 * <p>{@link #listen} and {@link #config} only work while {@link LobbyModule#enable} is running;
 * {@link ModuleRegistry} closes them immediately afterwards. Later platform additions (items,
 * navigator entries) follow the same shape: a small public view here, backed by the
 * {@link #onDisable} cleanup hook, so neither this class nor {@link ModuleRegistry} needs to change
 * again for them.
 */
public final class ModuleContext {

    private final String moduleId;
    private final EventNode<Event> node;
    private final ModuleTasksImpl tasks;
    private final ModuleCommandsImpl commands;
    private final @Nullable ConfigStore configStore;
    private final NavigatorEntries.View navigator;
    private final Deque<Runnable> cleanupHooks = new ArrayDeque<>();
    private volatile boolean listeningClosed;

    ModuleContext(String moduleId, Scheduler scheduler, CommandManager commandManager) {
        this(moduleId, scheduler, commandManager, null, new NavigatorEntries());
    }

    ModuleContext(String moduleId, Scheduler scheduler, CommandManager commandManager, @Nullable ConfigStore configStore) {
        this(moduleId, scheduler, commandManager, configStore, new NavigatorEntries());
    }

    ModuleContext(String moduleId, Scheduler scheduler, CommandManager commandManager, NavigatorEntries navigatorEntries) {
        this(moduleId, scheduler, commandManager, null, navigatorEntries);
    }

    ModuleContext(String moduleId, Scheduler scheduler, CommandManager commandManager, @Nullable ConfigStore configStore, NavigatorEntries navigatorEntries) {
        this.moduleId = moduleId;
        this.node = EventNode.all("titan/" + moduleId);
        this.tasks = new ModuleTasksImpl(scheduler);
        this.commands = new ModuleCommandsImpl(commandManager, this);
        this.configStore = configStore;
        this.navigator = navigatorEntries.forModule(moduleId, this::onDisable);
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
     * Returns this module's own configuration section, deserialized into {@code type}, using
     * {@code defaults} for anything the section (or the whole section) does not set.
     *
     * <p>Backed by {@link ConfigStore#section(String, Class, Record)}, called with this module's
     * own
     * {@link #moduleId()} as the section id - there is no overload that takes a different id, so a
     * module can only ever read its own section, never another module's (see {@code
     * lobby-module-config} spec, "Ein Modul MUSS nur seinen eigenen Abschnitt lesen können").
     *
     * <p>Only works while {@link LobbyModule#enable} is running, for the same reason as
     * {@link #listen}: a module reads its configuration once, up front, never in response to
     * something happening later (a player joining, a command running, ...).
     *
     * <p>If no {@link ConfigStore} was configured on the owning {@link ModuleRegistry}, this
     * returns
     * {@code defaults} unchanged instead of throwing, so a test module - or a module under test in
     * isolation - does not need to wire up a config file just to run.
     *
     * @param type     the config record type
     * @param defaults a fully populated default instance
     * @param <R>      the config record type
     * @return this module's section, deserialized into {@code type}, or {@code defaults} if no
     *         {@link ConfigStore} is configured
     * @throws IllegalStateException if called after {@link LobbyModule#enable} has returned
     * @throws ConfigException       if the section contains an invalid value; thrown by
     *                               {@code type}'s own compact constructor and completed by the
     *                               {@link ConfigStore} with this module's id as the section before
     *                               it reaches the caller
     */
    public <R extends Record> R config(Class<R> type, R defaults) {
        if (this.listeningClosed) {
            throw new IllegalStateException("Module '" + this.moduleId + "' tried to read its config after enable() returned. Config must be read while enable() runs.");
        }
        if (this.configStore == null) {
            return defaults;
        }
        return this.configStore.section(this.moduleId, type, defaults);
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
     * @return this module's own view of the platform's navigator entries; every entry added through
     *         it disappears again once this module is disabled
     */
    public NavigatorEntries.View navigator() {
        return this.navigator;
    }

    /**
     * Queues {@code cleanup} to run when this module is disabled. Later registrars (commands and
     * navigator entries today; items in a later change) call this instead of {@link ModuleRegistry}
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
     * Stops accepting new listeners and config reads; called by {@link ModuleRegistry} once
     * {@code enable} returns.
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
