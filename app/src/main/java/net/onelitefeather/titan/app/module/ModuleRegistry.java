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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.app.module.item.ItemPlacementConflictException;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts and stops the lobby's {@link LobbyModule}s.
 *
 * <p>{@link #enableAll()} creates one {@link ModuleContext} per module, attaches its event node
 * under the shared {@code parent}, and calls {@link LobbyModule#enable}, in registration order.
 * {@link #disableAll()} reverses that: for each module, in the opposite order, it detaches the
 * node, cancels the module's tasks, runs its cleanup hooks (commands, items and navigator entries)
 * and only then calls {@link LobbyModule#disable()} - so by the time a module's own shutdown code
 * runs, it can no longer receive events or run scheduled work. See {@code design.md}, decision 2,
 * and the {@code lobby-modules} spec.
 *
 * <p>{@link #enableAll()} and {@link #disableAll()} share the same per-module enable and disable
 * steps ({@link #startModule}/{@link #stopModule}) rather than each having their own copy.
 *
 * <p>Once every module is up, {@link #enableAll()} validates the shared {@link ItemRegistry}, so
 * two modules claiming the same item placement aborts startup instead of silently shadowing one of
 * them. See {@code design.md}, decision 7.
 *
 * <p>Built through {@link #builder()} rather than a public constructor, so a later wave can add
 * further platform services to the builder without breaking existing callers.
 */
public final class ModuleRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleRegistry.class);

    private final EventNode<Event> parent;
    private final ModulePlatform platform;
    private final List<LobbyModule> modules;
    private final Map<String, ModuleContext> runningContexts = new LinkedHashMap<>();

    private ModuleRegistry(Builder builder) {
        this.parent = builder.parent;
        Scheduler scheduler = builder.scheduler != null ? builder.scheduler : MinecraftServer.getSchedulerManager();
        CommandManager commandManager = builder.commandManager != null ? builder.commandManager : MinecraftServer.getCommandManager();
        ItemRegistry itemRegistry = builder.itemRegistry != null ? builder.itemRegistry : new ItemRegistry(this.parent);
        this.platform = new ModulePlatform(scheduler, commandManager, itemRegistry);
        this.modules = List.copyOf(builder.modules);
    }

    /**
     * @return a new, empty builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Starts every registered module exactly once, in registration order. For each module this
     * attaches a fresh {@code titan/<id>} event node under {@code parent}, hands the module a new
     * {@link ModuleContext}, and calls {@link LobbyModule#enable}. Once {@code enable} returns,
     * that
     * module's context stops accepting new listeners.
     *
     * <p>Once every module is enabled, validates the shared {@link ItemRegistry}. That validation
     * does not run if a module's {@code enable} throws.
     *
     * @throws ModuleLifecycleException       if a module's {@code enable} throws; the exception
     *                                        names the failing module and carries the original
     *                                        failure as its cause - for an
     *                                        {@link IllegalArgumentException} a module's own
     *                                        validation function threw, that cause already names
     *                                        the offending key and reason. The failing module's own
     *                                        node, tasks and cleanup hooks are torn down before
     *                                        this
     *                                        is thrown; modules enabled earlier in this call are
     *                                        left running. This registry does not shut itself down
     *                                        in response - the only caller, {@code
     *                                        TitanApplication}, logs the failure and exits the
     *                                        process instead
     * @throws ItemPlacementConflictException if two modules registered an item for the same
     *                                        placement; thrown after every module has enabled, so
     *                                        the message can name both of them
     */
    public void enableAll() {
        for (LobbyModule module : this.modules) {
            ModuleContext context;
            try {
                context = startModule(module);
            } catch (RuntimeException exception) {
                throw new ModuleLifecycleException(module.id(), exception);
            }
            this.runningContexts.put(module.id(), context);
        }
        this.platform.items().validate();
    }

    /**
     * Stops every started module, in the reverse of the order it was started in. For each module,
     * in that reverse order: detaches its event node from {@code parent}, cancels its tasks, runs
     * its cleanup hooks (most recently added first), and only then calls
     * {@link LobbyModule#disable()}.
     */
    public void disableAll() {
        List<LobbyModule> reverseOrder = new ArrayList<>(this.modules);
        Collections.reverse(reverseOrder);
        for (LobbyModule module : reverseOrder) {
            ModuleContext context = this.runningContexts.remove(module.id());
            if (context != null) {
                stopModule(module, context);
            }
        }
    }

    /**
     * Starts a single module: attaches a fresh {@code titan/<id>} event node under {@code parent},
     * hands it a new {@link ModuleContext} and calls {@link LobbyModule#enable}. On failure, tears
     * the partial start back down (node, tasks, cleanup hooks - but not {@link LobbyModule#disable}
     * itself, since the module never finished enabling) and rethrows the original exception, so
     * {@link #enableAll()} can wrap it into a {@link ModuleLifecycleException}.
     *
     * @param module the module to start
     * @return the module's new, running context
     */
    private ModuleContext startModule(LobbyModule module) {
        ModuleContext context = new ModuleContext(module.id(), this.platform);
        this.parent.addChild(context.node());
        try {
            module.enable(context);
        } catch (RuntimeException exception) {
            context.closeForListening();
            this.parent.removeChild(context.node());
            context.cancelTasks();
            context.runCleanupHooks();
            throw exception;
        }
        context.closeForListening();
        return context;
    }

    /**
     * Stops a single module: detaches its event node from {@code parent}, cancels its tasks, runs
     * its cleanup hooks (most recently added first), and only then calls
     * {@link LobbyModule#disable()} - used by {@link #disableAll()} for every module.
     *
     * @param module  the module to stop
     * @param context the context {@link #startModule} previously returned for it
     */
    private void stopModule(LobbyModule module, ModuleContext context) {
        this.parent.removeChild(context.node());
        context.cancelTasks();
        context.runCleanupHooks();
        module.disable();
    }

    /** Builds a {@link ModuleRegistry}. */
    public static final class Builder {

        private EventNode<Event> parent;
        private Scheduler scheduler;
        private CommandManager commandManager;
        private ItemRegistry itemRegistry;
        private final List<LobbyModule> modules = new ArrayList<>();

        private Builder() {
        }

        /**
         * The event node modules attach their own {@code titan/<id>} node under. Required.
         *
         * @param parent the parent node
         * @return this builder
         */
        public Builder parent(EventNode<Event> parent) {
            this.parent = parent;
            return this;
        }

        /**
         * The scheduler modules' tasks run on. Defaults to
         * {@link MinecraftServer#getSchedulerManager()}.
         *
         * @param scheduler the scheduler
         * @return this builder
         */
        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        /**
         * The command manager modules register commands on. Defaults to
         * {@link MinecraftServer#getCommandManager()}.
         *
         * @param commandManager the command manager
         * @return this builder
         */
        public Builder commandManager(CommandManager commandManager) {
            this.commandManager = commandManager;
            return this;
        }

        /**
         * The platform-wide item registry modules register {@code LobbyItem}s through. Defaults to
         * a fresh {@link ItemRegistry} attached to {@code parent}.
         *
         * @param itemRegistry the item registry
         * @return this builder
         */
        public Builder items(ItemRegistry itemRegistry) {
            this.itemRegistry = itemRegistry;
            return this;
        }

        /**
         * Appends modules to the registration order. Order across multiple calls is preserved.
         *
         * @param modules the modules to add
         * @return this builder
         */
        public Builder modules(LobbyModule... modules) {
            this.modules.addAll(List.of(modules));
            return this;
        }

        /**
         * Appends modules to the registration order. Order across multiple calls is preserved.
         *
         * @param modules the modules to add
         * @return this builder
         */
        public Builder modules(List<LobbyModule> modules) {
            this.modules.addAll(modules);
            return this;
        }

        /**
         * @return a new registry for the configured {@code parent}, services and modules
         * @throws NullPointerException if {@code parent} was never set
         */
        public ModuleRegistry build() {
            Objects.requireNonNull(this.parent, "parent must be set");
            return new ModuleRegistry(this);
        }
    }
}
