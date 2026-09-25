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
import net.minestom.server.event.Event;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.onelitefeather.titan.app.module.item.ModuleItems;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
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
 * closes it immediately afterwards.
 *
 * <p>Built from a single {@link ModulePlatform}, the package-private parameter object every
 * platform service ({@link ModulePlatform#scheduler()}, {@link ModulePlatform#commandManager()},
 * {@link ModulePlatform#items()}, {@link ModulePlatform#navigator()})
 * lives on. {@link ModulePlatform} itself is never exposed to a module - only the narrow,
 * per-module
 * views built from it here, each backed by the {@link #onDisable} cleanup hook, so adding another
 * platform service never needs another {@link ModuleContext} constructor.
 */
public final class ModuleContext {

    private final String moduleId;
    private final EventNode<Event> node;
    private final ModuleTasksImpl tasks;
    private final ModuleCommandsImpl commands;
    private final ModulePlatform platform;
    private final NavigatorEntries.View navigator;
    private final ModuleItems items;
    private final Deque<Runnable> cleanupHooks = new ArrayDeque<>();
    private volatile boolean listeningClosed;

    ModuleContext(String moduleId, ModulePlatform platform) {
        this.moduleId = moduleId;
        this.node = EventNode.all("titan/" + moduleId);
        this.tasks = new ModuleTasksImpl(platform.scheduler());
        this.commands = new ModuleCommandsImpl(platform.commandManager(), this);
        this.platform = platform;
        this.navigator = platform.navigator().forModule(moduleId, this::onDisable);
        this.items = platform.items().contextView(moduleId, this::onDisable);
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
     * <p>For a {@link net.minestom.server.event.trait.CancellableEvent}, {@code listener} is
     * skipped
     * once the event is already cancelled by the time it reaches this module's node - Minestom's
     * usual behaviour for a {@code Consumer}-based listener. A module that must react regardless of
     * an earlier module's cancellation (e.g. to close an inventory or forward a click even though
     * another module cancelled it) needs {@link #listenIncludingCancelled} instead.
     *
     * @param type     the event type to listen for
     * @param listener the listener
     * @param <E>      the event type
     * @throws IllegalStateException if called after {@link LobbyModule#enable} has returned - a
     *                               module registers everything it needs up front, never in
     *                               response to a player joining, opening a menu and so on
     */
    public <E extends Event> void listen(Class<E> type, Consumer<E> listener) {
        registerListener(type, listener, false);
    }

    /**
     * Registers {@code listener} for {@code type}, exactly like {@link #listen}, except the
     * listener
     * still runs even if the event is already cancelled by the time it reaches this module's node.
     *
     * <p>This exists so that two modules reacting to the same
     * {@link net.minestom.server.event.trait.CancellableEvent} - one of which unconditionally
     * cancels it, such as {@code feature.protection.ProtectionModule} - never end up coupled to
     * each
     * other's enable order: the built-in "cancelled events are skipped" behaviour that
     * {@link #listen} relies on would otherwise silently swallow this module's listener whenever
     * the
     * cancelling module happened to be enabled first. Use this only for a listener that genuinely
     * needs to run no matter what an earlier module did to the event - it still sees (and may
     * itself
     * check) {@link net.minestom.server.event.trait.CancellableEvent#isCancelled()}.
     *
     * <p>Implemented via {@link EventListener#builder(Class)} with
     * {@link EventListener.Builder#ignoreCancelled(boolean) ignoreCancelled(false)}, wrapped in the
     * same {@link TitanObservability#guard(String, Consumer)} as {@link #listen}.
     *
     * @param type     the event type to listen for
     * @param listener the listener
     * @param <E>      the event type
     * @throws IllegalStateException if called after {@link LobbyModule#enable} has returned, for
     *                               the
     *                               same reason as {@link #listen}
     */
    public <E extends Event> void listenIncludingCancelled(Class<E> type, Consumer<E> listener) {
        registerListener(type, listener, true);
    }

    private <E extends Event> void registerListener(Class<E> type, Consumer<E> listener, boolean includeCancelled) {
        if (this.listeningClosed) {
            throw new IllegalStateException("Module '" + this.moduleId + "' tried to register a listener for " + type.getSimpleName() + " after enable() returned. Listeners must be registered while enable() runs.");
        }
        Consumer<E> guarded = TitanObservability.guard(this.moduleId, listener);
        if (includeCancelled) {
            this.node.addListener(EventListener.builder(type).ignoreCancelled(false).handler(guarded).build());
        } else {
            this.node.addListener(type, guarded);
        }
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
     * @return this module's own view of the platform-wide item registry
     */
    public ModuleItems items() {
        return this.items;
    }

    /**
     * Queues {@code cleanup} to run when this module is disabled. Later registrars (commands, items
     * and navigator entries today) call this instead of {@link ModuleRegistry}
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
