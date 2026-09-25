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
import java.util.function.BooleanSupplier;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.thread.TickSchedulerThread;
import net.minestom.server.thread.TickThread;
import net.minestom.server.timer.Scheduler;
import net.onelitefeather.titan.app.module.item.ItemPlacementConflictException;
import net.onelitefeather.titan.app.module.item.ItemRegistry;
import net.onelitefeather.titan.app.module.navigator.NavigatorConflictException;
import net.onelitefeather.titan.app.module.navigator.NavigatorEntries;
import net.onelitefeather.titan.common.feature.FeatureFlags;
import org.jetbrains.annotations.Nullable;
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
 * <p>{@link #restart(String)} does the same two things - disable, then enable - for a single
 * module, without touching any other module; see {@code design.md}, decision 3, and its own
 * Javadoc below. Both {@link #enableAll()}/{@link #disableAll()} and {@link #restart(String)} share
 * the same per-module disable and enable steps ({@link #stopModule}/{@link #startModule}) rather
 * than each having their own copy.
 *
 * <p>Once every module is up, {@link #enableAll()} validates the shared {@link ItemRegistry} and
 * {@link NavigatorEntries}, so two modules claiming the same item placement or navigator slot
 * aborts startup instead of silently shadowing one of them. See {@code design.md}, decisions 7 and
 * 8. If a {@link FeatureFlags} source was configured via {@link Builder#featureFlags}, that same
 * {@link NavigatorEntries} validation also checks every entry's optional feature flag - regardless
 * of which module contributed the entry - against it, aborting startup on an unknown name; see
 * {@code design.md}, decision 13.
 *
 * <p>Built through {@link #builder()} rather than a public constructor, so a later wave can add
 * further platform services to the builder without breaking existing callers.
 */
public final class ModuleRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleRegistry.class);

    private final EventNode<Event> parent;
    private final ModulePlatform platform;
    private final @Nullable ConnectionManager connectionManagerOverride;
    private final List<LobbyModule> modules;
    private final Map<String, LobbyModule> modulesById;
    private final Map<String, ModuleContext> runningContexts = new LinkedHashMap<>();
    private final @Nullable FeatureFlags featureFlags;
    private final BooleanSupplier tickThreadCheck;

    private ModuleRegistry(Builder builder) {
        this.parent = builder.parent;
        Scheduler scheduler = builder.scheduler != null ? builder.scheduler : MinecraftServer.getSchedulerManager();
        CommandManager commandManager = builder.commandManager != null ? builder.commandManager : MinecraftServer.getCommandManager();
        NavigatorEntries navigatorEntries = builder.navigatorEntries != null ? builder.navigatorEntries : new NavigatorEntries();
        ItemRegistry itemRegistry = builder.itemRegistry != null ? builder.itemRegistry : new ItemRegistry(this.parent);
        this.platform = new ModulePlatform(scheduler, commandManager, itemRegistry, navigatorEntries);
        // Resolved lazily in restart() instead of here: enableAll()/disableAll() never need a
        // connection manager at all, and eagerly calling MinecraftServer.getConnectionManager()
        // here would break every standalone test (ModulePlatformFixture, ModuleContextTest, ...)
        // that builds a registry without ever booting Minestom, even though none of them restart
        // anything.
        this.connectionManagerOverride = builder.connectionManager;
        this.modules = List.copyOf(builder.modules);
        Map<String, LobbyModule> byId = new LinkedHashMap<>();
        for (LobbyModule module : this.modules) {
            byId.put(module.id(), module);
        }
        this.modulesById = Map.copyOf(byId);
        this.featureFlags = builder.featureFlags;
        this.tickThreadCheck = builder.tickThreadCheck != null ? builder.tickThreadCheck : () -> isTickSchedulerThread(Thread.currentThread());
    }

    /**
     * @param thread the thread to check
     * @return whether {@code thread} is the single, globally-serialized thread Minestom's own
     *         {@code SchedulerManager} ticks scheduled tasks on - a {@link TickSchedulerThread}
     *         (name {@code Ms-TickScheduler}, see
     *         {@link MinecraftServer#THREAD_NAME_TICK_SCHEDULER}). That is the thread
     *         {@code ConfigReloadBootstrap} wires as {@code ConfigReloader}'s tick executor
     *         ({@code SchedulerManager#scheduleNextTick}/{@code Scheduler#execute}), so it is the
     *         thread every production call to {@link #restart(String)} actually runs on.
     *
     *         <p>Deliberately narrower than "any Minestom tick thread": Minestom's per-partition
     *         {@link TickThread} workers tick concurrently with each other (one per partition), and
     *         {@link #restart(String)} detaches an event node and, once the module is back up,
     *         re-equips <em>every</em> online player - state that spans every partition, not one.
     *         Only the single thread the scheduler itself is serialized on is an acceptable owner
     *         for that; a per-partition {@link TickThread} is rejected even though its name also
     *         contains "Tick".
     *
     *         <p>Package-private so {@code ModuleRegistryTest} can exercise it directly against a
     *         constructed {@link TickSchedulerThread} instance, without booting a full Minestom
     *         server or starting that thread.
     */
    static boolean isTickSchedulerThread(Thread thread) {
        return thread instanceof TickSchedulerThread;
    }

    /**
     * @return a new, empty builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return every registered module's id ({@link LobbyModule#id()}), in registration order - an
     *         unmodifiable snapshot, independent of whether each module is currently running. Used
     *         as the ordering seam a {@code ConfigReloader} adapter hands to
     *         {@code ModuleRestarter#moduleOrder()}, so a configuration reload restarts affected
     *         modules in this same order rather than in whatever order their diff keys happen to
     *         sort in; see {@code openspec/changes/config-reload-feature-flags/design.md}, decision
     *         3.
     */
    public List<String> moduleIds() {
        return this.modules.stream().map(LobbyModule::id).toList();
    }

    /**
     * Starts every registered module exactly once, in registration order. For each module this
     * attaches a fresh {@code titan/<id>} event node under {@code parent}, hands the module a new
     * {@link ModuleContext}, and calls {@link LobbyModule#enable}. Once {@code enable} returns,
     * that
     * module's context stops accepting new listeners.
     *
     * <p>Once every module is enabled, validates the shared {@link ItemRegistry}, then the shared
     * {@link NavigatorEntries}. Neither validation runs if a module's {@code enable} throws.
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
     * @throws NavigatorConflictException     if, once every module is enabled, two navigator
     *                                        entries share a slot
     * @throws IllegalArgumentException       if a {@link FeatureFlags} source was configured via
     *                                        {@link Builder#featureFlags} and a navigator entry -
     *                                        contributed by any module, not only through
     *                                        configuration - names a feature that source does not
     *                                        recognize; thrown after every module has enabled, so
     *                                        the message can name the entry's origin module
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
        if (this.featureFlags != null) {
            this.platform.navigator().validate(this.featureFlags);
        } else {
            this.platform.navigator().validate();
        }
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
     * Restarts a single module - {@code moduleId} - without touching any other module. Must be
     * called on the tick scheduler thread - see {@link #isTickSchedulerThread(Thread)} - and
     * {@code design.md}, decision 3.
     *
     * <p>Runs, in order:
     * <ol>
     * <li>If the module is currently running, disables it exactly like {@link #disableAll()}
     * would (detach node, cancel tasks, cleanup hooks in reverse order, then
     * {@link LobbyModule#disable()}).</li>
     * <li>Starts it again with a fresh {@link ModuleContext}, exactly like {@link #enableAll()}
     * would for that module.</li>
     * <li>If that succeeded, re-validates the shared {@link ItemRegistry} and
     * {@link NavigatorEntries} (against every module, not only the restarted one) and
     * re-equips every online player via {@link ItemRegistry#equip}.</li>
     * </ol>
     *
     * <p>If starting the module or that validation fails, the partial start is torn down the same
     * way step 1 tears a running module down, and this method returns {@link RestartOutcome.Failed}
     * instead of throwing - the caller (a later {@code ConfigReloader}) decides whether to restore
     * the module's previous configuration values and restart it again. Every other module is left
     * running untouched, whichever outcome this call ends in.
     *
     * @param moduleId the id of the module to restart, i.e. {@link LobbyModule#id()}
     * @return {@link RestartOutcome.Restarted} on success, {@link RestartOutcome.Failed} otherwise
     * @throws IllegalStateException    if called from any thread other than the tick scheduler
     *                                  thread - see {@link #isTickSchedulerThread(Thread)}
     * @throws IllegalArgumentException if no module with {@code moduleId} is registered
     */
    public RestartOutcome restart(String moduleId) {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        if (!this.tickThreadCheck.getAsBoolean()) {
            throw new IllegalStateException(
                    "ModuleRegistry.restart() must run on the tick scheduler thread ('" + MinecraftServer.THREAD_NAME_TICK_SCHEDULER + "'), but was called from '" + Thread.currentThread().getName() + "'");
        }
        LobbyModule module = this.modulesById.get(moduleId);
        if (module == null) {
            throw new IllegalArgumentException("No module registered with id '" + moduleId + "'");
        }

        ModuleContext previousContext = this.runningContexts.remove(moduleId);
        if (previousContext != null) {
            stopModule(module, previousContext);
        }

        ModuleContext newContext;
        try {
            newContext = startModule(module);
        } catch (RuntimeException exception) {
            LOGGER.debug("Module {} failed to enable during restart", moduleId, exception);
            return new RestartOutcome.Failed(exception);
        }

        try {
            this.platform.items().validate();
            if (this.featureFlags != null) {
                this.platform.navigator().validate(this.featureFlags);
            } else {
                this.platform.navigator().validate();
            }
        } catch (RuntimeException exception) {
            LOGGER.debug("Module {} rejected during post-restart validation, rolling the partial start back", moduleId, exception);
            stopModule(module, newContext);
            return new RestartOutcome.Failed(exception);
        }

        this.runningContexts.put(moduleId, newContext);
        ConnectionManager connectionManager = this.connectionManagerOverride != null ? this.connectionManagerOverride : MinecraftServer.getConnectionManager();
        for (Player player : connectionManager.getOnlinePlayers()) {
            this.platform.items().equip(player);
        }
        LOGGER.debug("Module {} restarted", moduleId);
        return new RestartOutcome.Restarted();
    }

    /**
     * Starts a single module: attaches a fresh {@code titan/<id>} event node under {@code parent},
     * hands it a new {@link ModuleContext} and calls {@link LobbyModule#enable}. On failure, tears
     * the partial start back down (node, tasks, cleanup hooks - but not {@link LobbyModule#disable}
     * itself, since the module never finished enabling) and rethrows the original exception, so
     * both {@link #enableAll()} and {@link #restart(String)} can each decide what that means for
     * them - wrap it, or turn it into a {@link RestartOutcome.Failed}.
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
     * {@link LobbyModule#disable()} - shared by {@link #disableAll()} and {@link #restart(String)}.
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
        private ConnectionManager connectionManager;
        private NavigatorEntries navigatorEntries;
        private ItemRegistry itemRegistry;
        private @Nullable FeatureFlags featureFlags;
        private BooleanSupplier tickThreadCheck;
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
         * The connection manager {@link ModuleRegistry#restart(String)} reads the currently online
         * players from, to re-equip them via {@link ItemRegistry#equip}. Defaults to
         * {@link MinecraftServer#getConnectionManager()}.
         *
         * @param connectionManager the connection manager
         * @return this builder
         */
        public Builder connectionManager(ConnectionManager connectionManager) {
            this.connectionManager = connectionManager;
            return this;
        }

        /**
         * The registry every module's navigator entries are collected in, shared by all modules
         * built from this registry. Defaults to a fresh, empty {@link NavigatorEntries}.
         *
         * @param navigatorEntries the navigator entry registry
         * @return this builder
         */
        public Builder navigator(NavigatorEntries navigatorEntries) {
            this.navigatorEntries = navigatorEntries;
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
         * The source of truth checked, once every module has been enabled, against every navigator
         * entry's optional feature flag - see {@link NavigatorEntries#validate(FeatureFlags)},
         * which
         * this is handed to. Optional: if never set, {@link #enableAll()} still checks navigator
         * entries for slot conflicts (via {@link NavigatorEntries#validate()}), but skips the
         * feature-flag check entirely - so a test registry that never adds a feature-gated entry
         * does not need to wire one up. The composition root ({@code Titan}) always sets this, with
         * the same {@link FeatureFlags} instance a feature module such as {@code NavigatorModule}
         * was itself built with.
         *
         * @param featureFlags the source of truth for which feature names exist
         * @return this builder
         */
        public Builder featureFlags(FeatureFlags featureFlags) {
            this.featureFlags = featureFlags;
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
         * Test-only override for the check {@link ModuleRegistry#restart(String)} uses to enforce
         * that it runs on the tick scheduler thread. Defaults to
         * {@code isTickSchedulerThread(Thread.currentThread())} - see
         * {@link ModuleRegistry#isTickSchedulerThread(Thread)} - which no test can satisfy
         * directly: environments such as Cyano's {@code Env#tick()} run every tick synchronously
         * on the calling (JUnit) thread, never on a real
         * {@link net.minestom.server.thread.TickSchedulerThread}. Package-private: only this
         * package's own tests use it, production wiring always keeps the real check.
         *
         * @param tickThreadCheck the check to use instead of the default
         * @return this builder
         */
        Builder tickThreadCheck(BooleanSupplier tickThreadCheck) {
            this.tickThreadCheck = tickThreadCheck;
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
